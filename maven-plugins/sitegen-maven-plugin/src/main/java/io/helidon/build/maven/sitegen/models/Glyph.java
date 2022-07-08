/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.models;

import java.util.function.Supplier;

import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.Model;

import static io.helidon.build.common.Strings.requireValid;

/**
 * Configuration for image or icon.
 */
public class Glyph implements Model {

    private final String type;
    private final String value;

    private Glyph(Builder builder) {
        this.type = requireValid(builder.type, "type is invalid!");
        this.value = requireValid(builder.value, "glyph is invalid!");
    }

    /**
     * Get the type.
     *
     * @return the type value, never {@code null}
     */
    public String type() {
        return type;
    }

    /**
     * Get the value.
     *
     * @return the value, never {@code null}
     */
    public String value() {
        return value;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case "type":
                return type;
            case "value":
                return value;
            default:
                throw new IllegalArgumentException("Unknown attribute: " + attr);
        }
    }

    @Override
    public String toString() {
        return "Glyph{"
                + "type='" + type + '\''
                + ", value='" + value + '\''
                + '}';
    }

    /**
     * A builder of {@link Glyph}.
     */
    public static class Builder implements Supplier<Glyph> {

        private String type;
        private String value;

        /**
         * Set the type.
         *
         * @param type the type to use
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Set the value.
         *
         * @param value the value to use
         * @return this builder
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Apply the configuration.
         *
         * @param config config
         * @return this builder
         */
        public Builder config(Config config) {
            type = config.get("type").asString().orElse(null);
            value = config.get("value").asString().orElse(null);
            return this;
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public Glyph build() {
            return new Glyph(this);
        }

        @Override
        public Glyph get() {
            return build();
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param config config
     * @return new instance
     */
    public static Glyph create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Create a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
