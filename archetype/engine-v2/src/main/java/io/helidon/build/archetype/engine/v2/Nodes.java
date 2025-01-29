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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.common.Maps;

/**
 * {@link Node} utility methods.
 */
public class Nodes {

    private Nodes() {
    }

    /**
     * Create a {@link Kind#OUTPUT} node.
     *
     * @param children children
     * @return Node
     */
    public static Node output(Node... children) {
        return node(Kind.OUTPUT, children);
    }

    /**
     * Create a {@link Kind#MODEL} node.
     *
     * @param children children
     * @return Node
     */
    public static Node model(Node... children) {
        return node(Kind.MODEL, children);
    }

    /**
     * Create a {@link Kind#MODEL_MAP} node.
     *
     * @param children children
     * @return Node
     */
    public static Node modelMap(Node... children) {
        return node(Kind.MODEL_MAP, children);
    }

    /**
     * Create a {@link Kind#MODEL_MAP} node.
     *
     * @param key      model key
     * @param children children
     * @return Node
     */
    public static Node modelMap(String key, Node... children) {
        return modelMap(children).attribute("key", key);
    }

    /**
     * Create a {@link Kind#MODEL_LIST} node.
     *
     * @param children children
     * @return Node
     */
    public static Node modelList(Node... children) {
        return node(Kind.MODEL_LIST, children);
    }

    /**
     * Create a {@link Kind#MODEL_LIST} node.
     *
     * @param key      model key
     * @param children children
     * @return Node
     */
    public static Node modelList(String key, Node... children) {
        return modelList(children).attribute("key", key);
    }

    /**
     * Create a {@link Kind#MODEL_VALUE} node.
     *
     * @param value value
     * @return Node
     */
    public static Node modelValue(String value) {
        return node(Kind.MODEL_VALUE).value(value);
    }

    /**
     * Create a {@link Kind#MODEL_VALUE} node.
     *
     * @param value value
     * @param order order
     * @return Node
     */
    public static Node modelValue(String value, int order) {
        return modelValue(value).attribute("order", String.valueOf(order));
    }

    /**
     * Create a {@link Kind#MODEL_VALUE} node.
     *
     * @param key   key
     * @param value value
     * @return Node
     */
    public static Node modelValue(String key, String value) {
        return modelValue(value).attribute("key", key);
    }

    /**
     * Create a {@link Kind#MODEL_VALUE} node.
     *
     * @param key   key
     * @param value value
     * @param order order
     * @return Node
     */
    public static Node modelValue(String key, String value, int order) {
        return modelValue(key, value).attribute("order", String.valueOf(order));
    }

    /**
     * Create a {@link Kind#STEP} node.
     *
     * @param name     step name
     * @param children children
     * @return Node
     */
    public static Node step(String name, Node... children) {
        return step(name, b -> {
        }, children);
    }

    /**
     * Create a {@link Kind#STEP} node.
     *
     * @param name     step name
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node step(String name, Consumer<Node> consumer, Node... children) {
        return node(Kind.STEP, b -> {
            b.attribute("name", name);
            consumer.accept(b);
        }, children);
    }

    /**
     * Create a {@link Kind#INPUTS} node.
     *
     * @param children children
     * @return Node
     */
    public static Node inputs(Node... children) {
        return node(Kind.INPUTS, children);
    }

