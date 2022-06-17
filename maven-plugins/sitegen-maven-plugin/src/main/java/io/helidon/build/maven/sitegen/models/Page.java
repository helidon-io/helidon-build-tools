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

package io.helidon.build.maven.sitegen.models;

import io.helidon.build.common.SourcePath;
import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.Model;

import static io.helidon.build.common.Strings.requireValid;
import static java.util.Objects.requireNonNull;

/**
 * A page represents a document to be rendered.
 */
public class Page implements Model {

    /**
     * Constant for unresolved pages.
     */
    public static final Page UNRESOLVED = Page.builder()
                                              .source("unresolved.adoc")
                                              .target("unresolved")
                                              .metadata(Metadata.builder().title("Unresolved"))
                                              .build();

    private final String source;
    private final String target;
    private final Metadata metadata;

    private Page(Builder builder) {
        this.source = requireValid(builder.source, "source is invalid!");
        this.target = requireValid(builder.target, "target is invalid!");
        this.metadata = requireNonNull(builder.metadata, "metadata is null!");
    }

    /**
     * get the source path.
     *
     * @return source path.
     */
    public String source() {
        return source;
    }

    /**
     * Get the source path.
     *
     * @return source path
     */
    public SourcePath sourcePath() {
        return new SourcePath(source);
    }

    /**
     * Get the target path.
     *
     * @return target path.
     */
    public String target() {
        return target;
    }

    /**
     * Get the metadata.
     *
     * @return metadata
     */
    public Metadata metadata() {
        return metadata;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case "source":
                return source;
            case "target":
                return target;
            case "metadata":
                return metadata;
            default:
                throw new IllegalStateException("Unknown attribute: " + attr);
        }
    }

    @Override
    public String toString() {
        return "Page{"
                + "source='" + source + '\''
                + ", target='" + target + '\''
                + ", metadata=" + metadata
                + '}';
    }

    /**
     * Get the given path minus the file extension.
     *
     * @param path path
     * @return new path
     */
    public static String removeFileExt(String path) {
        return path.substring(0, path.lastIndexOf("."));
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
     * Builder of {@link Page}.
     */
    public static final class Builder {

        private String source;
        private String target;
        private Metadata metadata;

        /**
         * Set the source path.
         *
         * @param source source path
         * @return this builder
         */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * Set the target path.
         *
         * @param target target path
         * @return this builder
         */
        public Builder target(String target) {
            this.target = target;
            return this;
        }

        /**
         * Set the metadata.
         *
         * @param metadata metadata
         * @return this builder
         */
        public Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Set the metadata.
         *
         * @param builder metadata builder
         * @return this builder
         */
        public Builder metadata(Metadata.Builder builder) {
            this.metadata = builder.build();
            return this;
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public Page build() {
            return new Page(this);
        }
    }

    /**
     * Represents a {@link Page} metadata.
     */
    @SuppressWarnings("unused")
    public static class Metadata implements Model {

        private final String description;
        private final String keywords;
        private final String h1;
        private final String title;
        private final String h1Prefix;

        private Metadata(Builder builder) {
            this.description = builder.description;
            this.keywords = builder.keywords;
            this.h1 = builder.h1;
            this.title = requireValid(builder.docTitle, "docTitle is invalid!");
            this.h1Prefix = builder.h1Prefix;
        }

        /**
         * Get the {@link Page} description.
         *
         * @return the description
         */
        public String description() {
            return description;
        }

        /**
         * Get the keywords.
         *
         * @return the keywords
         */
        public String keywords() {
            return keywords;
        }

        /**
         * Get the alternative title.
         *
         * @return the alternative title
         */
        public String h1() {
            return h1;
        }

        /**
         * Get the title.
         *
         * @return the title
         */
        public String title() {
            return title;
        }

        /**
         * Get the parent title.
         *
         * @return the parent title
         */
        public String h1Prefix() {
            return h1Prefix;
        }

        @Override
        public Object get(String attr) {
            switch (attr) {
                case "description":
                    return description;
                case "keywords":
                    return keywords;
                case "h1":
                    return h1;
                case "title":
                    return title;
                case "h1Prefix":
                    return h1Prefix;
                default:
                    throw new IllegalStateException("Unknown attribute: " + attr);
            }
        }

        @Override
        public String toString() {
            return "Metadata{"
                    + "description='" + description + '\''
                    + ", keywords='" + keywords + '\''
                    + ", h1='" + h1 + '\''
                    + ", title='" + title + '\''
                    + ", h1Prefix='" + h1Prefix + '\''
                    + '}';
        }

        /**
         * Create a new instance from configuration.
         *
         * @param config config
         * @return new instance
         */
        public static Metadata create(Config config) {
            return builder().config(config).build();
        }

        /**
         * Create a new builder.
         *
         * @return new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder of {@link Metadata}.
         */
        @SuppressWarnings("unused")
        public static final class Builder {

            private String description;
            private String keywords;
            private String h1;
            private String h1Prefix;
            private String docTitle;

            /**
             * Set the description.
             *
             * @param description description
             * @return this builder
             */
            public Builder description(String description) {
                this.description = description;
                return this;
            }

            /**
             * Set the keywords.
             *
             * @param keywords keywords
             * @return this builder
             */
            public Builder keywords(String keywords) {
                this.keywords = keywords;
                return this;
            }

            /**
             * Set the title.
             *
             * @param title title
             * @return this builder
             */
            public Builder title(String title) {
                this.docTitle = title;
                return this;
            }

            /**
             * Set the h1.
             *
             * @param h1 h1
             * @return this builder
             */
            public Builder h1(String h1) {
                this.h1 = h1;
                return this;
            }

            /**
             * Set the h1 prefix.
             *
             * @param h1Prefix h1 prefix
             * @return this builder
             */
            public Builder h1Prefix(String h1Prefix) {
                this.h1Prefix = h1Prefix;
                return this;
            }

            /**
             * Apply the specified configuration.
             *
             * @param config config
             * @return this builder
             */
            public Builder config(Config config) {
                description = config.get("description").asString().orElse(null);
                keywords = config.get("keywords").asString().orElse(null);
                h1 = config.get("h1").asString().orElse(null);
                docTitle = config.get("doctitle").asString().orElse(null);
                h1Prefix = config.get("h1prefix").asString().orElse(null);
                return this;
            }

            /**
             * Build a new instance.
             *
             * @return new instance.
             */
            public Metadata build() {
                return new Metadata(this);
            }
        }
    }
}
