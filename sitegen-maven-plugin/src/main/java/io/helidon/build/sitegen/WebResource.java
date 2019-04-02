/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

import io.helidon.config.Config;

import static io.helidon.build.sitegen.Helper.checkNonNull;
import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;

/**
 * Configuration for different type of web resources such as css stylesheets or scripts.
 *
 * @author rgrecour
 */
public class WebResource implements Model {

    private static final String LOCATION_PROP = "location";
    private static final String TYPE_PROP = "type";
    private static final String PATH_PROP = "path";
    private static final String HREF_PROP = "href";
    private final Location location;
    private final String type;

    private WebResource(Location location, String type) {
        checkNonNull(location, LOCATION_PROP);
        this.location = location;
        this.type = type;
    }

    Location getLocation() {
        return location;
    }

    String getType() {
        return type;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case (LOCATION_PROP):
                return location.getValue();
            case (TYPE_PROP):
                return type;
            default:
                throw new IllegalArgumentException(
                        "Unkown attribute: " + attr);
        }
    }

    /**
     * A fluent builder to create {@link WebResource} instances.
     */
    public static class Builder extends AbstractBuilder<WebResource> {

        /**
         * Set the remote location.
         * @param href the remote location to use
         * @return the {@link Builder} instance
         */
        public Builder href(String href){
            put(HREF_PROP, href);
            return this;
        }

        /**
         * Set the local path.
         * @param path the local path to use
         * @return the {@link Builder} instance
         */
        public Builder path(String path) {
            put(PATH_PROP, path);
            return this;
        }

        /**
         * Set the type of resource.
         * @param type the type to use
         * @return the {@link Builder} instance
         */
        public Builder type(String type){
            put(TYPE_PROP, type);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node){
            if (node.exists()) {
                node.get(PATH_PROP).ifExists(c -> put(PATH_PROP, c.asString()));
                node.get(HREF_PROP).ifExists(c -> put(HREF_PROP, c.asString()));
                node.get(TYPE_PROP).ifExists(c -> put(TYPE_PROP, c.asString()));
            }
            return this;
        }

        @Override
        public WebResource build() {
            String path = null;
            String href = null;
            String type = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case(PATH_PROP):
                        path = asType(val, String.class);
                        break;
                    case(HREF_PROP):
                        href = asType(val, String.class);
                        break;
                    case(TYPE_PROP):
                        type = asType(val, String.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new WebResource(Location.from(path, href), type);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    public static Builder builder(){
        return new Builder();
    }

    static class Location {

        enum Type {
            PATH,
            HREF
        }

        private static final String VALUE_PROP = "value";
        private static final String TYPE_PROP = "type";
        private final String value;
        private final Type type;

        private Location(Type type, String value){
            checkNonNull(type, TYPE_PROP);
            checkNonNullNonEmpty(value, VALUE_PROP);
            this.type = type;
            this.value = value;
        }

        public Type getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Location other = (Location) obj;
            if (!Objects.equals(this.value, other.value)) {
                return false;
            }
            return this.type == other.type;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.value);
            hash = 53 * hash + Objects.hashCode(this.type);
            return hash;
        }

        static Location from(String path, String href){
            Location.Type locationType;
            String locationValue;
            if (path != null && !path.isEmpty() && href == null) {
                locationType = Location.Type.PATH;
                locationValue = path;
            } else if (href != null & !href.isEmpty() && path == null) {
                locationType = Location.Type.HREF;
                locationValue = href;
            } else {
                throw new IllegalArgumentException("Invalid location");
            }
            return new Location(locationType, locationValue);
        }
    }
}
