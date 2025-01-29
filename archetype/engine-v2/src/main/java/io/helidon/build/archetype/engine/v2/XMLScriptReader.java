/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.xml.XMLReader;

import static io.helidon.build.common.Strings.normalizePath;
import static java.util.Collections.unmodifiableMap;

/**
 * Script reader.
 */
public final class XMLScriptReader implements Script.Reader {

    private final XMLReader reader;
    private final boolean readOnly;
    private final Script script;
    private final Map<String, Node> methods;
    private Node.Builder builder;
    private Node node;

    /**
     * Create a new instance.
     *
     * @param source   source
     * @param readOnly read-only
     * @param loader   loader
     */
    public XMLScriptReader(Script.Source source, boolean readOnly, Script.Loader loader) {
        this.methods = new HashMap<>();
        this.script = new Script(loader, source, readOnly ? unmodifiableMap(methods) : methods);
        this.reader = new XMLReader(source.inputStream(), source.path().toString());
        this.readOnly = readOnly;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public Node readScript() {
        String name = reader.readName();
        if (!"archetype-script".equals(name)) {
            throw illegalState("Invalid root element: " + name);
        }
        builder = Node.builder(Kind.SCRIPT)
                .parent(builder)
                .location(location())
                .script(script);
        node = node();
        reader.read(this::readScript);
        if (node == null || node.kind() != Kind.SCRIPT) {
            throw illegalState("Invalid root element: " + node);
        }
        return node;
    }

    private IllegalStateException illegalState(String message) {
        return new IllegalStateException(String.format(
                "%s, location=%s:%d:%d",
                message, script.path(), reader.lineNumber(), reader.colNumber()));
    }

    private boolean readScript(String name) {
        return readMethods(name) || readDirective1(name);
    }

    private boolean readMethods(String name) {
        if (name.equals("methods")) {
            reader.read(this::readMethod);
            return true;
        }
        return false;
    }

    private boolean readMethod(String name) {
        if (name.equals("method")) {
            builder = Node.builder(Kind.METHOD)
                    .parent(builder)
                    .location(location())
                    .script(script);
            reader.readAttributes().forEach(builder::attribute);
            node = node();
            methods.put(node.attribute("name").getString(), node);
            reader.read(this::readDirective1);
            node = node.parent();
            builder = builder.parent();
            return true;
        }
        return false;
    }

    private boolean readHelp(String name) {
        if ("help".equals(name)) {
            builder.attribute(name, reader.readText());
            return true;
        }
        return false;
    }

    private boolean readDirective1(String name) {
        return readHelp(name) || readDirective2(name);
    }

    private boolean readDirective2(String name) {
        switch (name) {
            case "step":
                return readNode(Kind.STEP, this::readStep);
            case "validations":
                return readNode(Kind.VALIDATIONS, this::readValidations);
            default:
                return readDirective3(name);
        }
    }

    private boolean readDirective3(String name) {
        switch (name) {
            case "exec":
                return readNode(Kind.EXEC);
            case "source":
                return readNode(Kind.SOURCE);
            case "call":
                return readNode(Kind.CALL);
            case "presets":
                return readNode(Kind.PRESETS, this::readPresets);
            case "variables":
                return readNode(Kind.VARIABLES, this::readVariables);
            case "inputs":
                return readNode(Kind.INPUTS, this::readInputs);
            case "output":
                return readNode(Kind.OUTPUT, this::readOutput);
            default:
                return false;
        }
    }

    private boolean readStep(String name) {
        return readHelp(name) || readDirective3(name);
    }

    private boolean readInputs(String name) {
        switch (name) {
            case "text":
                return readNode(Kind.INPUT_TEXT, this::readHelp);
            case "boolean":
                return readNode(Kind.INPUT_BOOLEAN, this::readDirective1);
            case "enum":
                return readNode(Kind.INPUT_ENUM, this::readOptions);
            case "list":
                return readNode(Kind.INPUT_LIST, this::readOptions);
            default:
                return readDirective2(name);
        }
    }

    private boolean readOptions(String name) {
        return readHelp(name) || readOption(name);
    }

    private boolean readOption(String name) {
        if ("option".equals(name)) {
            return readNode(Kind.INPUT_OPTION, this::readDirective1);
        }
        return false;
    }

    private boolean readValidations(String name) {
        if ("validation".equals(name)) {
            return readNode(Kind.VALIDATION, this::readValidation);
        }
        return false;
    }

    private boolean readValidation(String name) {
        if ("regex".equals(name)) {
            return readValueNode(Kind.REGEX);
        }
        return false;
    }

    private boolean readListValue(String name) {
        if ("value".equals(name)) {
            builder.value().getList().add(reader.readText());
            return true;
        }
        return false;
    }

    private boolean readPresets(String name) {
        switch (name) {
            case "boolean":
                return readValueNode(Kind.PRESET_BOOLEAN);
            case "text":
                return readValueNode(Kind.PRESET_TEXT);
            case "enum":
                return readValueNode(Kind.PRESET_ENUM);
            case "list":
                return readValueNode(Kind.PRESET_LIST, Value.of(new ArrayList<>()), this::readListValue);
            default:
                return false;
        }
    }

    private boolean readVariables(String name) {
        switch (name) {
            case "boolean":
                return readValueNode(Kind.VARIABLE_BOOLEAN);
            case "text":
                return readValueNode(Kind.VARIABLE_TEXT);
            case "enum":
                return readValueNode(Kind.VARIABLE_ENUM);
            case "list":
                return readValueNode(Kind.VARIABLE_LIST, Value.of(new ArrayList<>()), this::readListValue);
            default:
                return false;
        }
    }

    private boolean readOutput(String name) {
        switch (name) {
            case "file":
                return readValueNode(Kind.FILE);
            case "template":
                return readNode(Kind.TEMPLATE, this::readModelBlock);
            case "files":
                return readNode(Kind.FILES, this::readResources);
            case "templates":
                return readNode(Kind.TEMPLATES, this::readTemplates);
            case "transformation":
                return readNode(Kind.TRANSFORMATION, this::readTransformation);
            case "model":
                return readModelBlock(name);
            default:
                return false;
        }
    }

    private boolean readResources(String name) {
        if ("directory".equals(name)) {
            builder.attribute(name, reader.readText());
            return true;
        }
        return readPattern(name);
    }

    private boolean readTemplates(String name) {
        return readResources(name) || readModelBlock(name);
    }

    private boolean readModelBlock(String name) {
        if ("model".equals(name)) {
            return readNode(Kind.MODEL, this::readModel);
        }
        return false;
    }

    private boolean readTransformation(String name) {
        if ("replace".equals(name)) {
            return readNode(Kind.REPLACE);
        }
        return false;
    }

    private boolean readPattern(String name) {
        switch (name) {
            case "includes":
                return readNode(Kind.INCLUDES, this::readIncludes);
            case "excludes":
                return readNode(Kind.EXCLUDES, this::readExcludes);
            default:
                return false;
        }
    }

    private boolean readIncludes(String name) {
        if ("include".equals(name)) {
            return readValueNode(Kind.INCLUDE);
        }
        return false;
    }

    private boolean readExcludes(String name) {
        if ("exclude".equals(name)) {
            return readValueNode(Kind.EXCLUDE);
        }
        return false;
    }

    private boolean readModel(String name) {
        switch (name) {
            case "value":
                return readValueNode(Kind.MODEL_VALUE);
            case "map":
                return readNode(Kind.MODEL_MAP, this::readModel);
            case "list":
                return readNode(Kind.MODEL_LIST, this::readModel);
            default:
                return false;
        }
    }

    private boolean readNode(Kind kind) {
        return readNode(kind, attrs -> Value.empty(), n -> true);
    }

    private boolean readValueNode(Kind kind) {
        return readNode(kind, attrs -> Value.dynamic(reader.readText()), n -> true);
    }

    private boolean readValueNode(Kind kind, Value<?> value, Predicate<String> predicate) {
        return readNode(kind, attrs -> value, predicate);
    }

    private boolean readNode(Kind kind, Predicate<String> predicate) {
        return readNode(kind, attrs -> Value.dynamic(attrs.get("value")), predicate);
    }

    private boolean readNode(Kind kind, Function<Map<String, String>, Value<?>> function, Predicate<String> predicate) {
        Node.Location location = location();
        Node.Builder builder = builder(kind, location);
        Map<String, String> attrs = reader.readAttributes();
        attrs.forEach(builder::attribute);
        builder.value(function.apply(attrs));
        String expression = attrs.get("if");
        if (expression != null) {
            Node.Builder condition = builder(Kind.CONDITION, location).expression(expression);
            push(builder.parent(condition).parent());
        }
        push(builder);
        reader.read(predicate);
        pop();
        if (expression != null) {
            pop();
        }
        return true;
    }

    private void push(Node.Builder b) {
        builder = b;
        node = node();
        builder.parent().children().add(node);
    }

    private void pop() {
        node = node.parent();
        builder = builder.parent();
    }

    private Node.Builder builder(Kind kind, Node.Location location) {
        return Node.builder(kind)
                .parent(builder)
                .location(location)
                .script(script);
    }

    private Node node() {
        return readOnly ? new Node.NodeImpl(node, builder) : builder;
    }

    private Node.Location location() {
        Path root = FileUtils.root(script.path());
        String fileName = normalizePath(root.relativize(script.path()));
        return new Node.Location(fileName, reader.lineNumber(), reader.colNumber());
    }
}
