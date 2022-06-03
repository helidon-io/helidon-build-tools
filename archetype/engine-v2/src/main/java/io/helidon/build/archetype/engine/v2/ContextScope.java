/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import io.helidon.build.archetype.engine.v2.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.common.GenericType;

import static io.helidon.build.common.PropertyEvaluator.evaluate;

/**
 * Context scope.
 * Tree nodes of context values.
 *
 * @see Context
 * @see ContextPath
 */
public final class ContextScope {

    /**
     * Scope visibility.
     */
    public enum Visibility {

        /**
         * Global visibility.
         * Global parent scopes are not included in the effective path, see {@link ContextScope#path(String)}.
         * Values are looked-up by traversing the root scope (breadth-first) until the first level of non-global children.
         */
        GLOBAL,

        /**
         * Local visibility.
         * Values are looked-up strictly within the scope (not using children).
         */
        LOCAL,

        /**
         * Unset visibility.
         * Behaves like {@link #LOCAL}, but can be changed later on to either {@link #GLOBAL} or {@link #LOCAL}.
         */
        UNSET
    }

    private final ContextScope root;
    private final ContextScope parent;
    private final String id;
    private Visibility visibility;
    private final Map<String, ContextScope> children = new HashMap<>();
    private final Map<String, ContextValue> values = new HashMap<>();

    private ContextScope(ContextScope parent, ContextScope root, String id, Visibility visibility) {
        this.visibility = visibility;
        if (parent != null) {
            this.parent = parent;
            this.root = Objects.requireNonNull(root, "root is null");
            if (id == null || id.isEmpty() || id.indexOf('.') >= 0) {
                throw new IllegalArgumentException("Invalid scope id");
            }
            this.id = id;
        } else {
            this.root = this;
            this.parent = null;
            this.id = null;
        }
    }

