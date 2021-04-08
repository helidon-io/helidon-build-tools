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

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import io.helidon.build.util.SourcePath;
import io.helidon.config.Config;

/**
 * Configuration for {@link SourcePath} filter.
 */
public class SourcePathFilter {

    private static final String INCLUDES_PROP = "includes";
    private static final String EXCLUDES_PROP = "excludes";
    private final List<String> includes;
    private final List<String> excludes;

    private SourcePathFilter(List<String> includes, List<String> excludes) {
        this.includes = includes == null ? Collections.emptyList() : includes;
        this.excludes = excludes == null ? Collections.emptyList() : excludes;
    }

    /**
     * Get the includes patterns.
     * @return the includes patterns, never {@code null}
     */
    public List<String> getIncludes() {
        return includes;
    }

    /**
     * Get the includes patterns.
     * @return the includes patterns, never {@code null}
     */
    public List<String> getExcludes() {
        return excludes;
    }

    /**
     * A fluent builder to create {@link SourcePathFilter} instances.
     */
    public static class Builder extends AbstractBuilder<SourcePathFilter> {

        /**
         * Set includes patterns.
         * @param includes the includes patterns to use
         * @return the {@link Builder} instance
         */
        public Builder includes(List<String> includes){
            put(INCLUDES_PROP, includes);
            return this;
        }

        /**
         * Set excludes patterns.
         * @param excludes the excludes patterns to use
         * @return the {@link Builder} instance
         */
        public Builder excludes(List<String> excludes) {
            put(EXCLUDES_PROP, excludes);
            return this;
        }

        /**
         * Apply the configuration represented by the given {@link Config} node.
         * @param node a {@link Config} node containing configuration values to apply
         * @return the {@link Builder} instance
         */
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
        public SourcePathFilter build() {
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
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new SourcePathFilter(includes, excludes);
        }
    }

    /**
     * Create a new {@link Builder} instance.
     * @return the created builder
     */
    public static Builder builder(){
        return new Builder();
    }
}
