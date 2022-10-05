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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.helidon.build.maven.sitegen.models.Header;
import io.helidon.build.maven.sitegen.models.Page;
import io.helidon.build.maven.sitegen.models.PageFilter;
import io.helidon.build.maven.sitegen.models.StaticAsset;
import io.helidon.build.maven.sitegen.spi.BackendProvider;

import org.asciidoctor.log.Severity;

/**
 * Yet another site generator.
 */
public class Site {

    private final SiteEngine engine;
    private final List<StaticAsset> assets;
    private final Header header;
    private final List<PageFilter> pages;
    private final Backend backend;
    private final Map<String, Object> options;

    private Site(Builder builder) {
        backend = Optional.ofNullable(builder.backend)
                          .orElseGet(BasicBackend::create);
        engine = Optional.ofNullable(builder.engine)
                         .orElseGet(() -> SiteEngine.builder().backend(backend.name()))
                         .site(this)
                         .build();
        header = Optional.ofNullable(builder.header)
                         .orElseGet(Header::create);
        options = builder.options;
        assets = builder.assets;
        pages = builder.pages;
    }

    /**
     * Get the configured site engine.
     *
     * @return site engine, never {@code null}
     */
    public SiteEngine engine() {
        return engine;
    }

    /**
     * Get the configured static assets.
     *
     * @return list of assets, never {@code null}
     */
    public List<StaticAsset> assets() {
        return assets;
    }

    /**
     * Get the configured header.
     *
     * @return header, never {@code null}
     */
    public Header header() {
        return header;
    }

    /**
     * Get the configured pages filter.
     *
     * @return list of filters, never {@code null}
     */
    public List<PageFilter> pages() {
        return pages;
    }

    /**
     * Get the configured backend.
     *
     * @return backend, never {@code null}
     */
    public Backend backend() {
        return backend;
    }

