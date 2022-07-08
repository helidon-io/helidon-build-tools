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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.build.common.SourcePath;
import io.helidon.build.maven.sitegen.freemarker.FreemarkerEngine;
import io.helidon.build.maven.sitegen.freemarker.TemplateSession;
import io.helidon.build.maven.sitegen.models.Nav;
import io.helidon.build.maven.sitegen.models.Page;

import static io.helidon.build.common.FileUtils.resourceAsPath;
import static io.helidon.build.common.Strings.requireValid;
import static io.helidon.build.maven.sitegen.Context.copyResources;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * A backend implementation for Vuetify.
 *
 * @see <a href="https://vuetifyjs.com">https://vuetifyjs.com</a>
 */
public class VuetifyBackend extends Backend {

    /**
     * The Vuetify backend name.
     */
    public static final String BACKEND_NAME = "vuetify";

    private final Nav nav;
    private final Map<String, String> theme;
    private final Path staticFiles;
    private final String home;
    private final List<String> releases;

    private VuetifyBackend(Builder builder) {
        super(BACKEND_NAME);
        theme = builder.theme;
        nav = builder.nav;
        home = requireValid(builder.home, "home is invalid!");
        releases = builder.releases;
        staticFiles = resourceAsPath("/files/vuetify", VuetifyBackend.class);
    }

    /**
     * Get the navigation tree root.
     *
     * @return navigation tree root or {@code null} if not set
     */
    public Nav nav() {
        return nav;
    }

    /**
     * Get the theme.
     *
     * @return map, never {@code null}
     */
    public Map<String, String> theme() {
        return theme;
    }

    /**
     * Get the home.
     *
     * @return home or {@code null} if not set
     */
    public String home() {
        return home;
    }

    /**
     * Get the releases.
     *
     * @return list, never {@code null}
     */
    public List<String> releases() {
        return releases;
    }

