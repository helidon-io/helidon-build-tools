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

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.util.ValueDelegate;
import io.helidon.build.common.Lists;

import java.util.List;

/**
 * Context value.
 * A context value qualifies the connection between nodes in the context tree.
 * It is a decorates an actual value with a kind that qualifies a value, see {@link ValueKind}.
 *
 * @see ContextEdge
 */
public final class ContextValue extends ValueDelegate {

    private final ContextScope scope;
    private final ValueKind kind;

    private ContextValue(ContextScope scope, Value value, ValueKind kind) {
        super(value);
        this.scope = scope;
        this.kind = kind;
    }

    /**
     * Create a new context value.
     *
     * @param scope scope
     * @param value wrapped value
     * @param kind  value kind
     */
    public static ContextValue create(ContextScope scope, Value value, ValueKind kind) {
        return new ContextValue(scope, value, kind);
    }

    /**
     * Test if this context value is read-only.
     *
     * @return {@code true} if read-only, {@code false} otherwise
     */
    public boolean isReadOnly() {
        switch (kind) {
            case EXTERNAL:
            case PRESET:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the scope.
     *
     * @return scope
     */
    public ContextScope scope() {
        return scope;
    }

    /**
     * Get the value kind.
     *
     * @return ValueKind
     */
    public ValueKind kind() {
        return kind;
    }

    @Override
    public String toString() {
        return "ContextValue{"
                + "kind=" + kind
                + ", value=" + value()
                + '}';
    }

    /**
     * Context value kind.
     */
    public enum ValueKind {
        /**
         * Preset value.
         */
        PRESET,

        /**
         * Local variable value.
         */
        LOCAL_VAR,

        /**
         * Default value.
         */
        DEFAULT,

        /**
         * External value.
         */
        EXTERNAL,

        /**
         * User value.
         */
        USER
    }
}
