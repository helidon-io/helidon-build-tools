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

import java.util.Map;
import java.util.Map.Entry;

import io.helidon.config.Config;

import static io.helidon.build.sitegen.Helper.checkNonNull;
import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;

/**
 * Backend base class.
 */
public abstract class Backend implements Model {

    private static final String NAME_PROP = "name";
    private static final String CONFIG_NODE_PROP = "confignode";
    private final String name;

    /**
     * Create a new backend instance.
     * @param name the name of the backend
     */
    protected Backend(String name){
        checkNonNullNonEmpty(name, NAME_PROP);
        this.name = name;
    }

    /**
     * Get the backend name.
     * @return the backend name
     */
    public String getName() {
        return name;
    }

    /**
     * Generate the site files.
     * @param ctx the context of this rendering invocation
     */
    public abstract void generate(RenderingContext ctx);

    /**
     * Get this backend {@link PageRenderer}s.
     * @return {@code Map} of {@link PageRenderer} keyed by file extension,
     * never {@code null}
     */
    public abstract Map<String, PageRenderer> pageRenderers();

    /**
     * Get a {@link PageRenderer} for the given file extension.
     * @param ext the file extension
     * @throws IllegalArgumentException if no renderer is found for the extension
     * @return the {@link PageRenderer} associated with the extension
     */
    public PageRenderer getPageRenderer(String ext){
        Map<String, PageRenderer> pageRenderers = pageRenderers();
        checkNonNull(pageRenderers, "pageRenderers");
        PageRenderer renderer = pageRenderers.get(ext);
        if (renderer == null) {
            throw new IllegalArgumentException(
                        "no renderer found for extension: " + ext);
        }
        return renderer;
    }

    @Override
    public Object get(String attr) {
        throw new IllegalArgumentException("Unkown attribute: " + attr);
    }

    /**
     * A base class for {@link Backend} implementation builder to extends from.
     * @param <T> the {@link Backend} implementation type
     */
    public static class Builder<T extends Backend> extends AbstractBuilder<T> {

        /**
         * Set the backend name.
         * @param name backend name
         * @return the {@link Builder} instance
         */
        public Builder name(String name){
            put(NAME_PROP, name);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node) {
            if (node.exists()) {
                node.get(NAME_PROP).ifExists(c
                        -> put(NAME_PROP, c.asString()));
                put(CONFIG_NODE_PROP, node);
            }
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T build() {
            String name = null;
            Config node = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case(NAME_PROP):
                        name = asType(val, String.class);
                        break;
                    case(CONFIG_NODE_PROP):
                        node = asType(val, Config.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return (T) BackendProvider.get(name, node);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @param <T> the {@link Backend} subtype
     * @return the created builder
     */
    public static <T extends Backend> Builder<T> builder(){
        return new Builder<>();
    }
}
