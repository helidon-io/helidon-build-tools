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

package io.helidon.build.archetype.engine.v2.ast;

import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Declared value.
 */
public abstract class DeclaredValue extends Block {

    private final Value value;
    private final String path;

    protected DeclaredValue(Builder builder, List<String> values) {
        super(builder);
        this.path = builder.attribute("path", true).asString();
        Kind kind = builder.kind();
        switch (kind) {
            case BOOLEAN:
                value = Value.create(Boolean.parseBoolean(builder.value()));
                break;
            case TEXT:
            case ENUM:
                value = Value.create(builder.value());
                break;
            case LIST:
                value = Value.create(values);
                break;
            default:
                throw new IllegalArgumentException("Unknown preset kind: " + kind);
        }
    }

    /**
     * Get the input path.
     *
     * @return path
     */
    public String path() {
        return path;
    }

    /**
     * Get the value.
     *
     * @return value
     */
    public Value value() {
        return value;
    }

    /**
     * Declared value builder.
     */
    public static abstract class Builder extends Block.Builder {

        /**
         * Create a new builder.
         *
         * @param info builder info
         * @param kind kind
         */
        protected Builder(BuilderInfo info, Kind kind) {
            super(info, kind);
        }

        /**
         * Create the declared value instance.
         *
         * @param builder builder
         * @param values  values
         * @return DeclaredValue
         */
        protected abstract DeclaredValue doBuild(Builder builder, List<String> values);

        @Override
        protected DeclaredValue doBuild() {
            if (kind() == Kind.LIST) {
                // collapse the nested values and clear the nested builders
                List<Node.Builder<? extends Node, ?>> nestedBuilders = nestedBuilders();
                List<String> values = nestedBuilders.stream().map(Node.Builder::value).collect(toUnmodifiableList());
                nestedBuilders.clear();
                return doBuild(this, values);
            }
            return doBuild(this, null);
        }
    }
}
