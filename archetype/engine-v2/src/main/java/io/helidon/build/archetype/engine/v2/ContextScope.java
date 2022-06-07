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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import io.helidon.build.archetype.engine.v2.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.common.GenericType;

import static io.helidon.build.archetype.engine.v2.ContextPath.PARENT_REF;
import static io.helidon.build.archetype.engine.v2.ContextPath.PATH_SEPARATOR;
import static io.helidon.build.archetype.engine.v2.ContextPath.PATH_SEPARATOR_CHAR;
import static io.helidon.build.archetype.engine.v2.ContextPath.ROOT_REF;
import static io.helidon.build.common.PropertyEvaluator.evaluate;

/**
 * Context scope.
 * Tree nodes that represents scoping of {@code DeclaredInput}.
 *
 * @see Context
 * @see ContextPath
 * @see io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput
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
            if (id == null || id.isEmpty() || id.indexOf(PATH_SEPARATOR_CHAR) >= 0) {
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
     * Get or create a scope.
     *
     * @param path   scope path, must be non {@code null} and non-empty
     * @param global {@code true} if the scope is global, {@code false} otherwise
     * @return scope
     * @throws IllegalArgumentException if path is {@code null}
     */
    public ContextScope getOrCreateScope(String path, boolean global) {
        return getOrCreateScope(path, global ? Visibility.GLOBAL : Visibility.LOCAL);
    }

    /**
     * Get or create a scope.
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
    public ContextScope getOrCreateScope(String path, Visibility visibility) {
        String[] segments = ContextPath.parse(path);
        ContextScope parent = getOrCreateParent(segments, Visibility.UNSET);
        String id = ContextPath.id(segments);
        return parent.getOrCreateScope0(id, visibility);
    }

    /**
     * Get or create the parent scopes for the given segments.
     * I.e. all segments except the last one.
     * If the path contains more than one segment (delimited by {@code .}), the intermediate scopes are implicitly
     * created.
     *
     * @param segments   path segments, must be non {@code null}
     * @param visibility scope visibility
     * @return parent scope
     * @throws NullPointerException     if path is null
     * @throws IllegalArgumentException if path is invalid
     * @throws IllegalStateException    if a scope already exists and the requested visibility doesn't match
     */
    public ContextScope getOrCreateParent(String[] segments, Visibility visibility) {
        return findScope(segments, (s, sid) -> s.getOrCreateScope0(sid, visibility));
    }

    /**
     * Find a scope.
     *
     * @param segments path segments
     * @return scope, or {@code null} if not found
     * @throws NullPointerException     if path is null
     * @throws IllegalArgumentException if path is invalid
     * @see ContextPath
     */
    public ContextScope findScope(String[] segments) {
        return findScope(segments, ContextScope::findScope0);
    }

    /**
     * Set a value in the context.
     *
     * @param key   value key
     * @param value the value
     * @param kind  value kind
     * @throws IllegalStateException if a non readonly value already exists
     */
    public void putValue(String key, Value value, ValueKind kind) {
        if (key.contains(PATH_SEPARATOR)) {
            throw new IllegalArgumentException(String.format("key must not contain '%s'", PATH_SEPARATOR));
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
    public Value getValue(String path) {
        String[] segments = ContextPath.parse(path);
        String id = ContextPath.id(segments);
        ContextScope scope = findScope(segments, ContextScope::findScope0);
        return scope.findValue(id);
    }

    /**
     * Compute the visible path for this scope.
     *
     * @param internal if {@code true}, include the full hierarchy
     * @return path
     */
    public String path(boolean internal) {
        StringBuilder resolved = new StringBuilder();
        ContextScope scope = this;
        while (scope.parent != null) {
            if (resolved.length() == 0) {
                resolved.append(scope.id);
            } else {
                resolved.insert(0, scope.id + PATH_SEPARATOR);
            }
            if (!internal
                    && (scope.visibility == Visibility.GLOBAL
                    || scope.parent.visibility == Visibility.GLOBAL)) {
                break;
            }
            scope = scope.parent;
        }
        return resolved.toString();
    }

    /**
     * Compute the visible path for this scope.
     *
     * @return path
     */
    public String path() {
        return path(false);
    }

    /**
     * Compute the visible path for a given value.
     *
     * @param key value key
     * @return path
     */
    public String path(String key) {
        StringBuilder resolved = new StringBuilder(key);
        ContextScope scope = this;
        while (scope.parent != null) {
            resolved.insert(0, scope.id + PATH_SEPARATOR);
            if (scope.visibility == Visibility.GLOBAL
                    || scope.parent.visibility == Visibility.GLOBAL) {
                break;
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
                Value val = getValue(var);
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

    private ContextScope getOrCreateScope0(String id, Visibility visibility) {
        ContextScope scope = children.computeIfAbsent(id, sid -> new ContextScope(this, root, sid, visibility));
        if (scope.visibility != visibility && visibility != Visibility.UNSET) {
            if (scope.visibility == Visibility.UNSET) {
                scope.visibility = visibility;
            } else {
                throw new IllegalStateException(String.format(
                        "Scope visibility mismatch, id=%s, current=%s, requested=%s",
                        id, scope.visibility, visibility));
            }
        }
        return scope;
    }

    private ContextScope findScope(String[] segments, BiFunction<ContextScope, String, ContextScope> fn) {
        ContextScope scope = this;
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            if (i == 0 && ROOT_REF.equals(segment)) {
                scope = root;
            } else if (PARENT_REF.equals(segment)) {
                scope = i == 0 ? parent : scope.parent;
            } else {
                scope = fn.apply(scope, segment);
                if (scope == null) {
                    throw new IllegalStateException("Unresolved scope: " + ContextPath.toString(segments, i));
                }
            }
        }
        return scope;
    }

    private ContextScope findScope0(String id) {
        if (children.containsKey(id)) {
            return children.get(id);
        }
        if (parent == null || parent.visibility == Visibility.GLOBAL) {
            Deque<ContextScope> stack = new ArrayDeque<>(children.values());
            while (!stack.isEmpty()) {
                ContextScope scope = stack.pop();
                if (scope.children.containsKey(id)) {
                    return scope.children.get(id);
                }
                if (scope.visibility == Visibility.GLOBAL) {
                    stack.addAll(scope.children.values());
                }
            }
        }
        return null;
    }

    private Value findValue(String id) {
        if (values.containsKey(id)) {
            return values.get(id);
        }
        if (parent == null || parent.visibility == Visibility.GLOBAL) {
            Deque<ContextScope> stack = new ArrayDeque<>(children.values());
            while (!stack.isEmpty()) {
                ContextScope scope = stack.pop();
                if (scope.parent.visibility != Visibility.GLOBAL) {
                    continue;
                }
                if (scope.values.containsKey(id)) {
                    return scope.values.get(id);
                }
                stack.addAll(scope.children.values());
            }
        }
        return null;
    }
}
