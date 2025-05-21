/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.build.common.SourcePath;
import io.helidon.build.maven.sitegen.Config;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Navigation tree.
 */
@SuppressWarnings("unused")
public final class Nav extends SourcePathFilter {

    /**
     * Navigation node type.
     */
    public enum Type {
        /**
         * Root node. Contains the following attributes:
         * <ul>
         *     <li>{@link #title()}</li>
         *     <li>{@link #items()}</li>
         *     <li>{@link #glyph()} (optional)</li>
         * </ul>
         */
        ROOT,

        /**
         * Tree node. Contains the following attributes:
         * <ul>
         *     <li>{@link #items()}</li>
         * </ul>
         */
        GROUPS,

        /**
         * Tree node. Contains the following attributes:
         * <ul>
         *     <li>{@link #title()}</li>
         *     <li>{@link #dir()}</li>
         *     <li>{@link #pathprefix()} (optional)</li>
         *     <li>{@link #items()}</li>
         * </ul>
         */
        GROUP,

        /**
         * Tree node. Contains the following attributes:
         * <ul>
         *     <li>{@link #title()}</li>
         *     <li>{@link #dir()}</li>
         *     <li>{@link #pathprefix()} (optional)</li>
         *     <li>{@link #sources()} (optional)</li>
         *     <li>{@link #includes()} (optional)</li>
         *     <li>{@link #excludes()} (optional)</li>
         *     <li>{@link #glyph()} (optional)</li>
         * </ul>
         */
        MENU,

        /**
         * Leaf node. Contains the following attributes:
         * <ul>
         *     <li>{@link #source()}</li>
         *     <li>{@link #title()} (optional)</li>
         *     <li>{@link #glyph()} (optional)</li>
         * </ul>
         */
        PAGE,

        /**
         * Leaf node. Contains the following attributes:
         * <ul>
         *     <li>{@link #title()}</li>
         *     <li>{@link #href()}</li>
         *     <li>{@link #target()} (optional)</li>
         *     <li>{@link #glyph()} (optional)</li>
         * </ul>
         */
        LINK,

        /**
         * Leaf node. Contains the following attributes:
         * <ul>
         *     <li>{@link #title()}</li>
         * </ul>
         */
        HEADER;

        /**
         * Test if this type is one of the given types.
         *
         * @param types types to match
         * @return {@code true} if this type matched, {@code false} otherwise
         */
        boolean is(Type... types) {
            for (Type type : types) {
                if (this == type) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the names.
         *
         * @param types types
         * @return names
         */
        static String names(Type... types) {
            return Arrays.stream(types).map(Enum::name).collect(Collectors.joining("|"));
        }

        /**
         * Get the names.
         *
         * @return names
         */
        static String names() {
            return names(values());
        }

        /**
         * Create a type from config.
         *
         * @param config config
         * @return type or {@code null}
         */
        static Type create(Config config) {
            return config.asString()
                         .map(s -> Type.valueOf(s.toUpperCase()))
                         .orElse(null);
        }
    }

    private final Type type;
    private final String title;
    private final Glyph glyph;
    private final String to;
    private final String source;
    private final String href;
    private final String target;
    private final String dir;
    private final String pathprefix;
    private final List<String> sources;
    private final List<Nav> items;
    private final int depth;

    private Nav(Builder builder) {
        super(builder);
        type = Objects.requireNonNull(builder.type, "type is null!");
        title = builder.title;
        glyph = builder.glyph;
        sources = builder.sources;
        to = builder.to;
        href = builder.href;
        target = Objects.requireNonNull(builder.target, "target is null!");
        items = builder.items;
        depth = builder.maxDepth;
        dir = builder.dir;
        pathprefix = builder.pathprefix;
        source = builder.source;
    }

    /**
     * Get the depth.
     *
     * @return depth
     */
    public int depth() {
        return depth;
    }

    /**
     * Get the type.
     *
     * @return type
     */
    public Type type() {
        return type;
    }

    /**
     * Get the title.
     *
     * @return title, may be {@code null}
     */
    public String title() {
        return title;
    }

    /**
     * Get the source.
     *
     * @return source, may be {@code null}
     */
    public String source() {
        return source;
    }

    /**
     * Get the sources.
     *
     * @return source, never {@code null}
     */
    public List<String> sources() {
        return sources;
    }

    /**
     * Get the glyph.
     *
     * @return glyph, may be {@code null}
     */
    public Glyph glyph() {
        return glyph;
    }

    /**
     * Get the "to" value.
     *
     * @return to value, may be {@code null}
     */
    public String to() {
        return to;
    }

    /**
     * Get the href value.
     *
     * @return href value, may be {@code null}
     */
    public String href() {
        return href;
    }

    /**
     * Get the href target.
     *
     * @return href target, never {@code null}
     */
    public String target() {
        return target;
    }

    /**
     * Get the directory.
     *
     * @return directory, may be {@code null}
     */
    public String dir() {
        return dir;
    }

    /**
     * Get the path prefix.
     *
     * @return path prefix, may be {@code null}
     */
    public String pathprefix() {
        return pathprefix;
    }

    /**
     * Get the nested items.
     *
     * @return list of items
     */
    public List<Nav> items() {
        return items;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case "type":
                return type.name().toLowerCase();
            case "title":
                return title;
            case "glyph":
                return glyph;
            case "to":
                return to;
            case "href":
                return href;
            case "target":
                return target;
            case "depth":
                return depth;
            case "pathprefix":
                return pathprefix;
            case "items":
                return items;
            case "islink":
                return href != null;
            default:
                return super.get(attr);
        }
    }

