/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.WeakHashMap;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Location;
import io.helidon.build.archetype.engine.v2.ast.Method;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.BuilderInfo;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Validation;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.Variable;
import io.helidon.build.common.Maps;
import io.helidon.build.common.VirtualFileSystem;
import io.helidon.build.common.xml.XMLParser;
import io.helidon.build.common.xml.XMLReader;
import io.helidon.build.common.xml.XMLReaderException;

import static io.helidon.build.common.xml.XMLParser.processXmlEscapes;

/**
 * Script loader.
 * XML reader for script document with caching.
 */
public class ScriptLoader {

    private static final Map<FileSystem, ScriptLoader> LOADERS = new WeakHashMap<>();
    private static final Random RANDOM = new Random();

    private final Map<Path, Script> scripts = new HashMap<>();

    private ScriptLoader() {
    }

    /**
     * Create a new instance.
     *
     * @return ScriptLoader
     */
    public static ScriptLoader create() {
        return new ScriptLoader();
    }

    /**
     * Get or load the script at the given path.
     *
     * @param path path
     * @return script
     */
    public static Script load(Path path) {
        return LOADERS.computeIfAbsent(path.getFileSystem(), fs -> new ScriptLoader())
                      .get(path);
    }

    /**
     * Create an unknown script path.
     *
     * @return path
     */
    public Path unknownPath() {
        Path randomDir = Path.of(String.valueOf(RANDOM.nextLong()));
        return VirtualFileSystem.create(randomDir).getPath("/").resolve("[unknown]");
    }

    /**
     * Add a script to this script loader.
     *
     * @param script script
     */
    public void add(Script script) {
        if (scripts.containsKey(script.scriptPath())) {
            throw new IllegalStateException("Script already defined: " + script);
        }
        scripts.put(script.scriptPath(), script);
    }

    /**
     * Load a script.
     *
     * @param path path to the script
     * @return Script
     */
    public Script get(Path path) {
        return scripts.computeIfAbsent(path.toAbsolutePath().normalize(), this::loadScript);
    }

    /**
     * Load an unnamed script.
     *
     * @param is input stream
     * @return Script
     */
    @SuppressWarnings("unused")
    public Script loadScript(InputStream is) {
        return loadScript(is, unknownPath());
    }

