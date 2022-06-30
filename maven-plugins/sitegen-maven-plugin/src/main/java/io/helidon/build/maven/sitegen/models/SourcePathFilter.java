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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import io.helidon.build.common.SourcePath;
import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.Model;

/**
 * Configuration for {@link SourcePath} filter.
 */
public abstract class SourcePathFilter implements Model {

    private final List<String> includes;
    private final List<String> excludes;

    /**
     * Create a new instance.
     *
     * @param builder builder
     */
    protected SourcePathFilter(AbstractBuilder<?, ?> builder) {
        this.includes = builder.includes;
        this.excludes = builder.excludes;
    }

    /**
     * Get the includes patterns.
     *
     * @return the includes patterns, never {@code null}
     */
    public List<String> includes() {
        return includes;
    }

    /**
     * Get the includes patterns.
     *
     * @return the includes patterns, never {@code null}
     */
    public List<String> excludes() {
        return excludes;
    }

    /**
     * Resolve this filter.
     *
     * @param paths input paths
     * @return resolved paths
     */
    public List<SourcePath> resolvePaths(Collection<SourcePath> paths) {
        if (includes.isEmpty()) {
            return List.of();
        }
        return SourcePath.filter(paths, includes, excludes);
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case "includes":
                return includes;
            case "excludes":
                return excludes;
            default:
                throw new IllegalArgumentException("Unknown attribute: " + attr);
        }
    }

    /**
     * Base builder for subclasses of {@link SourcePathFilter}.
     *
     * @param <T> builder subtype
     * @param <U> subtype
     */
    @SuppressWarnings({"unchecked", "unused"})
    public abstract static class AbstractBuilder<T extends AbstractBuilder<T, U>, U extends SourcePathFilter>
            implements Supplier<U> {

        private final List<String> includes = new ArrayList<>();
        private final List<String> excludes = new ArrayList<>();

        /**
         * Get the includes.
         *
         * @return includes, never {@code null}
         */
        public List<String> includes() {
            return includes;
        }

        /**
         * Get the excludes.
         *
         * @return excludes, never {@code null}
         */
        public List<String> excludes() {
            return excludes;
        }

        /**
         * Add includes patterns.
         *
         * @param includes includes patterns
         * @return this builder
         */
        public T includes(List<String> includes) {
            if (includes != null) {
                this.includes.addAll(includes);
            }
            return (T) this;
        }

        /**
         * Add includes patterns.
         *
         * @param includes includes patterns
         * @return this builder
         */
        public T includes(String... includes) {
            if (includes != null) {
                this.includes.addAll(Arrays.asList(includes));
            }
            return (T) this;
        }

        /**
         * Add excludes patterns.
         *
         * @param excludes the excludes patterns
         * @return this builder
         */
        public T excludes(List<String> excludes) {
            if (excludes != null) {
                this.excludes.addAll(excludes);
            }
            return (T) this;
        }

        /**
         * Add excludes patterns.
         *
         * @param excludes the excludes patterns
         * @return this builder
         */
        public T excludes(String... excludes) {
            if (excludes != null) {
                this.excludes.addAll(Arrays.asList(excludes));
            }
            return (T) this;
        }

        /**
         * Apply the specified configuration.
         *
         * @param config config
         * @return this builder
         */
        public T config(Config config) {
            includes.addAll(config.get("includes").asList(String.class).orElseGet(List::of));
            excludes.addAll(config.get("excludes").asList(String.class).orElseGet(List::of));
            return (T) this;
        }

        @Override
        public final U get() {
            return build();
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public abstract U build();
    }
}