    @Override
    public String toString() {
        return "Nav{"
                + "type=" + type
                + ", title='" + title + '\''
                + '}';
    }

    /**
     * Resolve this filter.
     *
     * @param sources input sources
     * @return resolved paths
     */
    public List<String> resolveSources(Collection<String> sources) {
        if (includes().isEmpty()) {
            return List.of();
        }
        List<SourcePath> paths = sources.stream()
                                        .map(SourcePath::new)
                                        .collect(toList());
        return SourcePath.filter(paths, includes(), excludes())
                         .stream()
                         .map(SourcePath::toString)
                         .collect(toList());
    }

    /**
     * Resolve this filter.
     *
     * @param pages input pages
     * @return resolved pages
     * @throws IllegalStateException if a page cannot be resolved
     */
    public List<Page> resolvePages(Collection<Page> pages) {
        if (includes().isEmpty()) {
            return List.of();
        }
        Map<SourcePath, Page> paths = pages.stream()
                                           .collect(toMap(Page::sourcePath, Function.identity()));
        List<SourcePath> resolvedPaths = resolvePaths(paths.keySet());
        return resolvedPaths.stream()
                            .map(s -> resolvePage(paths, s))
                            .collect(toList());
    }

    /**
     * Resolve a page by source.
     *
     * @param pages  pages map
     * @param source source
     * @param <T>    key type
     * @return resolved page, never {@code null}
     * @throws IllegalStateException if the page cannot be resolved
     */
    public static <T> Page resolvePage(Map<T, Page> pages, T source) {
        Page page = pages.get(source);
        if (page == null) {
            throw new IllegalArgumentException("Unable to get page for path: " + source.toString());
        }
        return page;
    }

    /**
     * Resolve pages by source.
     *
     * @param pages   pages map
     * @param sources sources
     * @param <T>     map key type
     * @return resolved page, never {@code null}
     * @throws IllegalStateException if a page cannot be resolved
     */
    public static <T> List<Page> resolvePages(Map<T, Page> pages, List<T> sources) {
        return sources.stream().map(s -> resolvePage(pages, s)).collect(toList());
    }

    /**
     * Create a new instance from config.
     *
     * @param config config
     * @return navigation tree root node
     */
    public static Nav create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder(null);
    }

    /**
     * Create a new builder.
     *
     * @param parent parent builder
     * @return builder
     */
    public static Builder builder(Nav.Builder parent) {
        return new Builder(parent);
    }

    /**
     * Builder of {@link Nav}.
     */
    public static final class Builder extends AbstractBuilder<Builder, Nav> {