    private Script loadScript(Path path) {
        try {
            return loadScript(Files.newInputStream(path), path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Script loadScript(InputStream is, Path path) {
        try {
            return new ReaderImpl(this).read(is, path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private enum State {
        VARIABLE,
        PRESET,
        INPUT,
        METHOD,
        EXECUTABLE,
        OUTPUT,
        MODEL,
        REGEX,
        VALIDATION,
        BLOCK
    }

    private static final class Context {

        private final State state;
        private final Node.Builder<?, ?> builder;

        Context(State state, Node.Builder<?, ?> builder) {
            this.state = state;
            this.builder = builder;
        }
    }

    private static final class ReaderImpl implements XMLReader {

        private final ScriptLoader loader;
        private Path path;
        private BuilderInfo info;
        private XMLParser parser;
        private String qName;
        private Map<String, Value> attrs;
        private LinkedList<Context> stack;
        private Context ctx;
        private Script.Builder scriptBuilder;

        ReaderImpl(ScriptLoader loader) {
            this.loader = loader;
        }

        Script read(InputStream is, Path path) throws IOException {
            this.path = Objects.requireNonNull(path, "path is null");
            stack = new LinkedList<>();
            parser = XMLParser.create(is, this);
            parser.parse();
            if (scriptBuilder == null) {
                throw new IllegalStateException("Unable to create script");
            }
            return scriptBuilder.build();
        }

        @Override
        public void startElement(String qName, Map<String, String> attrs) {
            this.qName = qName;
            this.attrs = Maps.mapValue(attrs, DynamicValue::create);
            Location location = Location.of(path, parser.lineNumber(), parser.charNumber());
            info = BuilderInfo.of(loader, path, location);
            ctx = stack.peek();
            if (ctx == null) {
                if (!"archetype-script".equals(qName)) {
                    throw new XMLReaderException(String.format(
                            "Invalid root element '%s'. {file=%s, location=%s}",
                            qName, path, location));
                }
                scriptBuilder = Script.builder(info);
                stack.push(new Context(State.EXECUTABLE, scriptBuilder));
            } else {
                try {
                    processElement();
                } catch (IllegalArgumentException ex) {
                    throw new XMLReaderException(String.format(
                            "Invalid element '%s'. { file=%s, location=%s }",
                            qName, path, location), ex);
                } catch (Throwable ex) {
                    throw new XMLReaderException(String.format(
                            "An unexpected error occurred. { file=%s, location=%s }",
                            path, location), ex);
                }
            }
        }

        @Override
        public void elementText(String value) {
            ctx = stack.peek();
            if (ctx != null) {
                ctx.builder.value(value);
            }
        }

        @Override
        public void endElement(String name) {
            stack.pop().builder.build();
        }

        void processElement() {
            switch (qName) {
                case "directory":
                case "help":
                    stack.push(new Context(ctx.state, new ValueBuilder(ctx.builder, qName)));
                    return;
                default:
            }
            switch (ctx.state) {
                case EXECUTABLE:
                    if (!processExec()) {
                        processBlock();
                    }
                    break;
                case METHOD:
                    processMethod();
                    break;
                case BLOCK:
                    processBlock();
                    break;
                case PRESET:
                    processPreset();
                    break;
                case VARIABLE:
                    processVariable();
                    break;
                case INPUT:
                    processInput();
                    break;
                case OUTPUT:
                    processOutput();
                    break;
                case MODEL:
                    processModel();
                    break;
                case VALIDATION:
                    processValidation();
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid state: %s. { element=%s }", ctx.state, qName));
            }
        }

        void processMethod() {
            addChild(State.EXECUTABLE, Method.builder(info));
        }

        boolean processExec() {
            switch (qName) {
                case "exec":
                case "source":
                case "call":
                    addChild(ctx.state, Invocation.builder(info, invocationKind()));
                    return true;
                default:
                    return false;
            }
        }

        void processInput() {
            if (processExec()) {
                return;
            }
            State nextState;
            Block.Kind kind = blockKind();
            switch (kind) {
                case OPTION:
                case BOOLEAN:
                case TEXT:
                    nextState = State.EXECUTABLE;
                    break;
                case LIST:
                case ENUM:
                    nextState = State.INPUT;
                    break;
                case OUTPUT:
                    processBlock();
                    return;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid input block: %s. { element=%s }", kind, qName));
            }
            addChild(nextState, Input.builder(info, kind));
        }

        void processValidation() {
            Block.Kind kind = blockKind();
            switch (kind) {
                case VALIDATION:
                case REGEX:
                    addChild(ctx.state, Validation.builder(info, blockKind()));
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid validation block: %s. { element=%s }", kind, qName));
            }
        }

        void processPreset() {
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case BOOLEAN:
                case TEXT:
                case ENUM:
                case LIST:
                    builder = Preset.builder(info, blockKind());
                    break;
                case VALUE:
                    builder = Block.builder(info, blockKind());
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid preset block: %s. { element=%s }", kind, qName));

            }
            addChild(ctx.state, builder);
        }

        void processVariable() {
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case BOOLEAN:
                case TEXT:
                case ENUM:
                case LIST:
                    builder = Variable.builder(info, blockKind());
                    break;
                case VALUE:
                    builder = Block.builder(info, blockKind());
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid variable block: %s. { element=%s }", kind, qName));

            }
            addChild(ctx.state, builder);
        }

        void processOutput() {
            State nextState = State.OUTPUT;
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case INCLUDES:
                case EXCLUDES:
                    builder = Block.builder(info, kind);
                    break;
                case INCLUDE:
                case EXCLUDE:
                case TRANSFORMATION:
                case REPLACE:
                case FILES:
                case TEMPLATES:
                case FILE:
                case TEMPLATE:
                    builder = Output.builder(info, kind);
                    break;
                case MODEL:
                    nextState = State.MODEL;
                    builder = Block.builder(info, kind);
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid output block: %s. { element=%s }", kind, qName));

            }
            addChild(nextState, builder);
        }

        void processModel() {
            Block.Builder builder;
            Block.Kind kind = blockKind();
            switch (kind) {
                case MAP:
                case VALUE:
                case LIST:
                    builder = Model.builder(info, kind);
                    break;
                default:
                    throw new XMLReaderException(String.format(
                            "Invalid model block: %s. { element=%s }", kind, qName));

            }
            addChild(ctx.state, builder);
        }

        void processBlock() {
            State nextState = State.BLOCK;
            Block.Builder builder = null;
            Block.Kind kind = blockKind();
            switch (kind) {
                case SCRIPT:
                    nextState = State.EXECUTABLE;
                    break;
                case METHODS:
                    nextState = State.METHOD;
                    break;
                case METHOD:
                    nextState = State.EXECUTABLE;
                    builder = Method.builder(info);
                    break;
                case STEP:
                    nextState = State.EXECUTABLE;
                    builder = Step.builder(info);
                    break;
                case INPUTS:
                    nextState = State.INPUT;
                    break;
                case VARIABLES:
                    nextState = State.VARIABLE;
                    break;
                case PRESETS:
                    nextState = State.PRESET;
                    break;
                case OUTPUT:
                    nextState = State.OUTPUT;
                    break;
                case VALIDATIONS:
                    nextState = State.VALIDATION;
                    break;
                default:
            }
            if (builder == null) {
                builder = Block.builder(info, kind);
            }
            addChild(nextState, builder);
        }

        void addChild(State nextState, Node.Builder<? extends Node, ?> builder) {
            builder.attributes(attrs);
            Value ifExpr = attrs.get("if");
            if (ifExpr != null) {
                String expr = processXmlEscapes(ifExpr.asString());
                ctx.builder.addChild(Condition.builder(info).expression(expr).then(builder));
            } else {
                ctx.builder.addChild(builder);
            }
            stack.push(new Context(nextState, builder));
        }

        Block.Kind blockKind() {
            return Block.Kind.valueOf(qName.toUpperCase());
        }

        Invocation.Kind invocationKind() {
            return Invocation.Kind.valueOf(qName.toUpperCase());
        }
    }

    private static final class ValueBuilder extends Node.Builder<Node, ValueBuilder> {

        private final Node.Builder<?, ?> parent;
        private final String qName;

        ValueBuilder(Node.Builder<?, ?> parent, String qName) {
            super(parent.info());
            this.parent = parent;
            this.qName = qName;
        }

        @Override
        public ValueBuilder value(String value) {
            parent.attribute(qName, DynamicValue.create(value));
            return this;
        }

        @Override
        protected Node doBuild() {
            return null;
        }
    }
}