    /**
     * Triggers rendering of the site.
     *
     * @param sourceDir the source directory containing the site documents
     * @param outputDir the output directory where to generate the site files
     * @throws RenderingException if any error occurs while processing the site
     */
    public void generate(Path sourceDir, Path outputDir) throws RenderingException {
        try {
            Files.createDirectories(outputDir);
            Context ctx = new Context(this, sourceDir, outputDir);
            ctx.runInContext(() -> backend.generate(ctx));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Read a given document metadata.
     *
     * @param source the file to read the metadata from
     * @return the {@link Page.Metadata} instance, never {@code null}
     */
    public Page.Metadata readMetadata(Path source) {
        Context context = new Context(this, source.getParent(), null);
        return context.runInContext(() -> backend.renderer(source).readMetadata(source));
    }

    /**
     * Get the options.
     *
     * @return options
     * @see Options
     */
    public Map<String, Object> options() {
        return options;
    }

    /**
     * Constants for options keys.
     */
    public static final class Options {

        /**
         * Option that controls strict validation of the cross-references.
         * Value can be {@code true} or {@code false}, default is {@code true}.
         */
        public static final String STRICT_XREF = "strict-xref";

        /**
         * Option that controls if errors are raised from missing templates.
         */
        public static final String STRICT_TEMPLATES = "strict-templates";

        /**
         * Option that controls if errors are raised from unresolved images.
         */
        public static final String STRICT_IMAGES = "strict-images";

        /**
         * Option that controls if errors are raised from asciidoc log records.
         * Value is a string representation of {@link Severity}.
         */
        public static final String FAIL_ON = "fail-on";
    }

    /**
     * A builder of {@link Site}.
     */
    @SuppressWarnings("unused")
    public static class Builder {

        private Backend backend;
        private SiteEngine.Builder engine;
        private Header header;
        private final Map<String, Object> options = new HashMap<>();
        private final List<StaticAsset> assets = new ArrayList<>();
        private final List<PageFilter> pages = new ArrayList<>();

        /**
         * Add options.
         *
         * @param options map of options
         * @return this builder
         */
        public Builder options(Map<String, Object> options) {
            if (options != null) {
                this.options.putAll(options);
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
            return config(config, Map.of());
        }

        /**
         * Apply the specified configuration.
         *
         * @param config     config
         * @param properties properties to add for resolution
         * @return this builder
         */
        public Builder config(Config config, Map<String, String> properties) {
            backend = config.get("backend")
                            .asOptional()
                            .map(BackendProvider::get)
                            .orElseGet(BasicBackend::create);

            engine = SiteEngine.builder().backend(backend.name());

            header = config.get("header")
                           .asOptional()
                           .map(Header::create).orElse(null);

            config.get("engine")
                  .asOptional()
                  .ifPresent(c -> engine.config(c));

            config.get("assets")
                  .asNodeList()
                  .orElseGet(List::of)
                  .stream()
                  .map(StaticAsset::create)
                  .forEach(assets::add);

            options.putAll(config.get("options")
                                 .asMap()
                                 .orElseGet(Map::of));

            config.get("pages")
                  .asNodeList()
                  .orElseGet(List::of)
                  .stream()
                  .map(PageFilter::create)
                  .forEach(pages::add);
            return this;
        }

        /**
         * Add page filters.
         *
         * @param pages document filters
         * @return this builder
         */
        public Builder pages(List<PageFilter> pages) {
            if (pages != null) {
                this.pages.addAll(pages);
            }
            return this;
        }

        /**
         * Add a page filter.
         *
         * @param supplier document filter supplier
         * @return this builder
         */
        public Builder page(Supplier<PageFilter> supplier) {
            if (supplier != null) {
                this.pages.add(supplier.get());
            }
            return this;
        }

        /**
         * Add a page filter.
         *
         * @param page document filter
         * @return this builder
         */
        public Builder page(PageFilter page) {
            if (page != null) {
                this.pages.add(page);
            }
            return this;
        }

        /**
         * Set the site engine.
         *
         * @param builder builder
         * @return this builder
         */
        public Builder engine(SiteEngine.Builder builder) {
            if (backend != null) {
                builder.backend(backend.name());
            }
            this.engine = builder;
            return this;
        }

        /**
         * Set the site engine.
         *
         * @param consumer consumer
         * @return this builder
         */
        public Builder engine(Consumer<SiteEngine.Builder> consumer) {
            SiteEngine.Builder builder = SiteEngine.builder();
            if (backend != null) {
                builder.backend(backend.name());
            }
            consumer.accept(builder);
            this.engine = builder;
            return this;
        }

        /**
         * Set the backend.
         *
         * @param backend the backend to use
         * @return this builder
         */
        public Builder backend(Backend backend) {
            this.backend = backend;
            return this;
        }

        /**
         * Set the backend.
         * <p>
         * Must be invoked first.
         *
         * @param supplier backend supplier
         * @return this builder
         */
        public Builder backend(Supplier<? extends Backend> supplier) {
            this.backend = supplier.get();
            return this;
        }

        /**
         * Set the header.
         *
         * @param header the header to use
         * @return this builder
         */
        public Builder header(Header header) {
            this.header = header;
            return this;
        }

        /**
         * Set the header.
         *
         * @param supplier supplier of header
         * @return this builder
         */
        public Builder header(Supplier<Header> supplier) {
            if (supplier != null) {
                this.header = supplier.get();
            }
            return this;
        }

        /**
         * Add static asset filters.
         *
         * @param assets the assets to use
         * @return this builder
         */
        public Builder assets(List<StaticAsset> assets) {
            if (assets != null) {
                this.assets.addAll(assets);
            }
            return this;
        }

        /**
         * Add a static asset filter.
         *
         * @param asset the asset add
         * @return this builder
         */
        public Builder asset(StaticAsset asset) {
            if (asset != null) {
                this.assets.add(asset);
            }
            return this;
        }

        /**
         * Add a static asset filter.
         *
         * @param supplier supplier of asset
         * @return this builder
         */
        public Builder asset(Supplier<StaticAsset> supplier) {
            if (supplier != null) {
                this.assets.add(supplier.get());
            }
            return this;
        }

        /**
         * Build a new instance.
         *
         * @return new instance
         */
        public Site build() {
            return new Site(this);
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param config config
     * @return new instance
     */
    public static Site create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Create a new instance.
     *
     * @param backend backend
     * @return new instance
     */
    public static Site create(Backend backend) {
        return builder().backend(backend).build();
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