        private static final String DEFAULT_TARGET = "_blank";
        private String title;
        private Glyph glyph;
        private String to;
        private String href;
        private String target = DEFAULT_TARGET;
        private String dir;
        private String pathprefix;
        private String source;
        private Type type;
        private final List<Nav> items = new ArrayList<>();
        private final List<String> sources = new ArrayList<>();
        private final Builder parent;
        private final int depth;
        private int maxDepth;

        private Builder(Builder parent) {
            this.parent = parent;
            if (parent != null) {
                this.depth = parent.depth + 1;
            } else {
                this.depth = 0;
                this.type = Type.ROOT;
            }
            this.maxDepth = depth;
        }

        private String effectiveDir() {
            LinkedList<String> dirs = new LinkedList<>();
            Builder builder = this;
            while (builder != null) {
                if (builder.dir != null) {
                    dirs.addFirst(builder.dir);
                }
                builder = builder.parent;
            }
            if (dirs.isEmpty()) {
                return null;
            }
            return new SourcePath(dirs).asString(false);
        }

        /**
         * Get the max depth.
         *
         * @return max depth
         */
        public int maxDepth() {
            return maxDepth;
        }

        /**
         * Get the parent builder.
         *
         * @return parent builder
         */
        public Builder parent() {
            return parent;
        }

        /**
         * Set the max depth.
         *
         * @param maxDepth max depth
         * @return this builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder maxDepth(int maxDepth) {
            if (maxDepth > this.maxDepth) {
                this.maxDepth = maxDepth;
            }
            return this;
        }

        /**
         * Set the type.
         *
         * @param type type
         * @return this builder
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Set the title.
         *
         * @param title title
         * @return this builder
         */
        public Builder title(String title) {
            return title(title, true);
        }

        /**
         * Set the title.
         *
         * @param title     title
         * @param overwrite {@code true} if the existing value should be overwritten
         * @return this builder
         */
        public Builder title(String title, boolean overwrite) {
            if (title != null && (this.title == null || overwrite)) {
                this.title = title;
            }
            return this;
        }

        /**
         * Set the page source.
         *
         * @param source source
         * @return this builder
         */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * Set the page sources.
         *
         * @param sources sources
         * @return this builder
         */
        public Builder sources(List<String> sources) {
            if (sources != null) {
                this.sources.addAll(sources);
            }
            return this;
        }

        /**
         * Set the glyph.
         *
         * @param type  glyph type
         * @param value glyph value
         * @return this builder
         */
        public Builder glyph(String type, String value) {
            this.glyph = Glyph.builder()
                              .type(type)
                              .value(value)
                              .build();
            return this;
        }

        /**
         * Set the glyph.
         *
         * @param glyph glyph
         * @return this builder
         */
        public Builder glyph(Glyph glyph) {
            this.glyph = glyph;
            return this;
        }

        /**
         * Set the glyph.
         *
         * @param supplier glyph supplier
         * @return this builder
         */
        public Builder glyph(Supplier<Glyph> supplier) {
            if (supplier != null) {
                this.glyph = supplier.get();
            }
            return this;
        }

        /**
         * Set the "to" value.
         *
         * @param to to
         * @return this builder
         */
        public Builder to(String to) {
            this.to = to;
            return this;
        }

        /**
         * Set the href value.
         *
         * @param href href
         * @return this builder
         */
        public Builder href(String href) {
            this.href = href;
            return this;
        }

        /**
         * Set the href target.
         *
         * @param target target
         * @return this builder
         */
        public Builder target(String target) {
            if (target != null) {
                this.target = target;
            }
            return this;
        }

        /**
         * Set the dir.
         *
         * @param dir dir
         * @return this builder
         */
        public Builder dir(String dir) {
            if (dir != null && !dir.isEmpty()) {
                this.dir = dir;
            }
            return this;
        }

        /**
         * Set the pathprefix.
         *
         * @param pathprefix pathprefix
         * @return this builder
         */
        public Builder pathprefix(String pathprefix) {
            this.pathprefix = pathprefix;
            return this;
        }

