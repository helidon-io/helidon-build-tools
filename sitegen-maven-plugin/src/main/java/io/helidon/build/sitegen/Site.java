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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import io.helidon.config.Config;
import io.helidon.config.ConfigFilters;
import io.helidon.config.ConfigMappers;
import io.helidon.config.ConfigSources;

import static io.helidon.build.sitegen.Helper.checkNonNull;

/**
 * Yet another site generator.
 */
public class Site {

    private static final String ENGINE_PROP = "engine";
    private static final String ASSETS_PROP = "assets";
    private static final String HEADER_PROP = "header";
    private static final String PAGES_PROP = "pages";
    private static final String BACKEND_PROP = "backend";

    /**
     * Ugly!
     */
    public static final ThreadLocal<String> THREADLOCAL = new ThreadLocal<>();

    private final SiteEngine engine;
    private final List<StaticAsset> assets;
    private final Header header;
    private final List<SourcePathFilter> pages;
    private final Backend backend;

    private Site(SiteEngine engine,
                List<StaticAsset> assets,
                Header header,
                List<SourcePathFilter> pages,
                Backend backend) {
        this.backend = backend == null ? new BasicBackend() : backend;
        final String backendName = this.backend.getName();
        THREADLOCAL.set(backendName);
        this.engine = engine == null ? SiteEngine.builder().build() : engine;
        this.assets = assets == null ? Collections.emptyList() : assets;
        this.header = header == null ? new Header() : header;
        this.pages = pages == null ? Collections.emptyList() : pages;
        SiteEngine.register(backendName, this.engine);
    }

    /**
     * Get the configured site engine.
     * @return {@link SiteEngine}, never {@code null}
     */
    public SiteEngine getEngine() {
        return engine;
    }

    /**
     * Get the configured static assets.
     * @return {@link StaticAsset}, never {@code null}
     */
    public List<StaticAsset> getAssets() {
        return assets;
    }

    /**
     * Get the configured header.
     * @return {@link Header}, never {@code null}
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Get the configured pages filter.
     * @return {@link SourcePathFilter}, never {@code null}
     */
    public List<SourcePathFilter> getPages() {
        return pages;
    }

    /**
     * Get the configured backend.
     * @return {@link Backend}, never {@code null}
     */
    public Backend getBackend() {
        return backend;
    }

    /**
     * Triggers rendering of the site.
     *
     * @param sourcedir the source directory containing the site documents, must
     * be an existing directory
     * @param outputdir the output directory where to generate the site files,
     * the directory and the missing parents will be automatically created
     * @throws RenderingException if any error occurs while processing the site
     */
    public void generate(File sourcedir, File outputdir) throws RenderingException {
        try {
            Files.createDirectories(outputdir.toPath());
        } catch (IOException ex) {
            throw new RenderingException(ex.getMessage(), ex);
        }
        backend.generate(new RenderingContext(this, sourcedir, outputdir));
    }

    /**
     * A fluent builder to create {@link Site} instances.
     */
    public static class Builder extends AbstractBuilder<Site> {

        /**
         * Apply the configuration represented by the given configuration file.
         * @param configFile the file to read the configuration from
         * @return the {@link Builder} instance
         */
        public Builder config(File configFile){
            return config(configFile, new Properties());
        }

        /**
         * Apply the configuration represented by the given configuration file.
         * @param configFile the file to read the configuration from
         * @param properties properties to add for resolution
         * @return the {@link Builder} instance
         */
        public Builder config(File configFile, Properties properties) {

            // TODO wrap this with a try/catch for ConfigException.

            // load config file as config nodes
            Config config = Config.builder()
                .addFilter(ConfigFilters.valueResolving())
                .addMapper(Properties.class, (Config c) -> ConfigMappers.toProperties(c.detach()))
                .sources(ConfigSources.file(configFile.getAbsolutePath()), ConfigSources.create(properties))
                .build();

            // backend
            config.get(BACKEND_PROP)
                    .asNode()
                    .ifPresentOrElse(c -> {
                        Backend backend = Backend.builder().config(c).build();
                        put(BACKEND_PROP, backend);
                        THREADLOCAL.set(backend.getName());
                    }, () -> {
                        // default backend
                        Backend backend = new BasicBackend();
                        put(BACKEND_PROP, backend);
                        THREADLOCAL.set(backend.getName());
                    });

            //  engine
            config.get(ENGINE_PROP).ifExists(c -> put(ENGINE_PROP, SiteEngine.builder()
                    .config(c)
                    .build()));

            // assets
            config.get(ASSETS_PROP)
                    .asNodeList()
                    .ifPresent(list -> put(ASSETS_PROP, list
                            .stream()
                            .map(n -> StaticAsset.builder().config(n).build())
                            .collect(Collectors.toList())));

            // header
            config.get(HEADER_PROP).ifExists(c -> put(HEADER_PROP, Header.builder()
                    .config(c)
                    .build()));

            // pages
            config.get(PAGES_PROP)
                    .asNodeList()
                    .ifPresent(list -> put(PAGES_PROP, list
                            .stream()
                            .map(n -> SourcePathFilter.builder()
                                    .config(n)
                                    .build())
                            .collect(Collectors.toList())));

            return this;
        }

        /**
         * Set the page filters.
         * @param pages the filters for all documents in the source directory
         * @return the {@link Builder} instance
         */
        public Builder pages(List<SourcePathFilter> pages){
            put(PAGES_PROP, pages);
            return this;
        }

        /**
         * Set the site engine.
         * @param engine the site engine to use
         * @return the {@link Builder} instance
         */
        public Builder engine(SiteEngine engine){
            put(ENGINE_PROP, engine);
            return this;
        }

        /**
         * Set the backend.
         *
         * Must be invoked first.
         *
         * @param backend the backend to use
         * @return the {@link Builder} instance
         */
        public Builder backend(Backend backend){
            checkNonNull(backend, BACKEND_PROP);
            put(BACKEND_PROP, backend);
            THREADLOCAL.set(backend.getName());
            return this;
        }

        /**
         * Set the header.
         * @param header the header to use
         * @return the {@link Builder} instance
         */
        public Builder header(Header header){
            put(HEADER_PROP, header);
            return this;
        }

        /**
         * Set the static asset filters.
         * @param assets the assets to use
         * @return the {@link Builder} instance
         */
        public Builder assets(List<StaticAsset> assets){
            put(ASSETS_PROP, assets);
            return this;
        }

        @Override
        public Site build(){
            SiteEngine engine = null;
            List<StaticAsset> assets = null;
            Header header = null;
            List<SourcePathFilter> pages = null;
            Backend backend = null;
            for (Map.Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case(ENGINE_PROP):
                        engine = asType(val, SiteEngine.class);
                        break;
                    case(ASSETS_PROP):
                        assets = asList(val, StaticAsset.class);
                        break;
                    case(HEADER_PROP):
                        header = asType(val, Header.class);
                        break;
                    case(PAGES_PROP):
                        pages = asList(val, SourcePathFilter.class);
                        break;
                    case(BACKEND_PROP):
                        backend = asType(val, Backend.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new Site(engine, assets, header, pages, backend);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     *
     * @return the created builder
     */
    public static Builder builder(){
        return new Builder();
    }
}
