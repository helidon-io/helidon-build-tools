/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;

import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.sitegen.Context;
import io.helidon.build.maven.sitegen.Model;
import io.helidon.build.maven.sitegen.RenderingException;

import static java.util.Objects.requireNonNull;

/**
 * Link helper.
 */
@SuppressWarnings("unused")
public final class Link implements Model {

    private final String source;
    private final String target;
    private final String hash;
    private final String title;
    private final String text;
    private final String window;
    private final String id;
    private final String type;
    private final String role;
    private final String rel;

    private Link(Builder builder) {
        Page doc = requireNonNull(builder.page, "page is null");
        requireNonNull(builder.type, "type is null");
        switch (builder.type) {
            case ("xref"):
                if (builder.path != null) {
                    Context ctx = Context.get();
                    String backend = ctx.site().backend().name();
                    source = builder.path.replace("." + backend, ".adoc");
                    Page page = ctx.resolvePage(doc, source);
                    if (page == null) {
                        boolean strict = ctx.strictXRef();
                        String msg = String.format("Unresolved cross-reference: %s, document: %s", source, doc.source());
                        if (strict) {
                            throw new RenderingException(msg);
                        }
                        Log.warn(msg);
                        page = Page.UNRESOLVED;
                    }
                    target = page.target();
                } else {
                    source = builder.refId;
                    target = null;
                }
                hash = builder.fragment;
                if (hash != null && (target == null || builder.page.target().equals(target))) {
                    type = "xref_anchor_self";
                } else if (hash != null && !hash.equals(source)) {
                    type = "xref_anchor";
                } else {
                    type = builder.type;
                }
                break;
            case ("ref"):
            case ("bibref"):
                hash = null;
                source = null;
                target = builder.path;
                type = builder.type;
                break;
            default:
                type = "?";
                hash = null;
                source = null;
                target = builder.target;
        }
        if (!builder.options.isEmpty()) {
            this.rel = String.join(" ", builder.options);
        } else {
            this.rel = null;
        }
        this.role = builder.role;
        this.id = builder.id;
        this.text = builder.text;
        this.title = builder.title;
        this.window = builder.window;
    }

    /**
     * Get the rel attribute.
     *
     * @return rel
     */
    public String rel() {
        return rel;
    }

    /**
     * Get the role attribute.
     *
     * @return role
     */
    public String role() {
        return role;
    }

    /**
     * The path of the source document for the link target.
     *
     * @return source path or {@code null} if the source document is unknown
     */
    public String source() {
        return source;
    }

    /**
     * The link target.
     *
     * @return link target or {@code null} if the link is just an anchor
     */
    public String target() {
        return target;
    }

    /**
     * The link text.
     *
     * @return link text, never {@code null}
     */
    public String text() {
        return text;
    }

    /**
     * The title for the link.
     *
     * @return link title, never {@code null}
     */
    public String title() {
        return title;
    }

    /**
     * The type of the link.
     *
     * @return link target, never {@code null}
     */
    public String type() {
        return type;
    }

    /**
     * The link id.
     *
     * @return link id, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * The link window target.
     *
     * @return link window
     */
    public String window() {
        return window;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case "source":
                return source;
            case "target":
                return target;
            case "text":
                return text;
            case "hash":
                return hash;
            case "title":
                return title;
            case "type":
                return type;
            case "id":
                return id;
            case "window":
                return window;
            case "rel":
                return rel;
            case "role":
                return role;
            default:
                throw new IllegalArgumentException("Unknown attribute: " + attr);
        }
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of {@link Link}.
     */
    public static final class Builder {

        private Page page;
        private String type;
        private String path;
        private String refId;
        private String fragment;
        private String target;
        private String title;
        private String text;
        private String id;
        private String window = "_blank";
        private String role;
        private final List<String> options = new ArrayList<>();

        private Builder() {
        }

        /**
         * Set the page.
         *
         * @param page page
         * @return this builder
         */
        public Builder page(Page page) {
            this.page = page;
            return this;
        }

        /**
         * Add options.
         *
         * @param options options
         * @return this builder
         */
        public Builder options(List<String> options) {
            if (options != null) {
                this.options.addAll(options);
            }
            return this;
        }

        /**
         * Set the role.
         *
         * @param role role
         * @return this builder
         */
        public Builder role(String role) {
            this.role = role;
            return this;
        }

        /**
         * Set the type.
         *
         * @param type type
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Set the path.
         *
         * @param path path
         * @return this builder
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Set the refId.
         *
         * @param refId refId
         * @return this builder
         */
        public Builder refId(String refId) {
            this.refId = refId;
            return this;
        }

        /**
         * Set the fragment.
         *
         * @param fragment fragment
         * @return this builder
         */
        public Builder fragment(String fragment) {
            this.fragment = fragment;
            return this;
        }

        /**
         * Set the target.
         *
         * @param target target
         * @return this builder
         */
        public Builder target(String target) {
            this.target = target;
            return this;
        }

        /**
         * Set the title.
         *
         * @param title title
         * @return this builder
         */
        public Builder title(String title) {
            if (title != null) {
                this.title = title;
            }
            return this;
        }

        /**
         * Set the text.
         *
         * @param text text
         * @return this builder
         */
        public Builder text(String text) {
            if (text != null) {
                this.text = text;
            }
            return this;
        }

        /**
         * Set the id.
         *
         * @param id id
         * @return this builder
         */
        public Builder id(String id) {
            if (id != null) {
                this.id = id;
            }
            return this;
        }

        /**
         * Set the window.
         *
         * @param window window
         * @return this builder
         */
        public Builder window(String window) {
            if (window != null) {
                this.window = window;
            }
            return this;
        }

        /**
         * Create a new instance.
         *
         * @return new instance
         */
        public Link build() {
            return new Link(this);
        }
    }
}