        /**
         * Add an item.
         *
         * @param builder item
         * @return this builder
         */
        public Builder item(Nav.Builder builder) {
            if (builder != null) {
                maxDepth(builder.maxDepth);
                this.items.add(builder.build());
            }
            return this;
        }

        /**
         * Add an item.
         *
         * @param type     type
         * @param consumer item builder block
         * @return this builder
         */
        public Builder item(Type type, Consumer<Builder> consumer) {
            Builder builder = builder(this).type(type);
            consumer.accept(builder);
            return item(builder);
        }

        /**
         * Apply the specified configuration.
         *
         * @param config config
         * @return this builder
         */
        public Builder config(Config config) {
            Deque<Builder> builders = new ArrayDeque<>();
            Deque<Config> stack = new ArrayDeque<>();
            applyConfig(config);
            builders.push(this);
            stack.push(config);
            Builder parentBuilder = null;
            while (!stack.isEmpty() && !builders.isEmpty()) {
                Config node = stack.peek();
                Builder builder = builders.peek();
                Type nodeType = Type.create(node.get("type"));
                if (nodeType != null
                        && nodeType.is(Type.ROOT, Type.GROUPS, Type.GROUP, Type.MENU)
                        && builder != parentBuilder) {
                    // tree node
                    List<Config> items = node.get("items").asNodeList().orElseGet(List::of);
                    if (!items.isEmpty()) {
                        ListIterator<Config> it = items.listIterator(items.size());
                        while (it.hasPrevious()) {
                            Config item = it.previous();
                            stack.push(item);
                            builders.push(new Builder(builder).applyConfig(item));
                        }
                        // 1st tree-node pass
                        continue;
                    }
                }
                // leaf-node, or 2nd tree-node pass
                if (builder.parent != null) {
                    builder.parent.item(builder);
                    builder.parent.maxDepth = builder.maxDepth;
                    parentBuilder = builder.parent;
                    builders.pop();
                }
                stack.pop();
            }
            return this;
        }

        private Builder applyConfig(Config config) {
            type = Type.create(config.get("type"));
            title = config.get("title").asString().orElse(title);
            glyph = config.get("glyph").asOptional().map(Glyph::create).orElse(glyph);
            source = config.get("source").asString().orElse(source);
            sources.addAll(config.get("sources").asList().orElse(List.of()));
            href = config.get("href").asString().orElse(href);
            target = config.get("target").asString().orElse(target);
            dir = config.get("dir").asString().orElse(dir);
            pathprefix = config.get("pathprefix").asString().orElse(pathprefix);
            type = config.get("type").asString().map(s -> Type.valueOf(s.toUpperCase())).orElse(type);
            super.config(config);
            return this;
        }

        private void require(boolean condition, String name) {
            if (!condition) {
                throw new IllegalArgumentException(String.format(
                        "A navigation node of type %s requires '%s'", type, name));
            }
        }

        private void requireNot(boolean condition, String name) {
            if (!condition) {
                throw new IllegalArgumentException(String.format(
                        "A navigation node of type %s cannot use '%s'", type, name));
            }
        }

        private static boolean arrayContains(int value, int... values) {
            for (int v : values) {
                if (value == v) {
                    return true;
                }
            }
            return false;
        }

        private void require(boolean condition, String name, int value, String expected) {
            if (!condition) {
                throw new IllegalArgumentException(String.format(
                        "A navigation node of type %s cannot have %s=%d, expected %s",
                        type, name, value, expected));
            }
        }

        private void requireParent(Type... types) {
            if (parent == null || parent.type == null || !parent.type.is(types)) {
                throw new IllegalArgumentException(String.format(
                        "%s nav node must have a parent of type %s, got %s",
                        type, Type.names(types), parent != null ? parent.type : null));
            }
        }

