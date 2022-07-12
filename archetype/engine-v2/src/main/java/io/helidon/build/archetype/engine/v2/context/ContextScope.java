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
package io.helidon.build.archetype.engine.v2.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Context tree node.
 *
 * @see Context
 * @see ContextPath
 */
public interface ContextScope extends ContextRegistry {

    /**
     * Scope visibility.
     */
    enum Visibility {

        /**
         * Global visibility.
         * <br/>
         * <br/>
         * A scope can be global only if its parent is also global.
         * Global parent scopes are optional path segments.
         * <br/>
         * <br/>
         * E.g. Consider {@code a}, {@code b} as global and {@code c} as local
         * <ul>
         *     <li>{@code "a.b.c", "b.c", "c"} are equivalent paths</li>
         *     <li>{@code "a.c", "a.b.c"} are equivalent paths only if {@code "a.b.c"} is created first</li>
         * </ul>
         *
         * @see ContextPath
         */
        GLOBAL,

        /**
         * Local visibility.
         * <br/>
         * <br/>
         * Local parent scopes are mandatory path segments.
         * <br/>
         * <br/>
         * E.g.
         * <ul>
         *     <li>{@code "a.b" == "b.c" == "c"} if {@code a,b} are global and {@code c} is local</li>
         *     <li>{@code "a.b.c" != "b.c" != "c"} if all nodes are local</li>
         * </ul>
         *
         * @see ContextPath
         */
        LOCAL,

        /**
         * Unset visibility.
         * Behaves like {@link #LOCAL}, but can be changed later on to either {@link #GLOBAL} or {@link #LOCAL}.
         */
        UNSET
    }

    /**
     * Get the parent scope.
     *
     * @return parent, or {@code null} if this scope is a root scope.
     */
    ContextScope parent();

    /**
     * Get the true parent scope.
     *
     * @return parent, or {@code null} if this scope is a root scope.
     */
    default ContextScope parent0() {
        return parent();
    }

    /**
     * Test if this scope is a child of the given scope.
     *
     * @return {@code true} if a child, {@code false} otherwise
     */
    default boolean isChildOf(ContextScope scope) {
        return parent() == scope;
    }

    /**
     * Get the visibility.
     *
     * @return visibility
     */
    Visibility visibility();

    /**
     * Compute the context path for this scope.
     *
     * @param internal if {@code true}, include all global parents
     * @return path
     */
    String path(boolean internal);

    /**
     * Compute the context path for this scope.
     *
     * @return path
     */
    default String path() {
        return path(false);
    }

    /**
     * Get or create a scope.
     *
     * @param path   context path, see {@link ContextPath}
     * @param global {@code true} if the scope should be global, {@code false} if local.
     * @return scope
     */
    default ContextScope getOrCreate(String path, boolean global) {
        return getOrCreate(path, global ? Visibility.GLOBAL : Visibility.LOCAL);
    }

    /**
     * Get or create a scope.
     *
     * @param path       context path, see {@link ContextPath}
     * @param visibility visibility
     * @return scope
     */
    ContextScope getOrCreate(String path, Visibility visibility);

    /**
     * Visit the edges.
     *
     * @param visitor visitor
     */
    default void visitEdges(ContextEdge.Visitor visitor) {
        visitEdges(visitor, true);
    }

    /**
     * Get all values keyed by path.
     *
     * @return map of values keyed by path
     */
    default Map<String, ContextValue> values() {
        Map<String, ContextValue> values = new HashMap<>();
        visitEdges(edge -> {
            ContextValue value = edge.value();
            if (value != null) {
                values.put(edge.node().path(), value);
            }
        });
        return values;
    }

    /**
     * Visit the edges.
     *
     * @param visitor         visitor
     * @param visitVariations {@code true} if variations should be visited
     */
    void visitEdges(ContextEdge.Visitor visitor, boolean visitVariations);
}
