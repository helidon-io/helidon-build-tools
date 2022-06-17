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

package io.helidon.build.maven.sitegen;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.helidon.build.maven.sitegen.asciidoctor.AsciidocEngine;
import io.helidon.build.maven.sitegen.freemarker.FreemarkerEngine;

import static java.util.Objects.requireNonNull;

/**
 * Configuration of {@link FreemarkerEngine} and {@link AsciidocEngine}.
 */
public final class SiteEngine {

    private final AsciidocEngine asciidoc;
    private final FreemarkerEngine freemarker;
    private final Site site;

    private SiteEngine(Builder builder) {
        String backend = requireNonNull(builder.backend, "backend is null!");
        site = requireNonNull(builder.site, "site is null!");
        freemarker = Optional.ofNullable(builder.freemarker)
                             .orElseGet(() -> FreemarkerEngine.create(backend));
        asciidoc = Optional.ofNullable(builder.asciidoc)
                           .orElseGet(() -> AsciidocEngine.create(backend));
    }

    /**
     * Get the site.
     *
     * @return site, never {@code null}
     */
    public Site site() {
        return site;
    }

    /**
     * Get the asciidoc engine.
     *
     * @return asciidoc engine, never {@code null}
     */
    public AsciidocEngine asciidoc() {
        return asciidoc;
    }

    /**
     * Get the freemarker engine.
     *
     * @return freemarker engine, never {@code null}
     */
    public FreemarkerEngine freemarker() {
        return freemarker;
    }

    /**
     * A builder of {@link SiteEngine}.
     */
    public static class Builder implements Supplier<SiteEngine> {

        private FreemarkerEngine freemarker;
        private AsciidocEngine asciidoc;
        private String backend;
        private Site site;

        /**
         * Set the site.
         *
         * @param site site
         * @return this builder
         */
        public Builder site(Site site) {
            this.site = site;
            return this;
        }

        /**
         * Set the backend name.
         *
         * @param backend backend name
         * @return this builder
         */
        public Builder backend(String backend) {
            this.backend = backend;
            return this;
        }

        /**
         * Set the freemarker engine to use.
         *
         * @param freemarker the freemarker engine
         * @return this builder
         */
        public Builder freemarker(FreemarkerEngine freemarker) {
            this.freemarker = freemarker;
            return this;
        }

        /**
         * Set the freemarker engine to use.
         *
         * @param builder the freemarker engine builder
         * @return this builder
         */
        public Builder freemarker(FreemarkerEngine.Builder builder) {
            this.freemarker = builder.build();
            return this;
        }

        /**
         * Set the freemarker engine to use.
         *
         * @param consumer the freemarker engine builder consumer
         * @return this builder
         */
        public Builder freemarker(Consumer<FreemarkerEngine.Builder> consumer) {
            FreemarkerEngine.Builder builder = FreemarkerEngine.builder();
            if (backend != null) {
                builder.backend(backend);
            }
            consumer.accept(builder);
            this.freemarker = builder.build();
            return this;
        }

        /**
         * Set the asciidoctor engine to use.
         *
         * @param asciidoc asciidoc engine
         * @return this builder
         */
        public Builder asciidoctor(AsciidocEngine asciidoc) {
            this.asciidoc = asciidoc;
            return this;
        }

        /**
         * Set the asciidoctor engine to use.
         *
         * @param builder asciidoc engine builder
         * @return this builder
         */
        public Builder asciidoctor(AsciidocEngine.Builder builder) {
            if (backend != null) {
                builder.backend(backend);
            }
            this.asciidoc = builder.build();
            return this;
        }

        /**
         * Set the asciidoctor engine to use.
         *
         * @param consumer asciidoc engine builder consumer
         * @return this builder
         */
        public Builder asciidoctor(Consumer<AsciidocEngine.Builder> consumer) {
            AsciidocEngine.Builder builder = AsciidocEngine.builder();
            if (backend != null) {
                builder.backend(backend);
            }
            consumer.accept(builder);
            this.asciidoc = builder.build();
            return this;
        }

        /**
         * Apply the specified configuration.
         *
         * @param config config
         * @return this builder
         */
        public Builder config(Config config) {
            freemarker = config.get("freemarker")
                               .asOptional()
                               .map(c -> FreemarkerEngine.create(backend, c))
                               .orElse(null);
            asciidoc = config.get("asciidoctor")
                             .asOptional()
                             .map(c -> AsciidocEngine.create(backend, c))
                             .orElse(null);
            return this;
        }

        /**
         * Build the instance.
         *
         * @return new instance.
         */
        public SiteEngine build() {
            return new SiteEngine(this);
        }

        @Override
        public SiteEngine get() {
            return build();
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param backend backend name
     * @param config  config
     * @return new instance
     */
    public static SiteEngine create(String backend, Config config) {
        return builder()
                .backend(backend)
                .config(config)
                .build();
    }

    /**
     * Create a new instance.
     *
     * @param backend backend name
     * @return new instance
     */
    public static SiteEngine create(String backend) {
        return builder()
                .backend(backend)
                .build();
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