    @Override
    public void generate(Context ctx) {
        Map<String, Page> pages = ctx.pages();
        Page home = pages.get(new SourcePath(this.home).asString(false));
        if (home == null) {
            throw new IllegalStateException("unable to get home page");
        }

        // resolve navigation
        Nav resolvedNav;
        List<String> navRoutes;
        if (nav != null) {
            resolvedNav = resolveNav(ctx);
            navRoutes = resolvedNav.items()
                                   .stream()
                                   .flatMap(n -> n.items().isEmpty() ? Stream.of(n) : n.items().stream()) // depth:1
                                   .flatMap(n -> n.items().isEmpty() ? Stream.of(n) : n.items().stream()) // depth:2
                                   .flatMap(n -> n.items().isEmpty() ? Stream.of(n) : n.items().stream()) // depth:3
                                   .map(Nav::to)
                                   .filter(Objects::nonNull)
                                   .collect(toList());
        } else {
            resolvedNav = null;
            navRoutes = List.of();
        }

        Path pagesDir = ctx.outputDir().resolve("pages");
        try {
            Files.createDirectories(pagesDir);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        // render all pages
        ctx.processPages(pagesDir, "js");

        // copy declared assets
        ctx.copyStaticAssets();

        TemplateSession session = ctx.templateSession();

        Map<String, Page> pagesByRoute = pages.values()
                                              .stream()
                                              .map(p -> Map.entry(p.target(), p))
                                              .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        // resolve all routes
        List<String> routes = new ArrayList<>();
        routes.add(home.target());
        navRoutes.forEach(r -> {
            if (!routes.contains(r)) {
                routes.add(r);
            }
        });
        pagesByRoute.keySet().forEach(r -> {
            if (!routes.contains(r)) {
                routes.add(r);
            }
        });

        Map<String, String> allBindings = session.vueBindings().bindings();

        Map<String, Object> model = new HashMap<>();
        model.put("searchEntries", session.searchIndex().entries());
        model.put("navRoutes", navRoutes);
        model.put("allRoutes", routes);
        model.put("customLayoutEntries", session.customLayouts().mappings());
        model.put("pages", pagesByRoute);
        model.put("metadata", home.metadata());
        model.put("nav", resolvedNav);
        model.put("header", ctx.site().header());
        model.put("theme", theme);
        model.put("home", home);
        model.put("releases", releases);
        model.put("bindings", allBindings);

        FreemarkerEngine freemarker = ctx.site().engine().freemarker();
        Path outputDir = ctx.outputDir();

        // custom bindings
        for (Page page : pages.values()) {
            String bindings = allBindings.get(page.source());
            if (bindings != null) {
                Map<String, Object> bindingsModel = new HashMap<>(model);
                bindingsModel.put("bindings", bindings);
                bindingsModel.put("page", page);
                String path = "pages/" + page.target() + "_custom.js";
                freemarker.renderFile("custom_bindings", path, bindingsModel, outputDir);
            }
        }

        // render search-index.js
        freemarker.renderFile("search_index", "main/search-index.json", model, outputDir);

        // render index.html
        freemarker.renderFile("index", "index.html", model, outputDir);

        // render main/config.js
        freemarker.renderFile("config", "main/config.js", model, outputDir);

        // copy vuetify resources
        copyResources(staticFiles, ctx.outputDir());
    }

    private Nav resolveNav(Context ctx) {
        Map<String, Page> pages = ctx.pages();
        Deque<Nav.Builder> builders = new ArrayDeque<>();
        Deque<Nav> stack = new ArrayDeque<>();
        stack.push(nav);
        builders.push(Nav.builder());
        Nav.Builder parent = null;
        while (!stack.isEmpty() && !builders.isEmpty()) {
            Nav node = stack.peek();
            Nav.Builder builder = builders.peek();
            List<Nav> items = node.items();
            if (parent != builder && !items.isEmpty()) {
                // 1st tree-node pass
                ListIterator<Nav> it = items.listIterator(items.size());
                while (it.hasPrevious()) {
                    Nav item = it.previous();
                    builders.push(Nav.builder(builder)
                                     .type(item.type()));
                    stack.push(item);
                }
            } else {
                // leaf-node, or 2nd tree-node pass
                Nav.Builder nodeParent = builder.parent();
                builder.title(node.title())
                       .to(node.to())
                       .href(node.href())
                       .pathprefix(node.pathprefix())
                       .glyph(node.glyph());

                Nav.Type type = node.type();
                if (type == Nav.Type.PAGE) {
                    String source = node.source();
                    // resolve source
                    if (source != null) {
                        try {
                            Page page = Nav.resolvePage(pages, source);
                            builder.title(page.metadata().title(), false)
                                   .source(source)
                                   .to(page.target());
                        } catch (IllegalArgumentException ex) {
                            ctx.error(new RenderingException(ex.getMessage()));
                        }
                    }
                } else if (type == Nav.Type.MENU) {
                    // resolve sources
                    for (String source : node.sources()) {
                        try {
                            String resolvedSource = new SourcePath(node.dir(), source).asString(false);
                            Page page = Nav.resolvePage(pages, resolvedSource);
                            builder.item(Nav.Type.PAGE, p ->
                                    p.title(page.metadata().title())
                                     .source(page.source())
                                     .to(page.target()));
                        } catch (IllegalArgumentException ex) {
                            ctx.error(new RenderingException(ex.getMessage()));
                        }
                    }

                    // resolve filter
                    List<Page> resolvedPages = node.resolvePages(pages.values());
                    if (!resolvedPages.isEmpty()) {
                        resolvedPages.sort(Comparator.comparing(Page::source));
                    }
                    for (Page page : resolvedPages) {
                        builder.item(Nav.Type.PAGE, p ->
                                p.title(page.metadata().title())
                                 .source(page.source())
                                 .to(page.target()));
                    }
                }
                if (nodeParent != null) {
                    parent = nodeParent.item(builder);
                    builders.pop();
                }
                stack.pop();
            }
        }
        return builders.pop().build();
    }

    /**
     * A builder of {@link VuetifyBackend}.
     */
    @SuppressWarnings("unused")
    public static class Builder implements Supplier<VuetifyBackend> {

        private final Map<String, String> theme = new HashMap<>();
        private Nav nav;
        private String home;
        private final List<String> releases = new ArrayList<>();

        /**
         * Set the theme.
         *
         * @param theme a map containing theme options
         * @return this builder
         */
        public Builder theme(Map<String, String> theme) {
            if (theme != null) {
                this.theme.putAll(theme);
            }
            return this;
        }

        /**
         * Set the navigation.
         *
         * @param nav nav
         * @return this builder
         */
        public Builder nav(Nav nav) {
            this.nav = nav;
            return this;
        }

        /**
         * Set the navigation.
         *
         * @param supplier navigation supplier
         * @return this builder
         */
        public Builder nav(Supplier<Nav> supplier) {
            this.nav = supplier.get();
            return this;
        }

        /**
         * Set the home.
         *
         * @param home source path of the home page document
         * @return this builder
         */
        public Builder home(String home) {
            this.home = home;
            return this;
        }

        /**
         * Set the releases.
         *
         * @param releases releases
         * @return this builder
         */
        public Builder releases(List<String> releases) {
            if (releases != null) {
                this.releases.addAll(releases);
            }
            return this;
        }

        /**
         * Set the releases.
         *
         * @param releases releases
         * @return this builder
         */
        public Builder releases(String... releases) {
            if (releases != null) {
                this.releases.addAll(Arrays.asList(releases));
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
            theme.putAll(config.get("theme")
                               .asMap(String.class)
                               .orElseGet(Map::of));
            home = config.get("homePage")
                         .asString()
                         .orElse(null);
            releases.addAll(config.get("releases")
                                  .asList(String.class)
                                  .orElseGet(List::of));
            nav = config.get("navigation")
                        .asOptional()
                        .map(Nav::create)
                        .orElse(null);
            return this;
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public VuetifyBackend build() {
            return new VuetifyBackend(this);
        }

        @Override
        public VuetifyBackend get() {
            return build();
        }

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
