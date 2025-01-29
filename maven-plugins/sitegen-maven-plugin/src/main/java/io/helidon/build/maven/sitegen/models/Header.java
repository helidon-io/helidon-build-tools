/*
 * Copyright (c) 2018, 2025 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.Model;

/**
 * Configuration for default header values.
 */
public class Header implements Model {

    private final WebResource favicon;
    private final List<WebResource> stylesheets;
    private final List<WebResource> scripts;
    private final Map<String, String> meta;

    private Header(Builder builder) {
        this.favicon = builder.favicon;
        this.stylesheets = builder.stylesheets;
        this.scripts = builder.scripts;
        this.meta = builder.meta;
    }

    /**
     * Get the favicon.
     *
     * @return favicon, or {@code null}
     */
    public WebResource favicon() {
        return favicon;
    }

    /**
     * Get the stylesheets.
     *
     * @return list, never {@code null}
     */
    public List<WebResource> stylesheets() {
        return stylesheets;
    }

    /**
     * Get the scripts.
     *
     * @return list never {@code null}
     */
    public List<WebResource> scripts() {
        return scripts;
    }

    /**
     * Get the meta.
     *
     * @return map, never {@code null}
     */
    public Map<String, String> meta() {
        return meta;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case "favicon":
                return favicon;
            case "stylesheets":
                return stylesheets;
            case "scripts":
                return scripts;
            case "meta":
                return meta;
            default:
                throw new IllegalArgumentException("Unknown attribute: " + attr);
        }
    }

    @Override
    public String toString() {
        return "Header{"
                + "favicon=" + favicon
                + ", stylesheets=" + stylesheets
                + ", scripts=" + scripts
                + ", meta=" + meta
                + '}';
    }

    /**
     * A builder of {@link Header}.
     */
    @SuppressWarnings("unused")
    public static class Builder implements Supplier<Header> {

        private WebResource favicon;
        private final List<WebResource> stylesheets = new ArrayList<>();
        private final List<WebResource> scripts = new ArrayList<>();
        private final Map<String, String> meta = new HashMap<>();

        /**
         * Set the favicon.
         *
         * @param favicon the favicon to use
         * @return this builder
         */
        public Builder favicon(WebResource favicon) {
            this.favicon = favicon;
            return this;
        }

        /**
         * Add a stylesheet.
         *
         * @param stylesheet stylesheet
         * @return this builder
         */
        public Builder stylesheet(WebResource stylesheet) {
            if (stylesheet != null) {
                this.stylesheets.add(stylesheet);
            }
            return this;
        }

        /**
         * Add a stylesheet.
         *
         * @param stylesheet stylesheet supplier
         * @return this builder
         */
        public Builder stylesheet(Supplier<WebResource> stylesheet) {
            if (stylesheet != null) {
                this.stylesheets.add(stylesheet.get());
            }
            return this;
        }

        /**
         * Add the scripts.
         *
         * @param script script
         * @return this builder
         */
        public Builder scripts(WebResource script) {
            if (script != null) {
                this.scripts.add(script);
            }
            return this;
        }


        /**
         * Add the scripts.
         *
         * @param script script supplier
         * @return this builder
         */
        public Builder scripts(Supplier<WebResource> script) {
            if (script != null) {
                this.scripts.add(script.get());
            }
            return this;
        }

        /**
         * Add meta.
         *
         * @param meta meta
         * @return this builder
         */
        public Builder meta(Map<String, String> meta) {
            if (meta != null) {
                this.meta.putAll(meta);
            }
            return this;
        }

        /**
         * Apply the specified configuration.
         *
         * @param config config
         * @return this builder
         */
        public Builder config(Config config) {
            favicon = config.get("favicon")
                            .asOptional()
                            .map(WebResource::create)
                            .orElse(null);

            config.get("stylesheets")
                  .asNodeList()
                  .orElseGet(List::of)
                  .stream()
                  .map(WebResource::create)
                  .forEach(stylesheets::add);

            config.get("scripts")
                  .asNodeList()
                  .orElseGet(List::of)
                  .stream()
                  .map(WebResource::create)
                  .forEach(scripts::add);

            meta.putAll(config.get("meta").asMap().orElseGet(Map::of));
            return this;
        }

        @Override
        public Header get() {
            return build();
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public Header build() {
            return new Header(this);
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param config config
     * @return new instance
     */
    public static Header create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Create a new instance.
     *
     * @return new instance
     */
    public static Header create() {
        return builder().build();
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
