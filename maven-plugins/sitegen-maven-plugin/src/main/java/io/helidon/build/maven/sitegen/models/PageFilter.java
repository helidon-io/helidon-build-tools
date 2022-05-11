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

import io.helidon.build.maven.sitegen.Config;

/**
 * Configuration for a page filter.
 */
public final class PageFilter extends SourcePathFilter {

    private PageFilter(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return "PageFilter{"
                + "includes='" + includes() + "'"
                + ", excludes='" + excludes() + "'"
                + '}';
    }

    /**
     * Builder of {@link PageFilter}.
     */
    public static class Builder extends AbstractBuilder<Builder, PageFilter> {

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public PageFilter build() {
            return new PageFilter(this);
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param config config
     * @return new instance
     */
    public static PageFilter create(Config config) {
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
}
