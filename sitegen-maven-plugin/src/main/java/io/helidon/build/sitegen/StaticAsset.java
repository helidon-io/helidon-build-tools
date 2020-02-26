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

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import io.helidon.config.Config;

import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;

/**
 * Configuration for static assets.
 *
 * Static assets are resources to be scanned in the site source directory
 * and copied in the generated directory.
 */
public class StaticAsset implements Model {

    private static final String TARGET_PROP = "target";
    private static final String INCLUDES_PROP = "includes";
    private static final String EXCLUDES_PROP = "excludes";

    private final String target;
    private final List<String> includes;
    private final List<String> excludes;

    private StaticAsset(String target,
            List<String> includes,
            List<String> excludes) {
        checkNonNullNonEmpty(target, "target");
        this.target = target;
        this.includes = includes == null ? Collections.emptyList() : includes;
        this.excludes = excludes == null ? Collections.emptyList() : excludes;
    }

    /**
     * Get the base path to use for copying the resources in the generated site
     * directory.
     * @return the base path, never {@code null}
     */
    public String getTarget() {
        return target;
    }

    /**
     * Get the include patterns used to filter the assets in the source directory.
     * @return {@code List<String>} for the include patterns, never {@code null}
     */
    public List<String> getIncludes() {
        return includes;
    }

    /**
     * Get the excludes patterns used to filter the assets in the source directory.
     * @return {@code List<String>} for the excludes patterns, never {@code null}
     */
    public List<String> getExcludes() {
        return excludes;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case (TARGET_PROP):
                return target;
            case (INCLUDES_PROP):
                return includes;
            case (EXCLUDES_PROP):
                return excludes;
            default:
                throw new IllegalArgumentException(
                        "Unkown attribute: " + attr);
        }
    }

    /**
     * A fluent builder to create {@link StaticAsset} instances.
     */
    public static class Builder extends AbstractBuilder<StaticAsset> {

        /**
         * Set the target path.
         * @param target the target path to use
         * @return the {@link Builder} instance
         */
        public Builder target(String target) {
            put(TARGET_PROP, target);
            return this;
        }

        /**
         * Set the include patterns.
         * @param includes the include patterns to use
         * @return the {@link Builder} instance
         */
        public Builder includes(List<String> includes) {
            put(INCLUDES_PROP, includes);
            return this;
        }

        /**
         * Set the exclude patterns.
         * @param excludes the exclude patterns to use
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
            if (node.exists()) {
                node.get(TARGET_PROP).ifExists(c
                        -> put(TARGET_PROP, c.asString()));
                node.get(INCLUDES_PROP).ifExists(c
                        -> put(INCLUDES_PROP, c.asStringList()));
                node.get(EXCLUDES_PROP).ifExists(c
                        -> put(EXCLUDES_PROP, c.asStringList()));
            }
            return this;
        }

        @Override
        public StaticAsset build() {
            String target = null;
            List<String> includes = null;
            List<String> excludes = null;
            for (Entry<String, Object> entry : values()) {
                String attr = entry.getKey();
                Object val = entry.getValue();
                switch (attr) {
                    case (TARGET_PROP):
                        target = asType(val, String.class);
                        break;
                    case (INCLUDES_PROP):
                        includes = asList(val, String.class);
                        break;
                    case (EXCLUDES_PROP):
                        excludes = asList(val, String.class);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unkown attribute: " + attr);
                }
            }
            return new StaticAsset(target, includes, excludes);
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
