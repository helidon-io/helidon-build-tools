/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen;

import java.util.Map.Entry;

import io.helidon.config.Config;

import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;

/**
 * Configuration for image or icon.
 */
public class Glyph implements Model {

    private static final String TYPE_PROP = "type";
    private static final String VALUE_PROP = "value";
    private final String type;
    private final String value;

    private Glyph(String type, String value) {
        checkNonNullNonEmpty(type, TYPE_PROP);
        checkNonNullNonEmpty(value, VALUE_PROP);
        this.type = type;
        this.value = value;
    }

    /**
     * Get the type.
     * @return the type value, never {@code null}
     */
    public String getType() {
        return type;
    }

    /**
     * Get the value.
     * @return the value, never {@code null}
     */
    public String getValue() {
        return value;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case (TYPE_PROP):
                return type;
            case (VALUE_PROP):
                return value;
            default:
                throw new IllegalArgumentException(
                        "Unkown attribute: " + attr);
        }
    }

    /**
     * A fluent builder to create {@link Glyph} instances.
     */
    public static class Builder extends AbstractBuilder<Glyph> {

        /**
         * Set the type.
         * @param type the type to use
         * @return the {@link Builder} instance
         */
        public Builder type(String type){
            put(TYPE_PROP, type);
            return this;
        }

        /**
         * Set the value.
         * @param value the value to use
         * @return the {@link Builder} instance
         */
        public Builder value(String value){
            put(VALUE_PROP, value);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node){
            if (node.exists()) {
                node.get(TYPE_PROP).ifExists(c ->
                  put(TYPE_PROP, c.asString()));
                node.get(VALUE_PROP).ifExists(c
                        -> put(VALUE_PROP, c.asString()));
            }
            return this;
        }

        @Override
        public Glyph build() {
            String type = null;
            String value = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case(TYPE_PROP):
                        type = asType(val, String.class);
                        break;
                    case(VALUE_PROP):
                        value = asType(val, String.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new Glyph(type, value);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
