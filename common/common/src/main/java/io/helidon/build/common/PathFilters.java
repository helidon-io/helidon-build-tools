/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides both common and custom filters for path matching; they do <em>not</em> check that the path exists or make
 * any other file system test. Custom filters are created using
 * <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant path patterns</a>.
 * <br><br>
 * All patterns are matched against a <em>relative</em> path. Since either an absolute or relative path may be passed to the
 * filter, {@code BiPredicate<Path,Path>} is required so that an absolute path may be converted to a relative one prior to
 * matching. The first path parameter to the filter is always the path to match against, and the second is the root directory
 * if needed to convert the first parameter to a relative one.
 */
public class PathFilters {
    private static final BiPredicate<Path, Path> ANY = (path, root) -> true;
    private static final BiPredicate<Path, Path> NONE = (path, root) -> false;

    private static final Path ROOT = Path.of(File.separator);
    private static final String SINGLE_CHAR_WILDCARD = "?";
    private static final String MULTI_CHAR_WILDCARD = "*";
    private static final char SINGLE_CHAR_WILDCARD_CHAR = '?';
    private static final char MULTI_CHAR_WILDCARD_CHAR = '*';
    private static final String DIRECTORY_WILDCARD = "**";
    private static final String PATH_SEPARATOR = "/";
    private static final char PATH_SEPARATOR_CHAR = '/';
    private static final char WINDOWS_SEPARATOR_CHAR = '\\';

    private static final String ANY_PARENT_WILDCARD_PATTERN = "**/";
    private static final String ANY_CHILD_WILDCARD_PATTERN = "/**";
    private static final String ANY_FILE_PATTERN = "**/*";
    private static final String ESCAPED_MULTI_CHAR_WILDCARD = "\\*";
    private static final char SINGLE_CHAR_WILDCARD_REGEX_CHAR = '.';
    private static final String MULTI_CHAR_WILDCARD_REGEX = ".*";
    private static final String SINGLE_CHAR_REGEX_WILDCARD = ".";
    private static final String ESCAPED_DOT_LITERAL = "\\.";

    /**
     * Returns a filter that returns {@code true} for any path.
     *
     * @return The filter. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesAny() {
        return ANY;
    }

    /**
     * Returns a filter that returns {@code false} for any path.
     *
     * @return The filter. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesNone() {
        return NONE;
    }

    /**
     * Returns a filter that returns {@code true} for any filename that equals the given name.
     *
     * @param name The file name to match.
     * @return The filter. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesFileName(String name) {
        return (path, root) -> path.getFileName().toString().endsWith(name);
    }

    /**
     * Returns a filter that returns {@code true} for any filename that ends with the given suffix.
     *
     * @param suffix The file name suffix to match.
     * @return The filter. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesFileNameSuffix(String suffix) {
        return (path, root) -> path.getFileName().toString().endsWith(suffix);
    }

    /**
     * Returns a filter that matches the given pattern.
     *
     * @param pattern The pattern.
     * @return The filter, where the first path parameter is made relative if required using the second parameter as the root.
     */
    public static BiPredicate<Path, Path> matches(String pattern) {
        pattern = normalizePattern(pattern);

        // Handle some common cases directly

        if (pattern.equals(ANY_FILE_PATTERN)) {

            // Match any path

            return matchesAny();

        } else if (pattern.startsWith(ANY_PARENT_WILDCARD_PATTERN)) {
            final String patternSuffix = stripConstantPrefix(pattern, ANY_PARENT_WILDCARD_PATTERN);
            if (patternSuffix.indexOf(PATH_SEPARATOR_CHAR) < 0) {

                // Match a filename within any directory

                return toFileNamePredicate(patternSuffix);

            } else if (patternSuffix.endsWith(ANY_CHILD_WILDCARD_PATTERN)) {

                // Match any path containing a constant path

                final String containedPath = stripConstantSuffix(patternSuffix, ANY_CHILD_WILDCARD_PATTERN);
                return (path, root) -> normalizePath(path, root).contains(containedPath);
            }
        } else if (pattern.endsWith(ANY_CHILD_WILDCARD_PATTERN)) {
            final String patternPrefix = stripConstantSuffix(pattern, DIRECTORY_WILDCARD);
            if (!containsWildcardChar(patternPrefix)) {

                // Match any path starting with a constant prefix

                return (path, root) -> normalizePath(path, root).startsWith(patternPrefix);
            }
        }

        // Not a common case we handle, so use SourcePath

        final String[] patternSegments = SourcePath.parseSegments(pattern);
        return (path, root) -> {
            final String[] pathSegments = SourcePath.parseSegments(normalizePath(path, root));
            return SourcePath.matches(pathSegments, patternSegments);
        };
    }

