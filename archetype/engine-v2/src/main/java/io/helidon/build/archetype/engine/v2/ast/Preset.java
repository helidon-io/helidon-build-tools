/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Preset.
 */
public final class Preset extends Block {

    private final Value value;
    private final String path;

    private Preset(Builder builder) {
        super(builder);
        this.path = builder.attribute("path", true);
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
                List<Node.Builder<? extends Node, ?>> children = builder.children();
                value = Value.create(children.stream().map(Node.Builder::value).collect(toUnmodifiableList()));
                children.clear();
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

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitPreset(this, arg);
    }

    /**
     * Create a new builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @param kind       kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Block.Kind kind) {
        return new Builder(scriptPath, position, kind);
    }

    /**
     * Preset builder.
     */
    public static final class Builder extends Block.Builder {

        private Builder(Path scriptPath, Position position, Block.Kind kind) {
            super(scriptPath, position, kind);
        }

        @Override
        protected Preset doBuild() {
            return new Preset(this);
        }
    }
}
