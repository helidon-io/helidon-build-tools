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

import static io.helidon.build.common.Strings.requireValid;

/**
 * Configuration for static assets.
 * <p>
 * Static assets are resources to be scanned in the site source directory
 * and copied in the generated directory.
 */
public final class StaticAsset extends SourcePathFilter {

    private final String target;

    private StaticAsset(Builder builder) {
        super(builder);
        this.target = requireValid(builder.target, "target is invalid!");
    }

    /**
     * Get the target.
     *
     * @return target, never {@code null}
     */
    public String target() {
        return target;
    }

    @Override
    public Object get(String attr) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (attr) {
            case ("target"):
                return target;
            default:
                return super.get(attr);
        }
    }

    @Override
    public String toString() {
        return "StaticAsset{"
                + "includes='" + includes() + "'"
                + ", excludes='" + excludes() + "'"
                + ", target='" + target + '\''
                + '}';
    }

    /**
     * Builder of {@link StaticAsset}.
     */
    public static class Builder extends AbstractBuilder<Builder, StaticAsset> {

        private String target;

        /**
         * Set the target path.
         *
         * @param target the target path to use
         * @return this builder
         */
        public Builder target(String target) {
            this.target = target;
            return this;
        }

        /**
         * Apply the specified configuration.
         *
         * @param config config
         * @return this builder
         */
        public Builder config(Config config) {
            target = config.get("target").asString().orElse(null);
            super.config(config);
            return this;
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public StaticAsset build() {
            return new StaticAsset(this);
        }
    }

    /**
     * Create a new instance from configuration.
     *
     * @param config config
     * @return new instance
     */
    public static StaticAsset create(Config config) {
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
