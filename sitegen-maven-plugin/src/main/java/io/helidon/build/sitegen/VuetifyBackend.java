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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.sitegen.asciidoctor.AsciidocPageRenderer;
import io.helidon.build.sitegen.freemarker.FreemarkerEngine;
import io.helidon.build.sitegen.freemarker.TemplateSession;
import io.helidon.config.Config;

import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;
import static io.helidon.build.sitegen.Helper.copyResources;
import static io.helidon.build.sitegen.Helper.loadResourceDirAsPath;
import static io.helidon.build.sitegen.asciidoctor.AsciidocPageRenderer.ADOC_EXT;
import static io.helidon.common.CollectionsHelper.mapOf;

/**
 * A backend implementation for vuetifyjs.
 * @see <a href="https://vuetifyjs.com">https://vuetifyjs.com</a>
 */
public class VuetifyBackend extends Backend {

    /**
     * The Vuetify backend name.
     */
    public static final String BACKEND_NAME = "vuetify";

    private static final String STATIC_RESOURCES = "/helidon-sitegen-static/vuetify";
    private static final String THEME_PROP = "theme";
    private static final String NAVIGATION_PROP = "navigation";
    private static final String HOME_PAGE_PROP = "homePage";
    private static final String RELEASES_PROP = "releases";

    private final Map<String, PageRenderer> pageRenderers;
    private final VuetifyNavigation navigation;
    private final Map<String, String> theme;
    private final Path staticResources;
    private final String homePage;
    private final List<String> releases;

