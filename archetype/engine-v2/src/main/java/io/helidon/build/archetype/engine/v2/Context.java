/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.common.GenericType;

import static io.helidon.build.common.PropertyEvaluator.evaluate;

/**
 * Context.
 * Holds the state for an archetype invocation.
 *
 * <ul>
 *     <li>Used to maintain the current working directory for resolving files and scripts.</li>
 *     <li>Used to maintain the input nesting hierarchy</li>
 *     <li>Used to maintain the values (external, internal, pushed)</li>
 * </ul>
 */
public final class Context {

    private static final Path NULL_PATH = Path.of("");

    private final Map<String, Value> defaults = new HashMap<>();
    private final Map<String, ContextValue> values = new HashMap<>();
    private final Deque<Path> directories = new ArrayDeque<>();
    private final Deque<String> inputs = new ArrayDeque<>();

    private Context(Path cwd, Map<String, String> externalValues, Map<String, String> externalDefaults) {
        if (externalDefaults != null) {
            externalDefaults.forEach((k, v) -> defaults.put(k, externalValue(v)));
        }
        if (externalValues != null) {
            externalValues.forEach((k, v) -> values.put(k, externalValue(evaluate(v, externalValues::get))));
        }
        directories.push(cwd == null ? NULL_PATH : cwd);
    }

    /**
     * Push a new working directory.
     *
     * @param dir directory
     */
    public void pushCwd(Path dir) {
        directories.push(dir.toAbsolutePath());
    }

    /**
     * Pop the current working directory.
     *
     * @throws IllegalStateException if the current cwd is the initial one
     */
    public void popCwd() {
        if (directories.size() == 1) {
            throw new IllegalStateException("Cannot pop the initial working directory");
        }
        directories.pop();
    }

    /**
     * Get the current working directory.
     *
     * @return path
     */
    public Path cwd() {
        return directories.peek();
    }

    /**
     * Get an external default value.
     *
     * @param name input name
     * @return value
     */
    public Value defaultValue(String name) {
        Value defaultValue = defaults.get(path(name));
        String wrapped = defaultValue != null ? defaultValue.asString() : null;
        if (wrapped != null) {
            defaultValue = Value.create(substituteVariables(wrapped));
        }
        return defaultValue;
    }

    /**
     * Put a value in the context.
     *
     * @param path     input path
     * @param value    value
     * @param readonly true if the value is read-only
     * @throws IllegalStateException if a value already exists
     */
    public void put(String path, Value value, boolean readonly) {
        ContextValue current = values.get(path);
        if (current == null || !current.readonly) {
            values.put(path, new ContextValue(value, readonly));
        } else if (!current.unwrap().equals(value.unwrap())) {
            throw new IllegalStateException(String.format(
                    "%s requires %s=%s", fullPath(), path, value.unwrap()));
        }
    }

    /**
     * Get a value by input path.
     *
     * @param path input path
     * @return value, or {@code null}
     */
    public Value get(String path) {
        return values.get(path);
    }

    /**
     * Push the given input path.
     *
     * @param path   input path
     * @param global true if the input is global
     */
    public void push(String path, boolean global) {
        if (!global) {
            inputs.push(path);
        }
    }

    /**
     * Push a new input value.
     *
     * @param name   input name
     * @param value  value
     * @param global true if the input is global
     */
    public void push(String name, Value value, boolean global, boolean readonly) {
        String path = path(name);
        if (value != null) {
            values.put(path, new ContextValue(value, readonly));
            push(path, global);
        } else if (values.get(path) != null) {
            push(path, global);
        }
    }

    /**
     * Pop the current input.
     */
    public void pop() {
        inputs.pop();
    }

    /**
     * Compute the relative input path for the given query.
     *
     * @param query input path query
     * @return value, {@code null} if not found
     */
    public String queryPath(String query) {
        String current = inputs.peek();
        if (current == null) {
            current = "";
        }
        String key;
        if (query.startsWith("ROOT.")) {
            key = query.substring(5);
        } else {
            int offset = 0;
            int level = 0;
            while (query.startsWith("PARENT.", offset)) {
                offset += 7;
                level++;
            }
            if (offset > 0) {
                query = query.substring(offset);
            } else {
                level = 0;
            }
            int index;
            for (index = current.length() - 1; index >= 0 && level > 0; index--) {
                if (current.charAt(index) == '.') {
                    level--;
                }
            }
            if (index > 0) {
                key = current.substring(0, index + 1) + "." + query;
            } else {
                key = query;
            }
        }
        return key;
    }

