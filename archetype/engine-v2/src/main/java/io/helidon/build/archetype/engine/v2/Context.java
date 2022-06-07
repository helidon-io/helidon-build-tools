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
import java.util.Map;
import java.util.NoSuchElementException;

import io.helidon.build.archetype.engine.v2.ContextScope.Visibility;
import io.helidon.build.archetype.engine.v2.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Value;

import static io.helidon.build.common.PropertyEvaluator.evaluate;

/**
 * Context.
 * Holds the state for an archetype invocation.
 *
 * <ul>
 *     <li>Maintains the current working directory for resolving files and scripts.</li>
 *     <li>Maintains the scope for storing and resolving values and variables</li>
 * </ul>
 */
public final class Context {

    private static final Path NULL_PATH = Path.of("");

    private ContextScope scope = ContextScope.create();
    private final Deque<Path> directories = new ArrayDeque<>();

    private Context(Path cwd, Map<String, String> externalValues, Map<String, String> externalDefaults) {
        if (externalDefaults != null) {
            externalDefaults.forEach((k, v) ->
                    put(scope, k, DynamicValue.create(() -> scope.interpolate(v)), ValueKind.DEFAULT));
        }
        if (externalValues != null) {
            externalValues.forEach((k, v) ->
                    put(scope, k, DynamicValue.create(evaluate(v, externalValues::get)), ValueKind.EXTERNAL));
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
     * Push a scope.
     *
     * @param scope scope
     */
    public void pushScope(ContextScope scope) {
        if (scope.parent() != this.scope) {
            throw new IllegalArgumentException(String.format(
                    "Invalid parent scope, actual=%s, expected=%s", scope.parent().id(), this.scope.id()));
        }
        this.scope = scope;
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
        if (this.scope.parent() == null) {
            throw new NoSuchElementException();
        }
        this.scope = this.scope.parent();
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

    private static void put(ContextScope scope, String path, Value value, ValueKind valueKind) {
        String[] segments = ContextPath.parse(path);
        String id = ContextPath.id(segments);
        scope.getOrCreateParent(segments, Visibility.UNSET)
             .putValue(id, value, valueKind);
    }
}
