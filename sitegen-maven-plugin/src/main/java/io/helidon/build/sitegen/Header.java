/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.helidon.config.Config;

/**
 * Configuration for default header values.
 */
public class Header implements Model {

    private static final String FAVICON_PROP = "favicon";
    private static final String STYLESHEETS_PROP = "stylesheets";
    private static final String SCRIPTS_PROP = "scripts";
    private static final String META_PROP = "meta";

    private final WebResource favicon;
    private final List<WebResource> stylesheets;
    private final List<WebResource> scripts;
    private final Map<String, String> meta;

    Header(){
        this.favicon = null;
        this.stylesheets = Collections.emptyList();
        this.scripts = Collections.emptyList();
        this.meta = Collections.emptyMap();
    }

    private Header(WebResource favicon,
                  List<WebResource> stylesheets,
                  List<WebResource> scripts,
                  Map<String, String> meta) {
        this.favicon = favicon;
        this.stylesheets = stylesheets == null ? Collections.emptyList() : stylesheets;
        this.scripts = scripts == null ? Collections.emptyList() : scripts;
        this.meta = meta == null ? Collections.emptyMap() : meta;
    }

    /**
     * Get the favicon.
     * @return {@link WebResource} instance, or {@code null} if not set
     */
    public WebResource getFavicon() {
        return favicon;
    }

    /**
     * Get the stylesheets.
     * @return {@code List<WebResource>}, never {@code null}
     */
    public List<WebResource> getStylesheets() {
        return stylesheets;
    }

    /**
     * Get the scripts.
     *
     * @return {@code List<WebResource>}, never {@code null}
     */
    public List<WebResource> getScripts() {
        return scripts;
    }

    /**
     * Get the meta.
     * @return {@code Map<String, String>}, never {@code null}
     */
    public Map<String, String> getMeta() {
        return meta;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case (FAVICON_PROP):
                return favicon;
            case (STYLESHEETS_PROP):
                return stylesheets;
            case (SCRIPTS_PROP):
                return scripts;
            case (META_PROP):
                return meta;
            default:
                throw new IllegalArgumentException(
                        "Unkown attribute: " + attr);
        }
    }

    /**
     * A fluent builder to create {@link Header} instances.
     */
    public static class Builder extends AbstractBuilder<Header> {

        /**
         * Set the favicon.
         * @param favicon the favicon to use
         * @return the {@link Builder} instance
         */
        public Builder favicon(WebResource favicon){
            put(FAVICON_PROP, favicon);
            return this;
        }

        /**
         * Set the stylesheets.
         * @param stylesheets the stylesheets to use
         * @return the {@link Builder} instance
         */
        public Builder stylesheets(List<WebResource> stylesheets){
            put(STYLESHEETS_PROP, stylesheets);
            return this;
        }

        /**
         * Set the scripts.
         * @param scripts the scripts to use
         * @return the {@link Builder} instance
         */
        public Builder scripts(List<WebResource> scripts){
            put(SCRIPTS_PROP, scripts);
            return this;
        }

        /**
         * Set the meta.
         * @param meta the meta to use
         * @return the {@link Builder} instance
         */
        public Builder meta(Map<String, String> meta){
            put(META_PROP, meta);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node){
            // favicon
            node.get(FAVICON_PROP).ifExists(c -> {
                put(FAVICON_PROP, WebResource.builder().config(c).build());
            });

            // stylesheets
            node.get(STYLESHEETS_PROP)
                    .asNodeList()
                    .ifPresent(nodeList -> {
                        put(STYLESHEETS_PROP, nodeList
                                .stream()
                                .map(c -> WebResource.builder().config(c).build())
                                .collect(Collectors.toList()));
                    });

            // scripts
            node.get(SCRIPTS_PROP)
                    .asNodeList()
                    .ifPresent(nodeList -> {
                        put(SCRIPTS_PROP, nodeList
                                .stream()
                                .map(c -> WebResource.builder().config(c).build())
                                .collect(Collectors.toList()));
                    });

            // meta
            node.get(META_PROP)
                    .detach()
                    .asMap()
                    .ifPresent(m -> put(META_PROP, m));

            return this;
        }

        @Override
        public Header build() {
            WebResource favicon = null;
            List<WebResource> stylesheets = null;
            List<WebResource> scripts = null;
            Map<String, String> meta = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case(FAVICON_PROP):
                        favicon = asType(val, WebResource.class);
                        break;
                    case(STYLESHEETS_PROP):
                        stylesheets = asList(val, WebResource.class);
                        break;
                    case(SCRIPTS_PROP):
                        scripts = asList(val, WebResource.class);
                        break;
                    case(META_PROP):
                        meta = asMap(val, String.class, String.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new Header(favicon, stylesheets, scripts, meta);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    public static Builder builder(){
        return new Builder();
    }
}