    /**
     * Returns a filter that returns {@code true} if any of the given patterns match.
     *
     * @param patterns The patterns.
     * @return The filter, where the first path parameter is made relative if required using the second parameter as the root.
     */
    public static BiPredicate<Path, Path> matchesAny(List<String> patterns) {
        if (patterns.isEmpty()) {
            return matchesAny();
        } else if (patterns.size() == 1) {
            return matches(patterns.get(0));
        } else {
            final List<BiPredicate<Path, Path>> filters = patterns.stream()
                                                                  .map(PathFilters::matches)
                                                                  .collect(Collectors.toList());
            return (path, root) -> {
                final Path relativePath = relativizePath(path, root);
                for (BiPredicate<Path, Path> filter : filters) {
                    if (filter.test(relativePath, root)) {
                        return true;
                    }
                }
                return false;
            };
        }
    }

    /**
     * Returns a filter that returns {@code true} if none of the given patterns match.
     *
     * @param patterns The patterns.
     * @return The filter.
     */
    public static BiPredicate<Path, Path> matchesNone(List<String> patterns) {
        if (patterns.isEmpty()) {
            return matchesAny();
        } else {
            final BiPredicate<Path, Path> filter = matchesAny(patterns);
            return (path, root) -> !filter.test(path, root);
        }
    }

    /**
     * Returns a filter that returns {@code true} if any of the given include patterns match and
     * none of the given exclude patterns match.
     *
     * @param includes The included patterns. If empty, acts as if all files are included.
     * @param excludes The excluded patterns.
     * @return The filter, where the first path parameter is made relative if required using the second parameter as the root.
     */
    public static BiPredicate<Path, Path> matches(List<String> includes, List<String> excludes) {
        if (includes.isEmpty()) {
            if (excludes.size() == 1 && excludes.get(0).equals(ANY_FILE_PATTERN)) {
                return matchesNone();
            } else {
                return matchesNone(excludes);
            }
        }
        return matchesAny(includes).and(matchesNone(excludes));
    }

    private static BiPredicate<Path, Path> toFileNamePredicate(String fileNamePattern) {
        if (fileNamePattern.contains(SINGLE_CHAR_WILDCARD)) {
            return toFileNameRegexPredicate(fileNamePattern);
        } else {
            final String[] segments = fileNamePattern.split(ESCAPED_MULTI_CHAR_WILDCARD);
            final int segmentCount = segments.length;
            if (segmentCount == 1) {
                // **/foo.txt
                final String fileName = segments[0];
                return matchesFileName(fileName);
            } else if (segmentCount == 2) {
                final String prefix = segments[0];
                final String suffix = segments[1];
                if (prefix.isEmpty()) {
                    // **/*.txt
                    return matchesFileNameSuffix(suffix);
                } else {
                    // **/Foo*.txt
                    return (path, root) -> {
                        final String name = path.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(suffix);
                    };
                }
            } else {
                // more than 1 wildcard, e.g. Foo*Bar*.java, use a regex
                return toFileNameRegexPredicate(fileNamePattern);
            }
        }
    }

    private static BiPredicate<Path, Path> toFileNameRegexPredicate(String fileNamePattern) {
        final String regex = fileNamePattern.replace(SINGLE_CHAR_REGEX_WILDCARD, ESCAPED_DOT_LITERAL)
                                            .replace(SINGLE_CHAR_WILDCARD_CHAR, SINGLE_CHAR_WILDCARD_REGEX_CHAR)
                                            .replace(MULTI_CHAR_WILDCARD, MULTI_CHAR_WILDCARD_REGEX);
        final Pattern pattern = Pattern.compile(regex);
        return (path, root) -> pattern.matcher(path.getFileName().toString()).matches();
    }

    private static boolean containsWildcardChar(String value) {
        return value.indexOf(SINGLE_CHAR_WILDCARD_CHAR) >= 0
               || value.indexOf(MULTI_CHAR_WILDCARD_CHAR) >= 0;
    }

    private static String stripConstantPrefix(String value, String constant) {
        return value.substring(constant.length());
    }

    private static String stripConstantSuffix(String value, String constant) {
        return value.substring(0, value.length() - constant.length());
    }

    private static boolean isAbsolute(Path path) {
        return path.isAbsolute() || ROOT.equals(path.getRoot());
    }

    private static Path relativizePath(Path path, Path root) {
        return isAbsolute(path) ? root.relativize(path) : path;
    }

    private static String normalizePath(Path path, Path root) {
        return normalizePathSeparators(relativizePath(path, root).toString());
    }

    private static String normalizePathSeparators(String path) {
        return path.replace(WINDOWS_SEPARATOR_CHAR, PATH_SEPARATOR_CHAR);
    }

    private static String normalizePattern(String pattern) {
        if (Strings.isNotValid(pattern)) {
            throw new IllegalArgumentException("pattern cannot be null or empty");
        }

        // Normalize separators
        pattern = normalizePathSeparators(pattern);

        // Convert trailing slash to "/**"
        if (pattern.endsWith(PATH_SEPARATOR)) {
            pattern += DIRECTORY_WILDCARD;
        }
        return pattern;
    }

    private PathFilters() {
    }
}
