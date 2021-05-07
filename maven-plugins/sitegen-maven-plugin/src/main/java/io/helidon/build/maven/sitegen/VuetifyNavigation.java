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

package io.helidon.build.maven.sitegen;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.config.Config;

import static io.helidon.build.maven.sitegen.Helper.checkNonNull;
import static io.helidon.build.maven.sitegen.Helper.checkNonNullNonEmpty;

/**
 * Configuration and mapping for vuetify navigation.
 */
public class VuetifyNavigation implements Model {

    private static final String GLYPH_PROP = "glyph";
    private static final String TITLE_PROP = "title";
    private static final String ITEMS_PROP = "items";
    private static final String PATHPREFIX_PROP = "pathprefix";
    private static final String HREF_PROP = "href";
    private static final String INCLUDES_PROP = "includes";
    private static final String EXCLUDES_PROP = "excludes";

    private final List<Item> items;
    private final Glyph glyph;
    private final String title;

    private VuetifyNavigation(String title, Glyph glyph, List<Item> items) {
        checkNonNullNonEmpty(title, "title");
        checkNonNull(items, "items");
        this.glyph = glyph;
        this.items = items;
        this.title = title;
    }

    /**
     * Get the top level navigation items.
     * @return {@code List<Item>}, never {@code null}
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Get the main navigation glyph.
     * @return the {@link Glyph} instance if set, {@code null} otherwise
     */
    public Glyph getGlyph() {
        return glyph;
    }

    /**
     * Get the main navigation title.
     * @return the title, never {@code null}
     */
    public String getTitle() {
        return title;
    }

