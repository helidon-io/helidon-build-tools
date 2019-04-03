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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.helidon.build.sitegen.asciidoctor.AsciidocEngine;
import io.helidon.build.sitegen.freemarker.FreemarkerEngine;
import io.helidon.config.Config;

import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;

/**
 * Configuration of pair of {@link FreemarkerEngine} and {@link AsciidocEngine}
 * indexed by backend names in a static registry.
 *
 * @author rgrecour
 */
public class SiteEngine {

    private static final Map<String, SiteEngine> REGISTRY = new HashMap<>();
    private static final String FREEMARKER_PROP = "freemarker";
    private static final String ASCIIDOCTOR_PROP = "asciidoctor";
    private static final String BACKEND_PROP = "backend";
    private final AsciidocEngine asciidoc;
    private final FreemarkerEngine freemarker;

    /**
     * Create a new instance of {@link SiteEngine}.
     * @param freemarker the freemarker engine
     * @param asciidoc the asciidoc engine
     */
    public SiteEngine(FreemarkerEngine freemarker, AsciidocEngine asciidoc) {
        this.freemarker = freemarker == null
                ? FreemarkerEngine.builder().build()
                : freemarker;
        this.asciidoc = asciidoc == null
                ? AsciidocEngine.builder().build()
                : asciidoc;
    }

    /**
     * Get the asciidoc engine.
     * @return instance of {@link AsciidocEngine}, never {@code null}
     */
    public AsciidocEngine asciidoc(){
        return asciidoc;
    }

    /**
     * Get the freemarker engine.
     * @return instance of {@link FreemarkerEngine}, never {@code null}
     */
    public FreemarkerEngine freemarker(){
        return freemarker;
    }

    /**
     * Register a new {@link SiteEngine} in the registry with the given backend
     * name.
     * @param backend the backend name to use as key in the registry
     * @param engine the engine instance to register
     */
    public static void register(String backend, SiteEngine engine) {
        checkNonNullNonEmpty(backend, BACKEND_PROP);
        REGISTRY.put(backend, engine);
    }

    /**
     * Remove the {@link SiteEngine} registered for the given backend.
     * @param backend the backend to remove
     */
    public static void deregister(String backend) {
        checkNonNullNonEmpty(backend, BACKEND_PROP);
        REGISTRY.remove(backend);
    }

    /**
     * Get a {@link SiteEngine} from the registry.
     * @param backend the backend name identifying the registered {@link SiteEngine}
     * @return instance of {@link SiteEngine}, never {@code null}
     * @throws IllegalArgumentException if there is no {@link SiteEngine} registered
     * for the given backend name
     */
    public static SiteEngine get(String backend) {
        SiteEngine siteEngine = REGISTRY.get(backend);
        if (siteEngine == null) {
            throw new IllegalArgumentException(
                    "no site engine found for backend: " + backend);
        }
        return siteEngine;
    }

    /**
     * A fluent builder to create {@link SiteEngine} instances.
     */
    public static class Builder extends AbstractBuilder<SiteEngine> {

        /**
         * Set the freemarker engine to use.
         * @param freemarker the freemarker engine
         * @return the {@link Builder} instance
         */
        public Builder freemarker(FreemarkerEngine freemarker){
            put(FREEMARKER_PROP, freemarker);
            return this;
        }

        /**
         * Set the asciidoctor engine to use.
         * @param asciidoctor the asciidoctor engine
         * @return the {@link Builder} instance
         */
        public Builder asciidoctor(AsciidocEngine asciidoctor){
            put(ASCIIDOCTOR_PROP, asciidoctor);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node){
            if (node.exists()) {
                node.get(FREEMARKER_PROP).ifExists(c
                        -> put(FREEMARKER_PROP,
                                FreemarkerEngine.builder()
                                        .config(c)
                                        .build()));
                node.get(ASCIIDOCTOR_PROP).ifExists(c
                        -> put(ASCIIDOCTOR_PROP,
                                AsciidocEngine.builder()
                                        .config(c)
                                        .build()));
            }
            return this;
        }

        @Override
        public SiteEngine build() {
            FreemarkerEngine freemarker = null;
            AsciidocEngine asciidoctor = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case(FREEMARKER_PROP):
                        freemarker = asType(val, FreemarkerEngine.class);
                        break;
                    case(ASCIIDOCTOR_PROP):
                        asciidoctor = asType(val, AsciidocEngine.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new SiteEngine(freemarker, asciidoctor);
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
