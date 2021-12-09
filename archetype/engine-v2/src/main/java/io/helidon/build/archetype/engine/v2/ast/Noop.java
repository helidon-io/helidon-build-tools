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
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * No-op node builder used at "builder-time" only.
 * <p>
 * This is a hack that helps in making the XML reader strictly follow the document hierarchy. It is used to map
 * the values of some nested XML elements as fields rather than child nodes.
 */
public final class Noop {

    private Noop() {
    }

    /**
     * Noop kind.
     */
    public enum Kind {

        /**
         * Replace.
         */
        REPLACE,

        /**
         * Directory.
         */
        DIRECTORY,

        /**
         * Include.
         */
        INCLUDE,

        /**
         * Exclude.
         */
        EXCLUDE,

        /**
         * Help.
         */
        HELP,

        /**
         * Value.
         */
        VALUE;

        /**
         * Noop kind names.
         */
        public static final List<String> NAMES = Arrays.stream(Kind.values())
                                                       .map(Kind::name)
                                                       .map(String::toLowerCase)
                                                       .collect(toUnmodifiableList());
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
     * No-op builder.
     */
    public static final class Builder extends Node.Builder<Node, Builder> {

        private String value;
        private final Kind kind;

        private Builder(Path scriptPath, Position position, Kind kind) {
            super(scriptPath, position);
            this.kind = kind;
        }

        /**
         * Get the kind.
         * @return kind
         */
        Kind kind() {
            return kind;
        }

        /**
         * Get the value.
         *
         * @return value
         */
        String value() {
            return value;
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

        @Override
        protected Node doBuild() {
            return null;
        }
    }
}