    /**
     * Resolve the navigation against the given pages.
     *
     * @param allPages the pages to use for resolving the navigation
     * @return a new "resolved" instance of {@link VuetifyNavigation}
     */
    VuetifyNavigation resolve(Collection<Page> allPages) {
        Builder navigationBuilder = VuetifyNavigation.builder();
        navigationBuilder.title(title);
        if (glyph != null) {
            navigationBuilder.glyph(glyph);
        }
        List<Item> resolvedGroups = new LinkedList<>();
        for (Item group : items) {
            if (!(group instanceof Group)) {
                throw new IllegalStateException("top level items is not a group");
            }
            resolvedGroups.add(((Group) group).resolve(allPages));
        }
        navigationBuilder.items(resolvedGroups);
        return navigationBuilder.build();
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case (TITLE_PROP):
                return title;
            case (GLYPH_PROP):
                return glyph;
            case (ITEMS_PROP):
                return items;
            default:
                throw new IllegalArgumentException("Unknown attribute: " + attr);
        }
    }

    /**
     * A fluent builder to create {@link VuetifyNavigation} instances.
     */
    public static class Builder extends AbstractBuilder<VuetifyNavigation> {

        /**
         * Set the main navigation title.
         * @param title the title to use
         * @return the {@link Builder} instance
         */
        public Builder title(String title) {
            put(TITLE_PROP, title);
            return this;
        }

        /**
         * Set the main navigation glyph.
         * @param glyph the glyph to use
         * @return the {@link Builder} instance
         */
        public Builder glyph(Glyph glyph) {
            put(GLYPH_PROP, glyph);
            return this;
        }

        /**
         * Set the top level navigation items.
         * @param items the items to use
         * @return the {@link Builder} instance
         */
        public Builder items(List<Item> items) {
            put(ITEMS_PROP, items);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
        public Builder config(Config node) {
            node.get(TITLE_PROP)
                    .asString()
                    .ifPresent(it -> put(TITLE_PROP, it));

            node.get(GLYPH_PROP)
                    .ifExists(it -> put(GLYPH_PROP, Glyph.builder().config(it).build()));

            node.get(ITEMS_PROP)
                    .asNodeList()
                    .ifPresent(it -> put(ITEMS_PROP, it.stream()
                            .map(n -> Item.from(n, true))
                            .collect(Collectors.toList())));

            return this;
        }

        @Override
        public VuetifyNavigation build() {
            String title = null;
            Glyph glyph = null;
            List<Item> items = null;
            for (Map.Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case(TITLE_PROP):
                        title = asType(val, String.class);
                        break;
                    case(GLYPH_PROP):
                        glyph = asType(val, Glyph.class);
                        break;
                    case(ITEMS_PROP):
                        items = asList(val, Item.class);
                        break;
                    default:
                        throw new IllegalStateException("Unknown attribute: " + attr);
                }
            }
            return new VuetifyNavigation(title, glyph, items);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A base class for all types of navigation items.
     */
    public abstract static class Item implements Model {

        private final String title;
        private final Glyph glyph;

        /**
         * Create a new instance of {@link Item}.
         *
         * @param title the item title
         * @param glyph the item glyph
         */
        protected Item(String title, Glyph glyph) {
            this.glyph = glyph;
            this.title = title;
        }

        /**
         * Get the item title.
         * @return the title, may be {@code null} depending on sub-class behavior
         */
        public String getTitle() {
            return title;
        }

        /**
         * Get the item glyph.
         * @return the glyph, may be {@code null} depending on sub-class behavior
         */
        public Glyph getGlyph() {
            return glyph;
        }

        /**
         * Test if the item is an instance of {@link Group}.
         * @return true if the item is {@link Group}, false otherwise
         */
        public boolean isGroup() {
            return this instanceof Group;
        }

        /**
         * Convert this item to a {@link Group}.
         * @throws IllegalStateException of the is not a an instance of {@link Group}
         * @return the item as a {@link Group}
         */
        public Group asGroup() throws IllegalStateException {
            if (!isGroup()) {
                throw new IllegalStateException("not a group: " + this);
            }
            return (Group) this;
        }

        /**
         * Test if the item is an instance of {@link SubGroup}.
         * @return true if the item is {@link SubGroup}, false otherwise
         */
        public boolean isSubGroup() {
            return this instanceof SubGroup;
        }

        /**
         * Convert this item to a {@link SubGroup}.
         * @throws IllegalStateException of the is not a an instance of {@link SubGroup}
         * @return the item as a {@link SubGroup}
         */
        public SubGroup asSubGroup() {
            if (!isGroup()) {
                throw new IllegalStateException("not a subgroup: " + this);
            }
            return (SubGroup) this;
        }

        /**
         * Test if the item is an instance of {@link Link}.
         * @return true if the item is {@link Link}, false otherwise
         */
        public boolean isLink() {
            return this instanceof Link;
        }

        /**
         * Convert this item to a {@link Link}.
         * @throws IllegalStateException of the is not a an instance of {@link Link}
         * @return the item as a {@link Link}
         */
        public Link asLink() {
            if (!isLink()) {
                throw new IllegalStateException("not a link: " + this);
            }
            return (Link) this;
        }

        @Override
        public Object get(String attr) {
            switch (attr) {
                case (TITLE_PROP):
                    return title;
                case (GLYPH_PROP):
                    return glyph;
                case ("isgroup"):
                    return isGroup();
                case ("islink"):
                    return isLink();
                default:
                    throw new IllegalArgumentException("Unknown attribute: " + attr);
            }
        }

        private static Item from(Config node, boolean topLevel) {
            if (node.get(ITEMS_PROP).exists()) {
                if (topLevel) {
                    return Group.builder().config(node).build();
                } else {
                    return SubGroup.builder().config(node).build();
                }
            } else if (node.get(HREF_PROP).exists()) {
                return Link.builder().config(node).build();
            } else if (node.get(INCLUDES_PROP).exists()
                    || node.get(EXCLUDES_PROP).exists()) {
                return new Pages(SourcePathFilter.builder()
                                .config(node)
                                .build());
            }
            throw new IllegalArgumentException("Unknown navigation item type");
        }

        /**
         * A base fluent builder for {@link Item} sub-classes.
         * @param <T> the sub-class type
         */
        protected abstract static class Builder<T extends Item> extends AbstractBuilder<T> {

            /**
             * Set the item title.
             * @param title the title to use
             * @return the {@link Builder} instance
             */
            public Builder<T> title(String title) {
                put(TITLE_PROP, title);
                return this;
            }

            /**
             * Set the item glyph.
             * @param glyph the glyph to use
             * @return the {@link Builder} instance
             */
            public Builder<T> glyph(Glyph glyph) {
                put(GLYPH_PROP, glyph);
                return this;
            }

            /**
             * Apply the configuration represented by the given {@link Config} node.
             * @param node a {@link Config} node containing configuration values to apply
             * @return the {@link Builder} instance
             */
            public Builder<T> config(Config node) {
                node.get(TITLE_PROP)
                        .asString()
                        .ifPresent(it -> put(TITLE_PROP, it));

                node.get(GLYPH_PROP)
                        .ifExists(c -> put(GLYPH_PROP, Glyph.builder()
                                .config(c)
                                .build()));

                return this;
            }
        }
    }

    /**
     * Special item type used to match {@link Page} instances.
     *
     * The {@link #resolve(java.util.Collection)} method is designed to replace
     * matched pages with {@link Link} instances.
     */
    public static class Pages extends Item {

        private final SourcePathFilter pages;

        private Pages(SourcePathFilter pages) {
            super(null, null);
            this.pages = pages;
        }

        private List<Item> resolve(Collection<Page> allPages) {
            return Page.filter(allPages, pages.getIncludes(), pages.getExcludes())
                    .stream()
                    .map(page -> Link.builder()
                        .href(page.getTargetPath())
                        .title(page.getMetadata().getTitle())
                        .build())
                    .collect(Collectors.toList());
        }

        /**
         * A fluent builder to create {@link Pages} instances.
         */
        public static class Builder extends Item.Builder<Pages> {

            /**
             * Set the includes pattern.
             * @param includes the includes pattern to use
             * @return the {@link Builder} instance
             */
            public Builder includes(List<String> includes) {
                put(INCLUDES_PROP, includes);
                return this;
            }

            /**
             * Set the excludes pattern.
             *
             * @param excludes the excludes pattern to use
             * @return the {@link Builder} instance
             */
            public Builder excludes(List<String> excludes) {
                put(EXCLUDES_PROP, excludes);
                return this;
            }

            @Override
            public Builder config(Config node) {
                node.get(INCLUDES_PROP)
                        .asList(String.class)
                        .ifPresent(it -> put(INCLUDES_PROP, it));

                node.get(EXCLUDES_PROP)
                        .asList(String.class)
                        .ifPresent(it -> put(EXCLUDES_PROP, it));

                return this;
            }

            @Override
            public Pages build() {
                List<String> includes = null;
                List<String> excludes = null;
                for (Entry<String, Object> entry : values()) {
                    String attr = entry.getKey();
                    Object val = entry.getValue();
                    switch (attr) {
                        case(INCLUDES_PROP):
                            includes = asList(val, String.class);
                            break;
                        case(EXCLUDES_PROP):
                            excludes = asList(val, String.class);
                            break;
                        default:
                            throw new IllegalStateException("Unknown attribute: " + attr);
                    }
                }
                return new Pages(SourcePathFilter.builder()
                        .includes(includes)
                        .excludes(excludes)
                        .build());
            }
        }

        /**
         * Create a new {@link Builder} instance.
         * @return the created builder
         */
        public static Builder builder() {
            return new Builder();
        }
    }

    /**
     * A top level navigation group.
     */
    public static class Group extends Item {

        private final String pathprefix;
        private final List<Item> items;

        /**
         * Create a new instance of {@link Group}.
         * @param title the group title
         * @param glyph the group glyph
         * @param pathprefix the group path prefix
         * @param items the group items
         */
        protected Group(String title, Glyph glyph, String pathprefix, List<Item> items) {
            super(title, glyph);
            checkNonNull(items, ITEMS_PROP);
            this.pathprefix = pathprefix;
            this.items = items;
        }

        /**
         * Get the path prefix associated with this group.
         * @return the path prefix, may be {@code null}
         */
        public String getPathprefix() {
            return pathprefix;
        }

        /**
         * Get the group items.
         * @return {@code List<Item>}, never {@code null}
         */
        public List<Item> getItems() {
            return items;
        }

        private Group resolve(Collection<Page> allPages) {
            Group.Builder groupBuilder = Group.builder();
            groupBuilder.title(getTitle());
            if (getGlyph() != null) {
                groupBuilder.glyph(getGlyph());
            }
            if (pathprefix != null) {
                groupBuilder.pathprefix(pathprefix);
            }
            groupBuilder.items(items.stream().flatMap(item -> {
                if (item instanceof Pages) {
                    return ((Pages) item).resolve(allPages).stream();
                }
                if (item instanceof SubGroup) {
                    return Stream.of(((SubGroup) item).resolve(allPages));
                }
                return Stream.of(item);
            }).collect(Collectors.toList()));
            return groupBuilder.build();
        }

        @Override
        public Object get(String attr) {
            switch (attr) {
                case (ITEMS_PROP):
                    return items;
                case (PATHPREFIX_PROP):
                    return pathprefix;
                default:
                    return super.get(attr);
            }
        }

        /**
         * A fluent builder to create {@link Link} instances.
         */
        public static class Builder extends Item.Builder<Group> {

            /**
             * Set the group items.
             * @param items the items to set
             * @return the {@link Builder} instance
             */
            public Builder items(List<Item> items) {
                put(ITEMS_PROP, items);
                return this;
            }

            /**
             * Set the path prefix.
             * @param pathprefix the path prefix to use
             * @return the {@link Builder} instance
             */
            public Group.Builder pathprefix(String pathprefix) {
                put(PATHPREFIX_PROP, pathprefix);
                return this;
            }

            @Override
            public Builder config(Config node) {
                super.config(node);

                node.get(ITEMS_PROP)
                        .asNodeList()
                        .ifPresent(it -> put(ITEMS_PROP, it.stream()
                                .map(n -> Item.from(n, false))
                                .collect(Collectors.toList())));

                node.get(PATHPREFIX_PROP).asString().ifPresent(it -> put(PATHPREFIX_PROP, it));

                return this;
            }

            @Override
            public Group build() {
                String title = null;
                Glyph glyph = null;
                String pathprefix = null;
                List<Item> items = null;
                for (Map.Entry<String, Object> entry : values()) {
                    String attr = entry.getKey();
                    Object val = entry.getValue();
                    switch (attr) {
                        case (TITLE_PROP):
                            title = asType(val, String.class);
                            break;
                        case (GLYPH_PROP):
                            glyph = asType(val, Glyph.class);
                            break;
                        case (ITEMS_PROP):
                            items = asList(val, Item.class);
                            break;
                        case (PATHPREFIX_PROP):
                            pathprefix = asType(val, String.class);
                            break;
                        default:
                            throw new IllegalStateException("Unknown attribute: " + attr);
                    }
                }
                return new Group(title, glyph, pathprefix, items);
            }
        }

        /**
         * Create a new {@link Builder} instance.
         * @return the created builder
         */
        public static Builder builder() {
            return new Builder();
        }
    }

    /**
     * Represents a navigation sub-group (second level).
     */
    public static class SubGroup extends Group {

        private SubGroup(String title, Glyph glyph, String pathprefix, List<Item> items) {
            super(title, glyph, pathprefix, items);
            checkNonNullNonEmpty(pathprefix, PATHPREFIX_PROP);
        }

        private SubGroup resolve(Collection<Page> allPages) {
            SubGroup.Builder subGroupBuilder = SubGroup.builder();
            subGroupBuilder.title(getTitle());
            if (getGlyph() != null) {
                subGroupBuilder.glyph(getGlyph());
            }
            subGroupBuilder.pathprefix(getPathprefix());
            subGroupBuilder.items(getItems().stream().flatMap(item -> {
                if (item instanceof Pages) {
                    return ((Pages) item).resolve(allPages).stream();
                }
                return Stream.of(item);
            }).collect(Collectors.toList()));
            return subGroupBuilder.build();
        }

        /**
         * A fluent builder to create {@link SubGroup} instances.
         */
        public static class Builder extends Group.Builder {

            @Override
            public SubGroup build() {
                String title = null;
                Glyph glyph = null;
                String pathprefix = null;
                List<Item> items = null;
                for (Map.Entry<String, Object> entry : values()) {
                    String attr = entry.getKey();
                    Object val = entry.getValue();
                    switch (attr) {
                        case (TITLE_PROP):
                            title = asType(val, String.class);
                            break;
                        case (GLYPH_PROP):
                            glyph = asType(val, Glyph.class);
                            break;
                        case (PATHPREFIX_PROP):
                            pathprefix = asType(val, String.class);
                            break;
                        case (ITEMS_PROP):
                            items = asList(val, Item.class);
                            break;
                        default:
                            throw new IllegalStateException("Unknown attribute: " + attr);
                    }
                }
                return new SubGroup(title, glyph, pathprefix, items);
            }
        }

        /**
         * Create a new {@link Builder} instance.
         * @return the created builder
         */
        public static Builder builder() {
            return new Builder();
        }
    }

    /**
     * Represents a navigation link, either to a {@link Page} or to a URL.
     */
    public static class Link extends Item {

        private final String href;

        private Link(String title, Glyph glyph, String href) {
            super(title, glyph);
            checkNonNullNonEmpty(href, "href");
            this.href = href;
        }

        public String getHref() {
            return href;
        }

        @Override
        public Object get(String attr) {
            switch (attr) {
                case (HREF_PROP):
                    return href;
                default:
                    return super.get(attr);
            }
        }

        /**
         * A fluent builder to create {@link Link} instances.
         */
        public static class Builder extends Item.Builder<Link> {

            /**
             * Set the link value.
             * @param href the link value to use
             * @return the {@link Builder} instance
             */
            public Builder href(String href) {
                put(HREF_PROP, href);
                return this;
            }

            @Override
            public Builder config(Config node) {
                super.config(node);

                node.get(HREF_PROP)
                        .asString()
                        .ifPresent(it -> put(HREF_PROP, it));

                return this;
            }

            @Override
            public Link build() {
                String title = null;
                Glyph glyph = null;
                String href = null;
                for (Map.Entry<String, Object> entry : values()) {
                    String attr = entry.getKey();
                    Object val = entry.getValue();
                    switch (attr) {
                        case (TITLE_PROP):
                            title = asType(val, String.class);
                            break;
                        case (GLYPH_PROP):
                            glyph = asType(val, Glyph.class);
                            break;
                        case (HREF_PROP):
                            href = asType(val, String.class);
                            break;
                        default:
                            throw new IllegalStateException("Unknown attribute: " + attr);
                    }
                }
                return new Link(title, glyph, href);
            }
        }

        /**
         * Create a new {@link Builder} instance.
         *
         * @return the created builder
         */
        public static Builder builder() {
            return new Builder();
        }
    }
}
