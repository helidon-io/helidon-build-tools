/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.sitegen.freemarker;

import java.util.Map;
import java.util.Objects;

import io.helidon.sitegen.Model;
import io.helidon.sitegen.Page;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.asciidoctor.ast.PhraseNode;

/**
 * Helper class to be used within templates.
 */
public class Helper implements TemplateHashModel {

    private final ObjectWrapper objectWrapper;

    /**
     * Create a new instance.
     * @param objectWrapper
     */
    Helper(ObjectWrapper objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    /**
     * Create a new link helper.
     * @param node the node representing the link
     * @return the link helper or {@code null} if the provided node is
     * {@code null}
     */
    @SuppressWarnings("unchecked")
    public Link link(PhraseNode node){
        if (node == null) {
            return null;
        }
        Map<String, Object> docAttrs = node.getDocument().getAttributes();
        Map<String, Object> nodeAttrs = node.getAttributes();
        return new Link((Map<String, Page>) docAttrs.get("pages"),
                (Page) docAttrs.get("page"), node.getType(),
                (String) nodeAttrs.get("path"),
                (String) nodeAttrs.get("refid"),
                (String) nodeAttrs.get("fragment"),
                node.getTarget(),
                (String) nodeAttrs.get("title"),
                node.getText(), node.getId(),
                (String) nodeAttrs.get("window"));
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
       // return method model if method name found for key
        if (SimpleMethodModel.hasMethodWithName(this, key)) {
            return new SimpleMethodModel(objectWrapper, this, key);
        }
        return null;
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    /**
     * Link helper.
     */
    public static class Link implements Model {

        private static final String XREF_ANCHOR_SELF_TYPE = "xref_anchor_self";
        private static final String XREF_ANCHOR_TYPE = "xref_anchor";
        private static final String UNKNOWN_TYPE = "?";
        private static final String DEFAULT_WINDOW = "_blank";
        private static final String SOURCE_PROP = "source";
        private static final String TARGET_PROP = "target";
        private static final String HASH_PROP = "hash";
        private static final String TITLE_PROP = "title";
        private static final String TEXT_PROP = "text";
        private static final String WINDOW_PROP = "window";
        private static final String ID_PROP = "id";
        private static final String TYPE_PROP = "type";

        private final String source;
        private final String target;
        private final String hash;
        private final String title;
        private final String text;
        private final String window;
        private final String id;
        private final String type;

        /**
         * Create a new instance.
         * @param pages all pages
         * @param page the page that contains the link
         * @param type the initial type for the link
         * @param path the link path
         * @param refid the link refId
         * @param fragment the link fragment
         * @param target the link target
         * @param title the link title
         * @param text the link text
         * @param id the link id
         * @param window the link window
         */
        @SuppressWarnings("checkstyle:ParameterNumber")
        public Link(Map<String, Page> pages, Page page, String type, String path,
                String refid, String fragment, String target, String title,
                String text, String id, String window) {

            Objects.requireNonNull(pages, "pages is null");
            Objects.requireNonNull(pages, "page is null");
            Objects.requireNonNull(type, "type is null");
            switch (type) {
                case("xref"):
                    if (path != null) {
                        this.source = path.replace(".html", ".adoc");
                    } else {
                        this.source = refid;
                    }
                    if (pages.containsKey("/" + source)) {
                        this.target = pages.get("/" + source).getTargetPath();
                    } else {
                        this.target = "";
                    }
                    this.hash = fragment;
                    if ((hash != null && (this.target == null || this.target.isEmpty()))
                            || page.getTargetPath().equals(this.target)){
                        this.type = XREF_ANCHOR_SELF_TYPE;
                    } else if (hash != null
                            && target != null
                            && !hash.equals(source)) {
                       this.type = XREF_ANCHOR_TYPE;
                    } else {
                        this.type = type;
                    }
                    break;
                case ("ref"):
                    this.hash = null;
                    this.source = null;
                    this.target = path;
                    this.type = type;
                    break;
                case ("bibref"):
                    this.hash = null;
                    this.source = null;
                    this.target = path;
                    this.type = type;
                    break;
                default:
                    this.type = UNKNOWN_TYPE;
                    this.hash = null;
                    this.source = null;
                    this.target = target;
            }
            this.id = id == null ? "" : id;
            this.text = text == null ? "" : text;
            this.title = title == null ? "" : title;
            this.window = window == null ? DEFAULT_WINDOW : window;
        }

        /**
         * The path of the source document for the link target.
         * @return source path or {@code null} if the source document is unknown
         */
        public String getSource() {
            return source;
        }

        /**
         * The link target.
         * @return link target or {@code null} if the link is just an anchor
         */
        public String getTarget() {
            return target;
        }

        /**
         * The link text.
         * @return link text, never {@code null}
         */
        public String getText() {
            return text;
        }

        /**
         * The title for the link.
         * @return link title, never {@code null}
         */
        public String getTitle() {
            return title;
        }

        /**
         * The type of the link.
         * @return link target, never {@code null}
         */
        public String getType() {
            return type;
        }

        /**
         * The link id.
         * @return link id, never {@code null}
         */
        public String getId() {
            return id;
        }

        /**
         * The link window target.
         * @return link window
         */
        public String getWindow() {
            return window;
        }

        /**
         * The link hash (fragment).
         * @return link hash, can be {@code null}
         */
        public String getHash() {
            return hash;
        }

        @Override
        public Object get(String attr) {
            switch (attr) {
                case (SOURCE_PROP):
                    return source;
                case (TARGET_PROP):
                    return target;
                case (TEXT_PROP):
                    return text;
                case (HASH_PROP):
                    return hash;
                case (TITLE_PROP):
                    return title;
                case (TYPE_PROP):
                    return type;
                case (ID_PROP):
                    return id;
                case (WINDOW_PROP):
                    return window;
                default:
                    throw new IllegalArgumentException(
                            "Unkown attribute: " + attr);
            }
        }
    }
}