    /**
     * Lookup a context value.
     *
     * @param path input path
     * @return value, {@code null} if not found
     */
    public Value lookup(String path) {
        return values.get(queryPath(path));
    }

    /**
     * Compute the path for a given input name.
     *
     * @param name name
     * @return input path
     */
    public String path(String name) {
        String path = inputs.peek();
        if (path != null) {
            path += "." + name;
        } else {
            path = name;
        }
        return path;
    }

    /**
     * Returns the current input path.
     *
     * @return input path
     */
    public String path() {
        return inputs.peek();
    }

    /**
     * Substitute the context variables within the given string.
     *
     * @param value string to process
     * @return processed string
     * @throws IllegalArgumentException if the string contains any unresolved variable
     */
    public String substituteVariables(String value) {
        if (value == null) {
            return null;
        }
        return evaluate(value, var -> {
            Value val = lookup(var);
            if (val == null) {
                throw new IllegalArgumentException("Unresolved variable: " + var);
            }
            return String.valueOf(val.unwrap());
        });
    }

    /**
     * Ensure that {@code inputs} is empty.
     *
     * @throws IllegalStateException if {@code inputs} is not empty
     */
    public void ensureEmptyInputs() {
        if (!inputs.isEmpty()) {
            throw new IllegalStateException("Invalid state, inputs is not empty: " + inputs);
        }
    }

    /**
     * If the given node is an instance of {@link Condition}, evaluate the expression.
     *
     * @param node node
     * @return {@code true} if the node is not an instance of {@link Condition} or the expression result
     */
    public boolean filterNode(Node node) {
        return Condition.filter(node, this::lookup);
    }

    private String fullPath() {
        StringBuilder sb = new StringBuilder();
        String[] inputsArray = inputs.toArray(new String[0]);
        for (int i = inputsArray.length - 1; i >= 0; i--) {
            String input = inputsArray[i];
            sb.append(input).append("=").append(values.get(input).unwrap());
            if (i > 0) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /**
     * Create a new context.
     *
     * @return context
     */
    static Context create() {
        return create(null, null, null);
    }

    /**
     * Create a new context.
     *
     * @param cwd initial working directory
     * @return context
     */
    public static Context create(Path cwd) {
        return new Context(cwd, null, null);
    }

    /**
     * Create a new context.
     *
     * @param cwd              initial working directory
     * @param externalValues   external values, must be non {code null}
     * @param externalDefaults external defaults, must be non {code null}
     * @return context
     * @throws NullPointerException if externalValues or externalDefaults is {@code null}
     */
    public static Context create(Path cwd, Map<String, String> externalValues, Map<String, String> externalDefaults) {
        return new Context(cwd, externalValues, externalDefaults);
    }

    private static ContextValue externalValue(String value) {
        return new ContextValue(DynamicValue.create(value), true);
    }

    /**
     * Context value.
     */
    private static final class ContextValue implements Value {

        private final boolean readonly;
        private final Value value;

        private ContextValue(Value value, boolean readonly) {
            this.value = value;
            this.readonly = readonly;
        }

        @Override
        public Boolean asBoolean() {
            return value.asBoolean();
        }

        @Override
        public String asString() {
            return value.asString();
        }

        @Override
        public Integer asInt() {
            return value.asInt();
        }

        @Override
        public List<String> asList() {
            return value.asList();
        }

        @Override
        public Object unwrap() {
            return value.unwrap();
        }

        @Override
        public GenericType<?> type() {
            return value.type();
        }

        @Override
        public <U> U as(GenericType<U> type) {
            return value.as(type);
        }

        @Override
        public String toString() {
            return "ContextValue{" +
                    "readonly=" + readonly +
                    ", value=" + value +
                    '}';
        }
    }
}
