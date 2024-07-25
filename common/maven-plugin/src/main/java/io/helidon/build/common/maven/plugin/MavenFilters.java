/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import io.helidon.build.common.Lists;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.maven.MavenModel;

/**
 * Maven filters.
 */
public final class MavenFilters {

    private MavenFilters() {
        // cannot be instantiated
    }

    /**
     * Create a new predicate for a {@link Path} that matches when the given paths exist.
     *
     * @param paths paths that must exists when resolved against the tested path
     * @return predicate
     */
    public static Predicate<Path> dirFilter(List<String> paths) {
        return dir -> Files.isDirectory(dir) && paths.stream().map(dir::resolve).allMatch(Files::exists);
    }

    /**
     * Create a new predicate for a {@link Path} that matches include and exclude patterns.
     *
     * @param includes include patterns with glob support
     * @param excludes exclude patterns with glob support
     * @param dir      root directory used to relativize the paths
     * @return predicate
     */
    public static Predicate<Path> pathFilter(List<String> includes, List<String> excludes, Path dir) {
        return filter(includes, excludes, Function.identity(), p -> new SourcePath(dir, p), SourcePath::matches);
    }

    /**
     * Create a new predicate for {@link String} that matches include and exclude patterns.
     *
     * @param includes include patterns with wildcard support
     * @param excludes exclude patterns with wildcard support
     * @return predicate
     */
    public static Predicate<String> stringFilter(List<String> includes, List<String> excludes) {
        return filter(includes, excludes, Function.identity(), Function.identity(), SourcePath::wildcardMatch);
    }

    /**
     * Create a new predicate for {@link MavenArtifact} that matches include and exclude patterns.
     *
     * @param includes include patterns (see {@link MavenPattern}
     * @param excludes exclude patterns (see {@link MavenPattern}
     * @return predicate
     */
    public static Predicate<MavenArtifact> artifactFilter(List<String> includes, List<String> excludes) {
        return filter(includes, excludes, MavenPattern::create, Function.identity(), (a, p) -> p.matches(a));
    }

    /**
     * Create a new predicate for {@link MavenModel} that matches include and exclude patterns.
     *
     * @param includes include patterns (see {@link MavenPattern}
     * @param excludes exclude patterns (see {@link MavenPattern}
     * @return predicate
     */
    public static Predicate<MavenModel> pomFilter(List<String> includes, List<String> excludes) {
        return filter(includes, excludes, MavenPattern::create, Function.identity(), (a, p) -> p.matches(a));
    }

    /**
     * Create a new predicate.
     *
     * @param includes       raw include patterns
     * @param excludes       raw include patterns
     * @param patternFactory pattern factory
     * @param mapper         input object mapper
     * @param predicate      predicate function
     * @param <T>            pattern type
     * @param <U>            input type
     * @param <V>            mapped input type
     * @return predicate
     */
    public static <T, U, V> Predicate<U> filter(List<String> includes,
                                                List<String> excludes,
                                                Function<String, T> patternFactory,
                                                Function<U, V> mapper,
                                                BiFunction<V, T, Boolean> predicate) {

        List<T> includePatterns = Lists.map(includes, patternFactory);
        List<T> excludePatterns = Lists.map(excludes, patternFactory);
        return u -> {
            V v = mapper.apply(u);
            return includePatterns.stream().anyMatch(it -> predicate.apply(v, it))
                   && excludePatterns.stream().noneMatch(it -> predicate.apply(v, it));
        };
    }
}