        private void validate() {
            if (type == null) {
                throw new IllegalArgumentException("Missing navigation node type: " + Type.names());
            }
            switch (type) {
                case ROOT:
                    if (parent != null) {
                        throw new IllegalArgumentException("ROOT nav node cannot have a parent");
                    }
                    require(title != null, "title");
                    requireNot(href == null, "href");
                    requireNot(DEFAULT_TARGET.equals(target), "target");
                    requireNot(pathprefix == null, "pathprefix");
                    requireNot(source == null, "pathprefix");
                    requireNot(sources.isEmpty(), "sources");
                    requireNot(includes().isEmpty(), "includes");
                    requireNot(excludes().isEmpty(), "excludes");
                    break;
                case GROUPS:
                    requireParent(Type.ROOT);
                    requireNot(title == null, "title");
                    requireNot(href == null, "href");
                    requireNot(DEFAULT_TARGET.equals(target), "target");
                    requireNot(pathprefix == null, "pathprefix");
                    requireNot(source == null, "pathprefix");
                    requireNot(sources.isEmpty(), "sources");
                    requireNot(includes().isEmpty(), "includes");
                    requireNot(excludes().isEmpty(), "excludes");
                    require(depth == 1, "depth", depth, "1");
                    require(maxDepth <= 4, "max depth", maxDepth, "[1, 4]");
                    break;
                case GROUP:
                    requireParent(Type.GROUPS);
                    require(title != null, "title");
                    requireNot(href == null, "href");
                    requireNot(DEFAULT_TARGET.equals(target), "target");
                    requireNot(source == null, "pathprefix");
                    requireNot(sources.isEmpty(), "sources");
                    requireNot(includes().isEmpty(), "includes");
                    requireNot(excludes().isEmpty(), "excludes");
                    require(depth == 2, "depth", depth, "2");
                    require(maxDepth > 1 && maxDepth <= 4, "max depth", maxDepth, "[2, 4]");
                    break;
                case MENU:
                    requireParent(Type.GROUP, Type.ROOT);
                    require(title != null, "title");
                    requireNot(href == null, "href");
                    requireNot(DEFAULT_TARGET.equals(target), "target");
                    requireNot(source == null, "pathprefix");
                    require(depth == 1 || depth == 3, "depth", depth, "1|3");
                    require(maxDepth <= 4, "max depth", maxDepth, "[1, 4]");
                    break;
                case PAGE:
                    requireParent(Type.GROUP, Type.MENU, Type.ROOT);
                    require(source != null, "source");
                    requireNot(pathprefix == null, "pathprefix");
                    requireNot(href == null, "href");
                    requireNot(DEFAULT_TARGET.equals(target), "target");
                    requireNot(sources.isEmpty(), "sources");
                    requireNot(includes().isEmpty(), "includes");
                    requireNot(excludes().isEmpty(), "excludes");
                    requireNot(items.isEmpty(), "items");
                    break;
                case LINK:
                    requireParent(Type.GROUP, Type.MENU, Type.ROOT);
                    require(title != null, "title");
                    require(href != null, "href");
                    requireNot(DEFAULT_TARGET.equals(target), "target");
                    requireNot(pathprefix == null, "pathprefix");
                    requireNot(source == null, "pathprefix");
                    requireNot(sources.isEmpty(), "sources");
                    requireNot(includes().isEmpty(), "includes");
                    requireNot(excludes().isEmpty(), "excludes");
                    requireNot(items.isEmpty(), "items");
                    break;
                case HEADER:
                    requireParent(Type.ROOT);
                    require(title != null, "title");
                    requireNot(href == null, "href");
                    requireNot(DEFAULT_TARGET.equals(target), "target");
                    requireNot(pathprefix == null, "pathprefix");
                    requireNot(source == null, "pathprefix");
                    requireNot(sources.isEmpty(), "sources");
                    requireNot(includes().isEmpty(), "includes");
                    requireNot(excludes().isEmpty(), "excludes");
                    requireNot(items.isEmpty(), "items");
                    break;
                default:
            }
        }

        /**
         * Build this instance.
         *
         * @return new instance
         */
        public Nav build() {
            dir = effectiveDir();
            if (type == Type.GROUP || type == Type.MENU) {
                if (pathprefix == null) {
                    if (dir != null) {
                        pathprefix = "/" + dir;
                    } else if (parent != null) {
                        pathprefix = parent.pathprefix;
                    }
                }
            }
            if (source != null) {
                source = new SourcePath(dir, source).asString(false);
            }
            validate();
            return new Nav(this);
        }
    }
}
