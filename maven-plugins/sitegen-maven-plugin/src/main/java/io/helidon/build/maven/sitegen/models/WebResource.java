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
import static java.util.Objects.requireNonNull;

/**
 * Configuration for different type of web resources such as css stylesheets or scripts.
 */
public final class WebResource implements Model {

    private final Location location;
    private final String type;

    private WebResource(Builder builder) {
        this.location = requireNonNull(builder.location, "location is null!");
        this.type = builder.type;
    }

    /**
     * Get the location.
     *
     * @return location
     */
    public Location location() {
        return location;
    }

    /**
     * Get the type.
     *
     * @return type
     */
    public String type() {
        return type;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case ("location"):
                return location.value;
            case ("type"):
                return type;
            default:
                throw new IllegalArgumentException("Unknown attribute: " + attr);
        }
    }

    @Override
    public String toString() {
        return "WebResource{"
                + "location=" + location
                + ", type='" + type + '\''
                + '}';
    }

    /**
     * A builder of {@link WebResource}.
     */
    public static class Builder implements Supplier<WebResource> {

        private Location location;
        private String type;

        /**
         * Set the location.
         *
         * @param location location
         * @return this builder
         */
        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        /**
         * Set the location.
         *
         * @param type  location type
         * @param value location value
         * @return this builder
         */
        public Builder location(Location.Type type, String value) {
            this.location = Location.create(type, value);
            return this;
        }

        /**
         * Set the type of resource.
         *
         * @param type the type to use
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Apply the specified configuration.
         *
         * @param config config
         * @return this builder
         */
        public Builder config(Config config) {
            type = config.get("type").asString().orElse(null);
            location = Location.create(config);
            return this;
        }

        @Override
        public WebResource get() {
            return build();
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public WebResource build() {
            return new WebResource(this);
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param config config
     * @return new instance
     */
    public static WebResource create(Config config) {
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

    /**
     * Location.
     */
    public static final class Location implements Model {

        /**
         * Location type.
         */
        public enum Type {
            /**
             * Path.
             */
            PATH,

            /**
             * Href.
             */
            HREF
        }

        private final String value;
        private final Type type;

        private Location(Type type, String value) {
            this.type = requireNonNull(type, "type is null!");
            this.value = requireValid(value, "value is invalid!");
        }

        /**
         * Get the location type.
         *
         * @return type
         */
        public Type type() {
            return type;
        }

        /**
         * Get the location value.
         *
         * @return value
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

        /**
         * Create a new instance.
         *
         * @param type  type
         * @param value value
         * @return new instance
         */
        public static Location create(Type type, String value) {
            return new Location(type, value);
        }

        /**
         * Create a new instance from configuration.
         *
         * @param config config
         * @return new instance
         */
        public static Location create(Config config) {
            return create(
                    config.get("href").asString().orElse(null),
                    config.get("path").asString().orElse(null));
        }

        /**
         * Create a new instance.
         *
         * @param href href, may be {@code null}
         * @param path path, may be {@code null}
         * @return new instance
         * @throws IllegalArgumentException if both href and path are {@code null}
         */
        public static Location create(String href, String path) {
            if (href != null) {
                return new Location(Type.HREF, href);
            }
            if (path != null) {
                return new Location(Type.PATH, path);
            }
            throw new IllegalArgumentException("Invalid location");
        }
    }
}
