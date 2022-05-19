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
import java.util.Map;
import java.util.NoSuchElementException;

import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.common.GenericType;

import static io.helidon.build.common.PropertyEvaluator.evaluate;

/**
 * Context.
 * Holds the state for an archetype invocation.
 *
 * <ul>
 *     <li>Maintains the current working directory for resolving files and scripts.</li>
 *     <li>Maintains the scope for resolving values and variables</li>
 *     <li>Maintains the values and variables</li>
 * </ul>
 */
public final class Context {

    private static final Path NULL_PATH = Path.of("");

    private Scope scope = Scope.ROOT;
    private final Map<String, Value> defaults = new HashMap<>();
    private final Map<String, Value> variables = new HashMap<>();
    private final Map<String, ContextValue> values = new HashMap<>();
    private final Deque<Path> directories = new ArrayDeque<>();

    private Context(Path cwd, Map<String, String> externalValues, Map<String, String> externalDefaults) {
        if (externalDefaults != null) {
            externalDefaults.forEach((k, v) -> defaults.put(k, ContextValue.external(v)));
        }
        if (externalValues != null) {
            externalValues.forEach((k, v) -> values.put(k, ContextValue.external(evaluate(v, externalValues::get))));
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
     * @param id     input id
     * @param global global
     * @return value
     */
    public Value defaultValue(String id, boolean global) {
        Value defaultValue = defaults.get(path(id, global));
        String wrapped = defaultValue != null ? defaultValue.asString() : null;
        if (wrapped != null) {
            defaultValue = Value.create(substituteVariables(wrapped));
        }
        return defaultValue;
    }

    /**
     * Set a value in the context.
     *
     * @param path     input path
     * @param newValue value
     * @param kind     value kind
     * @throws IllegalStateException if a value already exists
     */
    public void setValue(String path, Value newValue, ContextValue.ValueKind kind) {
        ContextValue currentValue = values.get(path);
        if (currentValue == null || !currentValue.isReadOnly()) {
            values.put(path, new ContextValue(newValue, kind));
        } else {
            GenericType<?> type = currentValue.type();
            if (type == null) {
                type = newValue.type();
            }
            if (type == null) {
                type = ValueTypes.STRING;
            }
            Object currentVal = currentValue.as(type);
            Object newVal = newValue.as(type);
            if (!currentVal.equals(newVal)) {
                String scopeInfo = scopeInfo();
                if (scopeInfo.isEmpty()) {
                    throw new IllegalStateException(String.format(
                            "Cannot set %s=%s, value already exists", path, newVal));
                } else {
                    throw new IllegalStateException(String.format(
                            "%s requires %s=%s", scopeInfo, path, currentVal));
                }
            }
        }
    }

    /**
     * Set a variable.
     *
     * @param id    variable id
     * @param value variable value
     */
    public void setVariable(String id, Value value) {
        variables.put(id, value);
    }

    /**
     * Get a value.
     *
     * @param key value key
     * @return value, or {@code null} if not found
     */
    public Value getValue(String key) {
        return values.get(key);
    }

    /**
     * Create a new scope.
     *
     * @param id     scope identifier, must be non {@code null}
     * @param global {@code true} if the scope is global, {@code false} otherwise
     * @return scope
     * @throws IllegalArgumentException if id is {@code null}
     */
    public Scope newScope(String id, boolean global) {
        if (id == null) {
            throw new IllegalArgumentException("Scope id is null");
        }
        return new Scope(this.scope, id, global);
    }

    /**
     * Push a scope.
     *
     * @param scope scope
     */
    public void pushScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * Get the current scope.
     *
     * @return Scope
     */
    public Scope peekScope() {
        return this.scope;
    }

    /**
     * Pop the current scope.
     *
     * @throws NoSuchElementException if the current scope is the root scope
     */
    public void popScope() {
        if (this.scope == Scope.ROOT) {
            throw new NoSuchElementException();
        }
        this.scope = this.scope.parent;
    }

    /**
     * Lookup a value.
     *
     * @param query query
     * @return value, {@code null} if not found
     */
    public Value lookup(String query) {
        String key = resolveQuery(query);
        Value value = variables.get(key);
        if (value != null) {
            return value;
        }
        return values.get(key);
    }

    /**
     * Compute the path for a given input name.
     *
     * @param id     input id
     * @param global global
     * @return key
     */
    public String path(String id, boolean global) {
        if (global) {
            return id;
        }
        return path(scope, id);
    }

    private static String path(Scope scope, String id) {
        return scope.global ? id : scope.id + "." + id;
    }

    /**
     * Compute the value key for a given query.
     *
     * @param query query
     * @return value key
     */
    public String resolveQuery(String query) {
        String scopeId = scope.global ? "" : scope.id();
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
            for (index = scopeId.length() - 1; index >= 0 && level > 0; index--) {
                if (scopeId.charAt(index) == '.') {
                    level--;
                }
            }
            if (index > 0) {
                key = scopeId.substring(0, index + 1) + "." + query;
            } else {
                key = query;
            }
        }
        return key;
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
     * If the given node is an instance of {@link Condition}, evaluate the expression.
     *
     * @param node node
     * @return {@code true} if the node is not an instance of {@link Condition} or the expression result
     */
    public boolean filterNode(Node node) {
        return Condition.filter(node, this::lookup);
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

    private String scopeInfo() {
        StringBuilder sb = new StringBuilder();
        Scope scope = this.scope;
        while (scope.id != null) {
            Value value = values.get(scope.id);
            sb.append(scope.id).append("=").append(value != null ? value.unwrap() : "null");
            if (scope.parent.id != null) {
                sb.append(";");
            }
            scope = scope.parent;
        }
        return sb.toString();
    }

    /**
     * Context scope.
     * Tree nodes for the context values.
     */
    public static final class Scope {

        /**
         * Root scope.
         */
        public static final Scope ROOT = new Scope(null, null, true);

        private final Scope parent;
        private final String id;
        private final boolean global;

        private Scope(Scope parent, String id, boolean global) {
            this.global = global;
            if (parent != null) {
                this.parent = parent;
                this.id = global ? id : path(parent, id);
            } else {
                this.parent = null;
                this.id = null;
            }
        }

        /**
         * Get the scope identifier.
         *
         * @return scope id
         */
        public String id() {
            return id;
        }

        /**
         * Test if this scope is global.
         *
         * @return {@code true} if the scope is global, {@code false} otherwise
         */
        @SuppressWarnings("unused")
        public boolean isGlobal() {
            return global;
        }
    }
}