    /**
     * Create a {@link Kind#INPUT_OPTION} node.
     *
     * @param name     option name
     * @param value    option value
     * @param children children
     * @return Node
     */
    public static Node inputOption(String name, String value, Node... children) {
        return inputOption(name, value, b -> {
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_OPTION} node.
     *
     * @param name     option name
     * @param value    option value
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node inputOption(String name, String value, Consumer<Node> consumer, Node... children) {
        return node(Kind.INPUT_OPTION, b -> {
            b.value(value);
            b.attribute("name", name);
            consumer.accept(b);
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_TEXT} node.
     *
     * @param id       input id
     * @param children children
     * @return Node
     */
    public static Node inputText(String id, Node... children) {
        return inputText(id, b -> {
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_TEXT} node.
     *
     * @param id       input id
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node inputText(String id, Consumer<Node> consumer, Node... children) {
        return node(Kind.INPUT_TEXT, b -> {
            b.attribute("id", id);
            consumer.accept(b);
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_BOOLEAN} node.
     *
     * @param id       input id
     * @param children children
     * @return Node
     */
    public static Node inputBoolean(String id, Node... children) {
        return inputBoolean(id, b -> {
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_BOOLEAN} node.
     *
     * @param id       input id
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node inputBoolean(String id, Consumer<Node> consumer, Node... children) {
        return node(Kind.INPUT_BOOLEAN, b -> {
            b.attribute("id", id);
            consumer.accept(b);
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_ENUM} node.
     *
     * @param id       input id
     * @param children children
     * @return Node
     */
    public static Node inputEnum(String id, Node... children) {
        return inputEnum(id, b -> {
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_ENUM} node.
     *
     * @param id       input id
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node inputEnum(String id, Consumer<Node> consumer, Node... children) {
        return node(Kind.INPUT_ENUM, b -> {
            b.attribute("id", id);
            consumer.accept(b);
        }, children);
    }

    /**
     * Create a {@link Kind#INPUT_LIST} node.
     *
     * @param id       input id
     * @param children children
     * @return Node
     */
    public static Node inputList(String id, Node... children) {
        return inputList(id, b -> b.attribute("id", id), children);
    }

    /**
     * Create a {@link Kind#INPUT_LIST} node.
     *
     * @param id       input id
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node inputList(String id, Consumer<Node> consumer, Node... children) {
        return node(Kind.INPUT_LIST, b -> {
            b.attribute("id", id);
            consumer.accept(b);
        }, children);
    }

    /**
     * Create a {@link Kind#METHOD} node.
     *
     * @param name     name
     * @param children children
     * @return Node
     */
    public static Node method(String name, Node... children) {
        return node(Kind.METHOD, children)
                .attribute("name", name);
    }

    /**
     * Create a {@link Kind#CALL} node.
     *
     * @param method method
     * @return Node
     */
    public static Node call(String method) {
        return node(Kind.CALL)
                .attribute("method", method);
    }

    /**
     * Create a {@link Kind#SOURCE} node.
     *
     * @param src src
     * @return Node
     */
    public static Node source(String src) {
        return node(Kind.SOURCE)
                .attribute("src", src);
    }

    /**
     * Create a {@link Kind#EXEC} node.
     *
     * @param src src
     * @return Node
     */
    public static Node exec(String src) {
        return node(Kind.EXEC)
                .attribute("src", src);
    }

    /**
     * Create a {@link Kind#PRESETS} node.
     *
     * @param children children
     * @return Node
     */
    public static Node presets(Node... children) {
        return node(Kind.PRESETS, children);
    }

    /**
     * Create a {@link Kind#PRESET_BOOLEAN} node.
     *
     * @param path  path
     * @param value value
     * @return Node
     */
    public static Node presetBoolean(String path, boolean value) {
        return node(Kind.PRESET_BOOLEAN)
                .attribute("path", path)
                .value(Value.of(value));
    }

    /**
     * Create a {@link Kind#PRESET_ENUM} node.
     *
     * @param path  path
     * @param value value
     * @return Node
     */
    public static Node presetEnum(String path, String value) {
        return node(Kind.PRESET_ENUM)
                .attribute("path", path)
                .value(value);
    }

    /**
     * Create a {@link Kind#PRESET_TEXT} node.
     *
     * @param path  path
     * @param value value
     * @return Node
     */
    public static Node presetText(String path, String value) {
        return node(Kind.PRESET_TEXT)
                .attribute("path", path)
                .value(value);
    }

    /**
     * Create a {@link Kind#PRESET_LIST} node.
     *
     * @param path   path
     * @param values values
     * @return Node
     */
    public static Node presetList(String path, List<String> values) {
        return node(Kind.PRESET_LIST)
                .attribute("path", path)
                .value(Value.of(values));
    }

    /**
     * Create a {@link Kind#VARIABLES} node.
     *
     * @param children children
     * @return Node
     */
    public static Node variables(Node... children) {
        return node(Kind.VARIABLES, children);
    }

    /**
     * Create a variable with default value from a value type.
     *
     * @param type value type
     * @param path path
     * @return Node
     */
    public static Node variable(Value.Type type, String path) {
        switch (type) {
            case BOOLEAN:
                return Nodes.variableBoolean(path, false);
            case STRING:
                return Nodes.variableText(path, "");
            case LIST:
                return Nodes.variableList(path, List.of());
            default:
                throw new IllegalArgumentException("Unsupported variable type: " + type);
        }
    }

    /**
     * Create a {@link Kind#VARIABLE_BOOLEAN} node.
     *
     * @param path  path
     * @param value value
     * @return Node
     */
    public static Node variableBoolean(String path, boolean value) {
        return node(Kind.VARIABLE_BOOLEAN)
                .attribute("path", path)
                .value(Value.of(value));
    }

    /**
     * Create a {@link Kind#VARIABLE_ENUM} node.
     *
     * @param path  path
     * @param value value
     * @return Node
     */
    public static Node variableEnum(String path, String value) {
        return node(Kind.VARIABLE_ENUM)
                .attribute("path", path)
                .value(value);
    }

    /**
     * Create a {@link Kind#VARIABLE_TEXT} node.
     *
     * @param path  path
     * @param value value
     * @return Node
     */
    public static Node variableText(String path, String value) {
        return node(Kind.VARIABLE_TEXT)
                .attribute("path", path)
                .value(value);
    }

    /**
     * Create a {@link Kind#VARIABLE_LIST} node.
     *
     * @param path   path
     * @param values values
     * @return Node
     */
    public static Node variableList(String path, List<String> values) {
        return node(Kind.VARIABLE_LIST)
                .attribute("path", path)
                .value(Value.of(values));
    }

    /**
     * Create a {@link Kind#VALIDATIONS} node.
     *
     * @param children children
     * @return Node
     */
    public static Node validations(Node... children) {
        return node(Kind.VALIDATIONS, children);
    }

    /**
     * Create a {@link Kind#VALIDATION} node.
     *
     * @param id       id
     * @param children children
     * @return Node
     */
    public static Node validation(String id, Node... children) {
        return node(Kind.VALIDATION, children)
                .attribute("id", id);
    }

    /**
     * Create a {@link Kind#VALIDATION} node.
     *
     * @param id          id
     * @param description description
     * @param children    children
     * @return Node
     */
    public static Node validation(String id, String description, Node... children) {
        return validation(id, children).attribute("description", description);
    }

    /**
     * Create a {@link Kind#REGEX} node.
     *
     * @param regex regex
     * @return Node
     */
    public static Node regex(String regex) {
        return node(Kind.REGEX).value(regex);
    }

    /**
     * Create a {@link Kind#CONDITION} node.
     *
     * @param expression expression
     * @param children   children
     * @return Node
     */
    public static Node condition(String expression, Node... children) {
        return condition(Expression.create(expression), children);
    }

    /**
     * Create a {@link Kind#CONDITION} node.
     *
     * @param expression expression
     * @param children   children
     * @return Node
     */
    public static Node condition(Expression expression, Node... children) {
        if (expression == Expression.TRUE && children.length == 1) {
            return children[0];
        }
        return node(Kind.CONDITION, children)
                .expression(expression);
    }

    /**
     * Create a {@link Kind#TRANSFORMATION} node.
     *
     * @param id       id
     * @param children children
     * @return Node
     */
    public static Node transformation(String id, Node... children) {
        return node(Kind.TRANSFORMATION, children)
                .attribute("id", id);
    }

    /**
     * Create a {@link Kind#TRANSFORMATION} node.
     *
     * @param id       id
     * @param children children
     * @return Node
     */
    public static Node transformation(String id, List<Node> children) {
        return node(Kind.TRANSFORMATION, children)
                .attribute("id", id);
    }

    /**
     * Create a {@link Kind#REPLACE} node.
     *
     * @param regex       regex
     * @param replacement replacement
     * @return Node
     */
    public static Node replace(String regex, String replacement) {
        return node(Kind.REPLACE)
                .attribute("regex", regex)
                .attribute("replacement", replacement);
    }

    /**
     * Create a {@link Kind#TEMPLATES} node.
     *
     * @param dir             directory
     * @param transformations transformations
     * @param children        children
     * @return Node
     */
    public static Node templates(String dir, List<String> transformations, Node... children) {
        return templates("mustache", dir, transformations, children);
    }

    /**
     * Create a {@link Kind#TEMPLATES} node.
     *
     * @param engine          engine
     * @param dir             directory
     * @param transformations transformations
     * @param children        children
     * @return Node
     */
    public static Node templates(String engine, String dir, List<String> transformations, Node... children) {
        return templates(engine, dir, children).attribute("transformations", transformations);
    }

    /**
     * Create a {@link Kind#TEMPLATES} node.
     *
     * @param engine   engine
     * @param dir      directory
     * @param children children
     * @return Node
     */
    public static Node templates(String engine, String dir, Node... children) {
        return node(Kind.TEMPLATES, children)
                .attribute("engine", engine)
                .attribute("directory", dir);
    }

    /**
     * Create a {@link Kind#TEMPLATE} node.
     *
     * @param engine   engine
     * @param source   source
     * @param target   target
     * @param children children
     * @return Node
     */
    public static Node template(String engine, String source, String target, Node... children) {
        return node(Kind.TEMPLATE, children)
                .attribute("engine", engine)
                .attribute("source", source)
                .attribute("target", target);
    }

    /**
     * Create a {@link Kind#FILES} node.
     *
     * @param dir      directory
     * @param children children
     * @return Node
     */
    public static Node files(String dir, Node... children) {
        return node(Kind.FILES, children)
                .attribute("directory", dir);
    }

    /**
     * Create a {@link Kind#FILES} node.
     *
     * @param dir             directory
     * @param transformations transformations
     * @param children        children
     * @return Node
     */
    public static Node files(String dir, List<String> transformations, Node... children) {
        return files(dir, children)
                .attribute("transformations", transformations);
    }

    /**
     * Create a {@link Kind#FILE} node.
     *
     * @param source   source
     * @param target   target
     * @param children children
     * @return Node
     */
    public static Node file(String source, String target, Node... children) {
        return node(Kind.FILE, children)
                .attribute("source", source)
                .attribute("target", target);
    }

    /**
     * Create a {@link Kind#INCLUDES} node.
     *
     * @param children children
     * @return Node
     */
    public static Node includes(Node... children) {
        return node(Kind.INCLUDES, children);
    }

    /**
     * Create a {@link Kind#INCLUDES} node.
     *
     * @param children children
     * @return Node
     */
    public static Node includes(List<Node> children) {
        return node(Kind.INCLUDES, children);
    }

    /**
     * Create a {@link Kind#INCLUDE} node.
     *
     * @param include exclude
     * @return Node
     */
    public static Node include(String include) {
        return node(Kind.INCLUDE).value(include);
    }

    /**
     * Create a {@link Kind#EXCLUDE} node.
     *
     * @param exclude exclude
     * @return Node
     */
    public static Node exclude(String exclude) {
        return node(Kind.EXCLUDE).value(exclude);
    }

    /**
     * Create a {@link Kind#EXCLUDES} node.
     *
     * @param children children
     * @return Node
     */
    public static Node excludes(Node... children) {
        return node(Kind.EXCLUDES, children);
    }

    /**
     * Create a {@link Kind#SCRIPT} node.
     *
     * @param children children
     * @return Node
     */
    public static Node script(Node... children) {
        Script script = new Script(new HashMap<>());
        Node.Builder node = Node.builder(Kind.SCRIPT).script(script);
        for (Node child : children) {
            child.parent(node);
            child.script(script);
            node.children().add(child);
        }
        return node;
    }

    /**
     * Get or create the last child of a node.
     *
     * @param block node
     * @param kind  kind
     * @return Node
     */
    public static Node ensureLast(Node block, Kind kind) {
        Node node = block.lastChild(kind::equals).orElse(null);
        if (node == null) {
            node = Node.builder(kind);
            block.append(node);
        }
        return node;
    }

    /**
     * Get or create a node before the given node.
     *
     * @param block block
     * @param kind  kind
     * @return Node
     */
    public static Node ensureBefore(Node block, Kind kind) {
        Node container;
        int index = block.index();
        if (index == 0) {
            container = Node.builder(kind);
            block.parent().append(0, container);
        } else {
            container = block.parent().children().get(index - 1);
            if (container.kind() != kind) {
                container = Nodes.variables();
                block.parent().append(index, container);
            }
        }
        return container;
    }

    /**
     * Set the parent on the given children.
     *
     * @param parent parent
     * @param node   node
     * @return Node
     */
    public static Node parent(Node node, Node parent) {
        node.parent(parent);
        if (parent != null) {
            node.script(parent.script());
        }
        return node;
    }

    /**
     * Create a new node.
     *
     * @param kind     kind
     * @param children children
     * @return Node
     */
    public static Node node(Kind kind, Node... children) {
        return node(kind, Arrays.asList(children));
    }

    /**
     * Create a new node.
     *
     * @param kind     kind
     * @param children children
     * @return Node
     */
    public static Node node(Kind kind, List<Node> children) {
        Node.Builder node = Node.builder(kind);
        for (Node child : children) {
            parent(child, node);
            node.children().add(child);
        }
        return node;
    }

    /**
     * Create a new node.
     *
     * @param kind     kind
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node node(Kind kind, Consumer<Node> consumer, Node... children) {
        return node(kind, consumer, Arrays.asList(children));
    }

    /**
     * Create a new node.
     *
     * @param kind     kind
     * @param consumer consumer
     * @param children children
     * @return Node
     */
    public static Node node(Kind kind, Consumer<Node> consumer, List<Node> children) {
        Node node = node(kind, children);
        consumer.accept(node);
        return node;
    }

    /**
     * Get the option nodes of an option input.
     *
     * @param node node
     * @return nodes
     */
    public static List<Node> options(Node node) {
        return options(node, n -> true);
    }

    /**
     * Get the option nodes of an option input.
     *
     * @param node      node
     * @param predicate predicate
     * @return nodes
     */
    public static List<Node> options(Node node, Predicate<Node> predicate) {
        return node.children().stream()
                .filter(predicate)
                .map(Node::unwrap)
                .filter(n -> n.kind() == Kind.INPUT_OPTION)
                .collect(Collectors.toList());
    }

    /**
     * Get the index of the option with the given name.
     *
     * @param name    name
     * @param options options
     * @return index or {@code -1} if not found
     */
    public static int optionIndex(String name, List<Node> options) {
        if (name != null) {
            for (ListIterator<Node> it = options.listIterator(); it.hasNext();) {
                if (it.next().value().getString().equalsIgnoreCase(name)) {
                    return it.previousIndex();
                }
            }
        }
        return -1;
    }

    /**
     * Compute the hash code for a node.
     *
     * @param node node
     * @return hash
     */
    public static int hash(Node node) {
        return Objects.hash(
                node.kind(),
                Maps.mapValue(node.attributes(), v -> Value.toString(v)),
                Value.toString(node.value()));
    }
}