    private VuetifyBackend(Map<String, String> theme,
                           VuetifyNavigation navigation,
                           String homePage,
                           List<String> releases) {
        super(BACKEND_NAME);
        checkNonNullNonEmpty(homePage, HOME_PAGE_PROP);
        this.theme = theme == null ? Collections.emptyMap() : theme;
        this.navigation = navigation;
        this.homePage = homePage;
        this.releases = releases == null ? Collections.emptyList() : releases;
        this.pageRenderers = mapOf(
                ADOC_EXT, new AsciidocPageRenderer(BACKEND_NAME)
        );
        try {
            staticResources = loadResourceDirAsPath(STATIC_RESOURCES);
        } catch (URISyntaxException | IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get the navigation.
     * @return {@link VuetifyNavigation} or {@code null} if not set
     */
    public VuetifyNavigation getNavigation() {
        return navigation;
    }

    /**
     * Get the theme.
     * @return {@code Map<String,String>}, never {@code null}
     */
    public Map<String, String> getTheme() {
        return theme;
    }

    /**
     * Get the home page source path.
     * @return the source path or {@code null} if not set
     */
    public String getHomePage() {
        return homePage;
    }

    /**
     * Get the list of releases.
     * @return {@code List<String,String>}, never {@code null}
     */
    public List<String> getReleases() {
        return releases;
    }

    @Override
    public Map<String, PageRenderer> pageRenderers() {
        return pageRenderers;
    }

    @Override
    public void generate(RenderingContext ctx) {
        File pagesdir = new File(ctx.getOutputdir(), "pages");
        try {
            Files.createDirectories(pagesdir.toPath());
        } catch (IOException ex) {
            throw new RenderingException(ex.getMessage(), ex);
        }

        // render all pages
        ctx.processPages(pagesdir, "js");

        // copy declared assets
        ctx.copyStaticAssets();

        TemplateSession session = ctx.getTemplateSession();

        Page home = ctx.getPages().get(new SourcePath(homePage).asString());
        if (home == null) {
            throw new IllegalStateException("unable to get home page");
        }

        // resolve navigation
        VuetifyNavigation resolvedNavigation = navigation == null ? null
                : navigation.resolve(ctx.getPages().values());

        // resolve navigation routes
        List<String> navRouteEntries = resolvedNavigation != null
                 ? resolvedNavigation.getItems().stream()
                .filter((item) -> item.isGroup())
                .flatMap((item) -> item.asGroup().getItems().stream())
                .filter((groupItem) -> groupItem.isSubGroup())
                .flatMap((groupItem) -> groupItem.asSubGroup().getItems().stream())
                .filter((subGroupItem) -> subGroupItem.isLink())
                .map((subGroupItem) -> ctx.getPageForRoute(
                        subGroupItem.asLink()
                            .getHref())
                            .getSourcePath())
                .collect(Collectors.toList()) : Collections.emptyList();

        // resolve route entries
        List<String> routeEntries = Stream.concat(
                navRouteEntries.contains(home.getSourcePath())
                ? Stream.empty() : Stream.of(home.getSourcePath()),
                Stream.concat(navRouteEntries.stream(),
                        ctx.getPages().keySet().stream()
                                .filter(item -> !navRouteEntries.contains(item))))
                .collect(Collectors.toList());

        Map<String, String> allBindings = session.getVueBindings().getBindings();

        Map<String, Object> model = new HashMap<>();
        model.put("searchEntries", session.getSearchIndex().getEntries());
        model.put("navRouteEntries", navRouteEntries);
        model.put("routeEntries", routeEntries);
        model.put("customLayoutEntries", session.getCustomLayouts().getMappings());
        model.put("pages", ctx.getPages());
        model.put("metadata", home.getMetadata());
        model.put("navigation", resolvedNavigation);
        model.put("header", ctx.getSite().getHeader());
        model.put("theme", theme);
        model.put("home", home);
        model.put("releases", releases);
        model.put("bindings", allBindings);

        FreemarkerEngine freemarker = ctx.getSite().getEngine().freemarker();

        // custom bindings
        for (Page page : ctx.getPages().values()) {
            String bindings = allBindings.get(page.getSourcePath());
            if (bindings != null) {
                Map<String, Object> bindingsModel = new HashMap<>(model);
                bindingsModel.put("bindings", bindings);
                bindingsModel.put("page", page);
                freemarker.renderFile("custom_bindings",
                        "pages/" + page.getTargetPath() + "_custom.js",
                        bindingsModel, ctx);
            }
        }

        // render search-index.js
        freemarker.renderFile("search_index", "main/search-index.json", model, ctx);

        // render index.html
        freemarker.renderFile("index", "index.html", model, ctx);

        // render main/config.js
        freemarker.renderFile("config", "main/config.js", model, ctx);

        // copy vuetify resources
        try {
            copyResources(staticResources, ctx.getOutputdir());
        } catch (IOException ex) {
            throw new RenderingException(
                    "An error occurred during static resource processing ", ex);
        }
    }

    /**
     * A fluent builder to create {@link VuetifyBackend} instances.
     */
    public static class Builder extends Backend.Builder<VuetifyBackend> {

        /**
         * Set the theme.
         * @param theme a {@code Map<String, String>} representing theme options
         * @return the {@link Builder} instance
         */
        public Builder theme(Map<String, String> theme){
            put(THEME_PROP, theme);
            return this;
        }

        /**
         * Set the navigation.
         * @param navigation see {@link VuetifyNavigation}
         * @return the {@link Builder} instance
         */
        public Builder navigation(VuetifyNavigation navigation) {
            put(NAVIGATION_PROP, navigation);
            return this;
        }

        /**
         * Set the home page source path.
         * @param homePage the source path of the document to use as home page
         * @return the {@link Builder} instance
         */
        public Builder homePage(String homePage) {
            put(HOME_PAGE_PROP, homePage);
            return this;
        }

        /**
         * Set the releases.
         * @param releases a {@code List<String>} representing all release versions
         * @return the {@link Builder} instance
         */
        public Builder releases(List<String> releases){
            put(RELEASES_PROP, releases);
            return this;
        }

        @Override
        public Builder config(Config node) {
            if (node.exists()) {
                // theme
                node.get(THEME_PROP).ifExists(c
                        -> put(THEME_PROP, c.detach().asMap()));

                // navigation
                node.get(NAVIGATION_PROP).ifExists(c
                        -> put(NAVIGATION_PROP, VuetifyNavigation.builder()
                                .config(c)
                                .build()));

                // homePage
                node.get(HOME_PAGE_PROP).ifExists(c
                        -> put(HOME_PAGE_PROP, c.asString()));

                // releases
                node.get(RELEASES_PROP).ifExists(c
                        -> put(RELEASES_PROP, c.asStringList()));
            }
            return this;
        }

        @Override
        public VuetifyBackend build() {
            Map<String, String> theme = null;
            VuetifyNavigation navigation = null;
            String homePage = null;
            List<String> releases = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case (THEME_PROP):
                        theme = asMap(val, String.class, String.class);
                        break;
                    case (NAVIGATION_PROP):
                        navigation = asType(val, VuetifyNavigation.class);
                        break;
                    case (HOME_PAGE_PROP):
                        homePage = asType(val, String.class);
                        break;
                    case (RELEASES_PROP):
                        releases = asList(val, String.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new VuetifyBackend(theme, navigation, homePage, releases);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(){
        return new Builder();
    }
}
