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
package io.helidon.build.archetype.engine.v2.context;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.NoSuchElementException;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;

import static io.helidon.build.common.PropertyEvaluator.evaluate;
import static java.util.Objects.requireNonNull;

/**
 * Archetype context.
 * Holds the state for an archetype invocation.
 *
 * <ul>
 *     <li>Maintains the current working directory for resolving files and scripts.</li>
 *     <li>Maintains the scope for storing and resolving values and variables</li>
 * </ul>
 */
public final class Context implements ContextRegistry {

    private ContextScope scope;
    private final Deque<Path> directories = new ArrayDeque<>();

    private Context(Builder builder) {
        this.scope = builder.scope;
        requireRootScope();
        builder.externalDefaults.forEach((k, v) -> scope.putValue(k,
                DynamicValue.create(() -> scope.interpolate(v)), ValueKind.DEFAULT));
        builder.externalValues.forEach((k, v) -> scope.putValue(k,
                DynamicValue.create(evaluate(v, builder.externalValues::get)), ValueKind.EXTERNAL));
        directories.push(builder.cwd);
    }

    /**
     * Push a new working directory.
     *
     * @param workDir directory
     */
    public void pushCwd(Path workDir) {
        directories.push(workDir.toAbsolutePath());
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
     * Push a scope.
     *
     * @param id     scope id
     * @param global {@code true} if the scope should be global, {@code false} if local.
     * @return the new current scope
     */
    public ContextScope pushScope(String id, boolean global) {
        return pushScope(scope.getOrCreate(id, global));
    }

    /**
     * Push a scope.
     *
     * @param scope scope
     * @return the new current scope
     */
    public ContextScope pushScope(ContextScope scope) {
        if (!scope.isChildOf(this.scope)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid scope: current=%s, given=%s", this.scope.path(true), scope.path(true)));
        }
        this.scope = scope;
        return scope;
    }

    /**
     * Get the current scope.
     *
     * @return Scope
     */
    public ContextScope scope() {
        return this.scope;
    }

    /**
     * Pop the current scope.
     *
     * @throws NoSuchElementException if the current scope is the root scope
     */
    public void popScope() {
        if (scope.parent() == null) {
            throw new NoSuchElementException();
        }
        scope = scope.parent0();
    }

    /**
     * Require the current scope to be a root scope.
     *
     * @throws IllegalStateException if the current scope is {@code null} or not the root scope.
     */
    public void requireRootScope() {
        if (this.scope == null || this.scope.parent() != null) {
            throw new IllegalStateException("Invalid scope");
        }
    }

    @Override
    public ContextValue putValue(String path, Value value, ValueKind kind) {
        return scope.putValue(path, value, kind);
    }

    @Override
    public ContextValue getValue(String path) {
        return scope.getValue(path);
    }

    @Override
    public String interpolate(String value) {
        return scope.interpolate(value);
    }

    /**
     * Create a new context.
     *
     * @return context
     */
    public static Context create() {
        return new Builder().build();
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Context builder.
     */
    public static final class Builder {

        private Map<String, String> externalValues = Map.of();
        private Map<String, String> externalDefaults = Map.of();
        private Path cwd = Path.of("");
        private ContextScope scope = ContextNode.create();

        private Builder() {
        }

        /**
         * Set the initial working directory.
         *
         * @param cwd working directory
         * @return this builder
         */
        public Builder cwd(Path cwd) {
            this.cwd = requireNonNull(cwd, "cwd is null");
            return this;
        }

        /**
         * Set the external values.
         *
         * @param externalValues external values
         * @return this builder
         */
        public Builder externalValues(Map<String, String> externalValues) {
            this.externalValues = requireNonNull(externalValues, "externalValues is null");
            return this;
        }

        /**
         * Set the external defaults.
         *
         * @param externalDefaults external defaults
         * @return this builder
         */
        public Builder externalDefaults(Map<String, String> externalDefaults) {
            this.externalDefaults = requireNonNull(externalDefaults, "externalDefaults is null");
            return this;
        }

        /**
         * Set the initial scope.
         *
         * @param scope initial scope
         * @return this builder
         */
        public Builder scope(ContextScope scope) {
            this.scope = requireNonNull(scope, "scope is null");
            return this;
        }

        /**
         * Build the context instance.
         *
         * @return new context
         */
        public Context build() {
            return new Context(this);
        }
    }
}