    /**
     * Create a new root scope.
     *
     * @return scope
     */
    public static ContextScope create() {
        return new ContextScope(null, null, null, Visibility.GLOBAL);
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
     * Get the scope visibility.
     *
     * @return visibility
     */
    public Visibility visibility() {
        return visibility;
    }

    /**
     * Get the parent scope.
     *
     * @return parent, or {@code null} if this scope is a root scope.
     */
    public ContextScope parent() {
        return parent;
    }

    /**
     * Get the root scope.
     *
     * @return root scope, never {@code null}
     */
    public ContextScope root() {
        return root;
    }

    /**
     * Get or create a new scope.
     *
     * @param path   scope path, must be non {@code null} and non-empty
     * @param global {@code true} if the scope is global, {@code false} otherwise
     * @return scope
     * @throws IllegalArgumentException if path is {@code null}
     */
    public ContextScope getOrCreate(String path, boolean global) {
        return getOrCreate(path, global ? Visibility.GLOBAL : Visibility.LOCAL);
    }

    /**
     * Get or create a new scope.
     * If the path contains more than one segment (delimited by {@code .}), the intermediate scopes are implicitly
     * created.
     *
     * @param path       scope path, must be non {@code null} and non-empty
     * @param visibility scope visibility
     * @return scope
     * @throws NullPointerException     if path is null
     * @throws IllegalArgumentException if path is invalid
     * @throws IllegalStateException    if a scope already exists and the requested visibility doesn't match
     */
    public ContextScope getOrCreate(String path, Visibility visibility) {
        ContextPath contextPath = ContextPath.create(path);
        String[] segments = contextPath.segments();
        if (segments.length == 0) {
            return root;
        }
        String id = segments[segments.length - 1];
        if (id.indexOf('.') > 0) {
            throw new IllegalArgumentException("Invalid scope id: " + id);
        }
        ContextScope parent = findScope(contextPath, (s, sid) -> s.getOrCreate0(sid, visibility));
        return parent.getOrCreate0(id, visibility);
    }

    /**
     * Set a value in the context.
     *
     * @param key   value key
     * @param value the value
     * @param kind  value kind
     * @throws IllegalStateException if a non readonly value already exists
     */
    public void put(String key, Value value, ValueKind kind) {
        if (key.indexOf('.') >= 0) {
            throw new IllegalArgumentException("key must not contain '.'");
        }
        ContextValue currentValue = values.get(key);
        if (currentValue == null || !currentValue.isReadOnly()) {
            values.put(key, new ContextValue(value, kind));
        } else {
            GenericType<?> type = currentValue.type();
            if (type == null) {
                type = value.type();
            }
            if (type == null) {
                type = ValueTypes.STRING;
            }
            Object currentVal = currentValue.as(type);
            Object newVal = value.as(type);
            if (!currentVal.equals(newVal)) {
                throw new IllegalStateException(String.format(
                        "Cannot set value, key=%s, current={kind=%s, %s}, new={kind=%s, %s}",
                        key,
                        currentValue.kind(),
                        currentValue.wrapped(),
                        kind,
                        value));
            }
        }
    }

    /**
     * Lookup a value.
     *
     * @param path path
     * @return effective path
     * @throws NullPointerException     if path is null
     * @throws IllegalArgumentException if path is invalid
     */
    public Value get(String path) {
        ContextPath contextPath = valuePath(path);
        // TODO bi-function should traverse global children and first level of non global to find sid
        ContextScope scope = findScope(contextPath, (s, sid) -> s.children.get(sid));
        // TODO BFS traverse global children to find id
        return scope.values.get(id);
    }

    /**
     * Compute the effective path of a value.
     * Scopes with visibility is {@link Visibility#GLOBAL} are not included.
     *
     * @param path path
     * @return effective path
     * @throws NullPointerException     if path is null
     * @throws IllegalArgumentException if path is invalid
     */
    public String path(String path) {
        ContextPath contextPath = valuePath(path);
        ContextScope scope = findScope(contextPath, (s, sid) -> s.children.get(sid));
        StringBuilder resolved = new StringBuilder(id);
        while (scope.parent != null) {
            if (scope.visibility != Visibility.GLOBAL) {
                resolved.insert(0, scope.id + ".");
            }
            scope = scope.parent;
        }
        return resolved.toString();
    }

    /**
     * Substitute the properties of the form {@code ${key}} within the given string.
     * Properties are substituted until the resulting string does not contain any references.
     *
     * @param value string to process
     * @return processed string
     * @throws IllegalArgumentException if the string contains any unresolved variable
     */
    public String interpolate(String value) {
        if (value == null) {
            return null;
        }
        String input = null;
        String output = value;
        while (!output.equals(input)) {
            input = output;
            output = evaluate(output, var -> {
                Value val = get(var);
                if (val == null) {
                    throw new IllegalArgumentException("Unresolved variable: " + var);
                }
                return String.valueOf(val.unwrap());
            });
        }
        return output;
    }

    @Override
    public String toString() {
        return "ContextScope{"
                + "id='" + id + '\''
                + ", visibility=" + visibility
                + '}';
    }

    private ContextScope getOrCreate0(String id, Visibility visibility) {
        ContextScope scope = children.computeIfAbsent(id, sid -> new ContextScope(this, root, sid, visibility));
        if (scope.visibility != visibility) {
            if (scope.visibility == Visibility.UNSET) {
                scope.visibility = visibility;
            } else {
                throw new IllegalStateException(String.format(
                        "Scope visibility mismatch, id=%s, current=%s, requested=%s",
                        id, parent.visibility, visibility));
            }
        }
        return scope;
    }

    private ContextScope findScope(ContextPath path, BiFunction<ContextScope, String, ContextScope> fn) {
        ContextScope scope = root;
        String[] segments = path.segments();
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            if (".".equals(segment)) {
                if (i == 0) {
                    scope = this;
                }
            } else if ("..".equals(segment)) {
                scope = i == 0 ? parent : scope.parent;
            } else {
                scope = fn.apply(scope, segment);
                if (scope == null) {
                    throw new IllegalStateException("Unresolved scope: " + path.asString(i));
                }
            }
        }
        return scope;
    }

    private static ContextPath valuePath(String rawPath) {
        ContextPath path = ContextPath.create(rawPath);
        String[] segments = path.segments();
        if (segments.length == 0) {
            throw new IllegalArgumentException("Normalized path is empty");
        }
        String id = segments[segments.length - 1];
        if (id.indexOf('.') > 0) {
            throw new IllegalArgumentException("Invalid scope id: " + id);
        }
        return path;
    }
}
