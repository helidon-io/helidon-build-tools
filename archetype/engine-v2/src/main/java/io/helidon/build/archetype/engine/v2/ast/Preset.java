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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Preset.
 */
public final class Preset extends Node {

    private final Kind kind;
    private final Value value;
    private final String path;

    private Preset(Builder builder) {
        super(builder);
        this.kind = Objects.requireNonNull(builder.kind, "kind is null");
        this.path = builder.attribute("path");
        switch (kind) {
            case BOOLEAN:
                value = Value.create(Boolean.parseBoolean(builder.value));
                break;
            case TEXT:
            case ENUM:
                value = Value.create(builder.value);
                break;
            case LIST:
                value = Value.create(Collections.unmodifiableList(builder.values));
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
    public <A> VisitResult accept(Node.Visitor<A> visitor, A arg) {
        return visitor.visitPreset(this, arg);
    }

    /**
     * Preset kind.
     */
    public enum Kind {

        /**
         * Text.
         */
        TEXT,

        /**
         * Boolean.
         */
        BOOLEAN,

        /**
         * Enum.
         */
        ENUM,

        /**
         * List.
         */
        LIST
    }

    /**
     * Get the preset kind.
     *
     * @return kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Create a new builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @param kind       kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Kind kind) {
        return new Builder(scriptPath, position, kind);
    }

    /**
     * Preset builder.
     */
    public static final class Builder extends Node.Builder<Preset, Builder> {

        private final List<String> values = new LinkedList<>();
        private final Kind kind;
        private String value;

        private Builder(Path scriptPath, Position position, Kind kind) {
            super(scriptPath, position);
            this.kind = kind;
        }

        /**
         * Set the value.
         *
         * @param value value
         * @return this builder
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        private boolean doRemove(Noop.Builder b) {
            if (b.kind() == Noop.Kind.VALUE) {
                values.add(b.value());
            }
            return true;
        }

        @Override
        protected Preset doBuild() {
            remove(children(), Noop.Builder.class, this::doRemove);
            return new Preset(this);
        }
    }
}
