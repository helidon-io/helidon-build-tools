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
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides both common and custom path predicates. Custom predicates are created using
 * <a href="http://ant.apache.org/manual/dirtasks.html#patterns>Ant path patterns></a>.
 * <p></p>
 * All patterns are matched against a <em>relative</em> path. Since either an absolute or relative path may be passed to the
 * predicate, {@code BiPredicate<Path,Path>} is required so that an absolute path may be converted to a relative one prior to
 * matching. The first path parameter to the predicate is always the path to match against, and the second is the root directory
 * if needed to convert the first parameter to a relative one.
 */
public class PathPredicates {

    private static final BiPredicate<Path, Path> ANY = (path, root) -> true;
    private static final BiPredicate<Path, Path> MAVEN_POM = (path, root) -> matchesName(path, "pom.xml");
    private static final BiPredicate<Path, Path> JAVA_SOURCE = (path, root) -> matchesExtension(path, ".java");
    private static final BiPredicate<Path, Path> JAVA_CLASS = (path, root) -> matchesExtension(path, ".class");
    private static final BiPredicate<Path, Path> JAR_FILE = (path, root) -> matchesExtension(path, ".jar");
    private static final BiPredicate<Path, Path> RESOURCE_FILE = (path, root) -> {
        final String fileName = path.getFileName().toString();
        return !fileName.startsWith(".") && !fileName.endsWith(".class") && !fileName.endsWith(".swp") && !fileName.endsWith("~");
    };

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
     * Returns a predicate that returns {@code true} for any path.
     *
     * @return The predicate. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesAny() {
        return ANY;
    }

    /**
     * Returns a predicate that returns {@code true} for any filename that equals {@code "pom.xml"}.
     *
     * @return The predicate. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesMavenPom() {
        return MAVEN_POM;
    }

    /**
     * Returns a predicate that returns {@code true} for any filename ending with {@code ".java"}.
     *
     * @return The predicate. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesJavaSource() {
        return JAVA_SOURCE;
    }

    /**
     * Returns a predicate that returns {@code true} for any filename ending with {@code ".class"}.
     *
     * @return The predicate. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesJavaClass() {
        return JAVA_CLASS;
    }

    /**
     * Returns a predicate that returns {@code true} for any filename ending with {@code ".jar"}.
     *
     * @return The predicate. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesJar() {
        return JAR_FILE;
    }

    /**
     * Returns a predicate that returns {@code true} for any filename that does not start with {@code "."} and does not end with
     * {@code ".class"}, {@code ".swp"} or {@code "~"}.
     *
     * @return The predicate. The second path parameter is always ignored.
     */
    public static BiPredicate<Path, Path> matchesResource() {
        return RESOURCE_FILE;
    }

    /**
     * Returns a predicate that matches the given pattern.
     *
     * @param pattern The pattern.
     * @return The predicate, where the first path parameter is made relative if required using the second parameter as the root.
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
     * Returns a predicate that returns {@code true} if any of the given patterns match.
     *
     * @param patterns The patterns.
     * @return The predicate, where the first path parameter is made relative if required using the second parameter as the root.
     */
    public static BiPredicate<Path, Path> matchesAny(List<String> patterns) {
        if (patterns.isEmpty()) {
            return matchesAny();
        } else if (patterns.size() == 1) {
            return matches(patterns.get(0));
        } else {
            final List<BiPredicate<Path, Path>> predicates = patterns.stream()
                                                                     .map(PathPredicates::matches)
                                                                     .collect(Collectors.toList());
            return (path, root) -> {
                final Path relativePath = relativizePath(path, root);
                for (BiPredicate<Path, Path> predicate : predicates) {
                    if (predicate.test(relativePath, root)) {
                        return true;
                    }
                }
                return false;
            };
        }
    }

    /**
     * Returns a predicate that returns {@code true} if none of the given patterns match.
     *
     * @param patterns The patterns.
     * @return The predicate.
     */
    public static BiPredicate<Path, Path> matchesNone(List<String> patterns) {
        if (patterns.isEmpty()) {
            return matchesAny();
        } else {
            final BiPredicate<Path, Path> predicate = matchesAny(patterns);
            return (path, root) -> !predicate.test(path, root);
        }
    }

    /**
     * Returns a predicate that returns {@code true} if any of the given include patterns match and
     * none of the given exclude patterns match.
     *
     * @param includes The included patterns. If empty, acts as if all files are included.
     * @param excludes The excluded patterns.
     * @return The predicate, where the first path parameter is made relative if required using the second parameter as the root.
     */
    public static BiPredicate<Path, Path> matches(List<String> includes, List<String> excludes) {
        if (includes.isEmpty()) {
            return matchesNone(excludes);
        }
        return matchesAny(includes).and(matchesNone(excludes));
    }

    private static boolean matchesName(Path file, String name) {
        return file.getFileName().toString().endsWith(name);
    }

    private static boolean matchesExtension(Path file, String extension) {
        return file.getFileName().toString().endsWith(extension);
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
                return (path, root) -> matchesName(path, fileName);
            } else if (segmentCount == 2) {
                final String prefix = segments[0];
                final String suffix = segments[1];
                if (prefix.isEmpty()) {
                    // **/*.txt
                    return (path, root) -> matchesExtension(path, suffix);
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

    private static Path relativizePath(Path path, Path root) {
        return path.isAbsolute() ? root.relativize(path) : path;
    }

    private static String normalizePath(Path path, Path root) {
        return normalizePathSeparators(relativizePath(path, root).toString());
    }

    private static String normalizePathSeparators(String path) {
        return path.replace(WINDOWS_SEPARATOR_CHAR, PATH_SEPARATOR_CHAR);
    }

    private static String normalizePattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
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

    private PathPredicates() {
    }
}
