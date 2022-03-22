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

package io.helidon.build.archetype.engine.v2.ast;

import java.nio.file.Path;
import java.util.List;

import io.helidon.build.archetype.engine.v2.ScriptLoader;

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Preset.
 */
public final class Preset extends Block {

    private final Value value;
    private final String path;
    private final boolean resolvable;
    private final boolean isTransient;

    private Preset(Builder builder, List<String> values) {
        super(builder);
        this.path = builder.attribute("path", true).asString();
        this.resolvable = !"false".equalsIgnoreCase(builder.attribute("resolvable", false).asString());
        this.isTransient = builder.attribute("transient", false).asBoolean();
        Block.Kind kind = builder.kind();
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
     * Test if this preset can be resolved.
     *
     * @return {@code true} if it can be resolved, {@code false} otherwise
     */
    public boolean isResolvable() {
        return resolvable;
    }

    /**
     * Test if this preset is transient.
     *
     * @return {@code true} if transient, {@code false} otherwise
     */
    public boolean isTransient() {
        return isTransient;
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

    @Override
    public String toString() {
        return "Preset{"
                + "path='" + path + '\''
                + ", optional=" + resolvable + '\''
                + ", value=" + value + '\''
                + '}';
    }

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitPreset(this, arg);
    }

    /**
     * Create a new builder.
     *
     * @param loader     script loader
     * @param scriptPath script path
     * @param position   position
     * @param kind       kind
     * @return builder
     */
    public static Builder builder(ScriptLoader loader, Path scriptPath, Position position, Block.Kind kind) {
        return new Builder(loader, scriptPath, position, kind);
    }

    /**
     * Preset builder.
     */
    public static final class Builder extends Block.Builder {

        private Builder(ScriptLoader loader, Path scriptPath, Position position, Block.Kind kind) {
            super(loader, scriptPath, position, kind);
        }

        @Override
        protected Preset doBuild() {
            if (kind() == Kind.LIST) {
                // collapse the nested values and clear the nested builders
                List<Node.Builder<? extends Node, ?>> nestedBuilders = nestedBuilders();
                List<String> values = nestedBuilders.stream().map(Node.Builder::value).collect(toUnmodifiableList());
                nestedBuilders.clear();
                return new Preset(this, values);
            }
            return new Preset(this, null);
        }
    }
}
