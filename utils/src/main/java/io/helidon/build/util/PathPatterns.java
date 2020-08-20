/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.util;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility methods to create {@link Predicate<Path>} instances using
 * <a href="https://confluence.atlassian.com/fisheye/pattern-matching-guide-960155410.html">Ant style path patterns></a>.
 */
public class PathPatterns {

    private static final String DEFAULT_PATTERN_PREFIX = "/**/";
    private static final String DEFAULT_PATTERN_SUFFIX = "**";
    private static final String ANY_FILE_PATTERN = "/**/*";
    private static final String WILDCARD = "\\*";
    private static final String SINGLE_WILDCARD = "?";
    private static final String WILDCARD_REGEX = ".*";
    private static final char PATH_SEP_CHAR = '/';
    private static final char WINDOWS_SEP_CHAR = '\\';
    private static final String PATH_SEP = "/";

    /**
     * Returns a predicate that matches the given pattern.
     *
     * @param pattern The pattern.
     * @return The predicate.
     */
    public static Predicate<Path> matches(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern cannot be null or empty");
        }
        pattern = pattern.replace(WINDOWS_SEP_CHAR, PATH_SEP_CHAR);
        if (pattern.startsWith(PATH_SEP)) {
            pattern = DEFAULT_PATTERN_PREFIX + pattern;
        }
        if (pattern.endsWith(PATH_SEP)) {
            pattern += DEFAULT_PATTERN_SUFFIX;
        }
        if (pattern.equals(ANY_FILE_PATTERN)) {
            return path -> true;
        } else if (pattern.startsWith(DEFAULT_PATTERN_PREFIX)) {
            final String patternSuffix = pattern.substring(DEFAULT_PATTERN_PREFIX.length());
            if (patternSuffix.indexOf(PATH_SEP_CHAR) < 0) {

                // OK, we have a file name pattern

                return toFileNamePredicate(patternSuffix);
            }
        }

        // Not a simple pattern, so use SourcePath

        final String[] patternSegments = SourcePath.parseSegments(pattern);
        return path -> {
            final String linuxPath = path.toString().replace(WINDOWS_SEP_CHAR, PATH_SEP_CHAR);
            final String[] pathSegments = SourcePath.parseSegments(linuxPath);
            return SourcePath.matches(pathSegments, patternSegments);
        };
    }

    /**
     * Returns a predicate that returns {@code true} if any of the given patterns match.
     *
     * @param patterns The patterns.
     * @return The predicate.
     */
    public static Predicate<Path> matchesAny(List<String> patterns) {
        final List<Predicate<Path>> predicates = patterns.stream()
                                                         .map(PathPatterns::toFileNamePredicate)
                                                         .collect(Collectors.toList());
        return path -> {
            for (Predicate<Path> predicate : predicates) {
                if (predicate.test(path)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Returns a predicate that returns {@code true} if none of the given patterns match.
     *
     * @param patterns The patterns.
     * @return The predicate.
     */
    public static Predicate<Path> matchesNone(List<String> patterns) {
        return Predicate.not(matchesAny(patterns));
    }

    /**
     * Returns a predicate that returns {@code true} if any of the given include patterns match and
     * none of the given exclude patterns match.
     *
     * @param includes The included patterns.
     * @param excludes The excluded patterns.
     * @return The predicate.
     */
    public static Predicate<Path> matches(List<String> includes, List<String> excludes) {
        return matchesAny(includes).and(matchesNone(excludes));
    }

    private static List<String> assertNotEmpty(List<String> patterns) {
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("empty patterns list");
        }
        return patterns;
    }

    private static Predicate<Path> toFileNamePredicate(String fileNamePattern) {
        if (fileNamePattern.contains(SINGLE_WILDCARD)) {
            return toFileNamePatternPredicate(fileNamePattern);
        } else {
            final String[] segments = fileNamePattern.split(WILDCARD);
            final int segmentCount = segments.length;
            if (segmentCount == 1) {
                // **/foo.txt
                final String fileName = segments[0];
                return path -> path.getFileName().toString().equals(fileName);
            } else if (segmentCount == 2) {
                final String prefix = segments[0];
                final String suffix = segments[1];
                if (prefix.isEmpty()) {
                    // **/*.txt
                    return path -> path.getFileName().toString().endsWith(suffix);
                } else {
                    // **/Foo*.txt
                    return path -> {
                        final String name = path.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(suffix);
                    };
                }
            } else {
                // more than 1 wildcard, e.g. Foo*Bar*.java, use a regex
                return toFileNamePatternPredicate(fileNamePattern);
            }
        }
    }

    private static Predicate<Path> toFileNamePatternPredicate(String fileNamePattern) {
        final String regex = fileNamePattern.replace(".", "\\.")
                                            .replace(SINGLE_WILDCARD, ".")
                                            .replace(WILDCARD, WILDCARD_REGEX);
        final Pattern pattern = Pattern.compile(regex);
        return path -> pattern.matcher(path.getFileName().toString()).matches();
    }

    private PathPatterns() {
    }
}
