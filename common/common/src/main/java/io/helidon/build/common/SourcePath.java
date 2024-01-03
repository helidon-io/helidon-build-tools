/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.helidon.build.common.Strings.normalizePath;

/**
 * Utility class to parse and match path segments.
 */
public class SourcePath {

    private static final char WILDCARD_CHAR = '*';
    private static final String WILDCARD = "*";
    private static final String DOUBLE_WILDCARD = "**";
    private static final List<String> DEFAULT_INCLUDES = List.of("**/*");

    private final String[] segments;

    /**
     * Create a new {@link SourcePath} instance for a file in a given directory.
     * The path represented will be the relative path of the file in the directory
     *
     * @param dir  the directory containing the file
     * @param file the filed contained in the directory
     */
    public SourcePath(File dir, File file) {
        this(dir.toPath(), file.toPath());
    }

    /**
     * Create a new {@link SourcePath} instance for a file in a given directory.
     * The path represented will be the relative path of the file in the directory
     *
     * @param dir  the directory containing the file
     * @param file the filed contained in the directory
     */
    public SourcePath(Path dir, Path file) {
        this(dir.relativize(file));
    }

    /**
     * Create a new {@link SourcePath} instance for the given path.
     *
     * @param path the path to use
     */
    public SourcePath(Path path) {
        this(normalizePath(path));
    }

    /**
     * Create a new {@link SourcePath} instance for the given path.
     *
     * @param path the path to use as {@code String}
     */
    public SourcePath(String path) {
        segments = parseSegments(path);
    }

    /**
     * Create a new {@link SourcePath} instance for the given paths.
     *
     * @param prefix a path prefix, may be {@code null}
     * @param path   the path to use as {@code String}
     */
    public SourcePath(String prefix, String path) {
        this(prefix != null ? List.of(prefix, path) : List.of(path));
    }

    /**
     * Create a new {@link SourcePath} instance for the given paths.
     *
     * @param paths the paths to use as {@code String}
     */
    public SourcePath(List<String> paths) {
        segments = paths.stream()
                .flatMap(p -> Arrays.stream(parseSegments(p)))
                .toArray(String[]::new);
    }

    /**
     * Parse a {@code '/'} separated path into segments. Collapses empty or {@code '.'} only segments.
     *
     * @param path The path.
     * @return The segments.
     * @throws IllegalArgumentException If the path is invalid.
     */
    public static String[] parseSegments(String path) throws IllegalArgumentException {
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        }
        String[] tokens = path.split("/");
        List<String> segments = new ArrayList<>(tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if ((i < tokens.length - 1 && token.isEmpty())
                || token.equals(".")) {
                continue;
            }
            segments.add(token);
        }
        return segments.toArray(new String[0]);
    }

    /**
     * Filter the given {@code Collection} of {@link SourcePath} with the given filter.
     *
     * @param paths            the paths to filter
     * @param includesPatterns a {@code Collection} of {@code String} as include patterns
     * @param excludesPatterns a {@code Collection} of {@code String} as exclude patterns
     * @return the filtered {@code Collection} of pages
     */
    public static List<SourcePath> filter(Collection<SourcePath> paths,
                                          Collection<String> includesPatterns,
                                          Collection<String> excludesPatterns) {

        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }
        return paths.stream()
                .filter(p -> p.matches(includesPatterns, excludesPatterns))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SourcePath other = (SourcePath) obj;
        for (int i = 0; i < this.segments.length; i++) {
            if (!this.segments[i].equals(other.segments[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.segments);
        return hash;
    }

    private static boolean doRecursiveMatch(String[] segments,
                                            int offset,
                                            String[] patterns,
                                            int pOffset) {

        boolean expand = false;
        // for each segment pattern
        for (; pOffset < patterns.length; pOffset++) {
            // no segment to match
            if (offset == segments.length) {
                break;
            }
            String pattern = patterns[pOffset];
            if (pattern.equals(DOUBLE_WILDCARD)) {
                expand = true;
            } else {
                if (expand) {
                    for (int j = 0; j < segments.length; j++) {
                        if (wildcardMatch(segments[j], pattern)) {
                            if (doRecursiveMatch(segments, j + 1, patterns, pOffset + 1)) {
                                return true;
                            }
                        }
                    }
                    return false;
                } else if (!wildcardMatch(segments[offset], pattern)) {
                    return false;
                }
                offset++;
            }
        }

        // unprocessed patterns can only be double wildcard
        for (; pOffset < patterns.length; pOffset++) {
            String pattern = patterns[pOffset];
            if (!(pattern.equals(DOUBLE_WILDCARD) || pattern.equals(WILDCARD))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests if this {@link SourcePath} matches any of the given include patterns and none of the excludes patterns.
     *
     * @param includesPatterns includes patterns, if {@code null} or empty matches everything
     * @param excludesPatterns excludes patterns, if {@code null} or empty matches nothing
     * @return {@code true} if this {@link SourcePath} matches, {@code false} otherwise
     */
    public boolean matches(Collection<String> includesPatterns, Collection<String> excludesPatterns) {
        if (includesPatterns == null || includesPatterns.isEmpty()) {
            includesPatterns = DEFAULT_INCLUDES;
        }
        return matches(includesPatterns) && !matches(excludesPatterns);
    }

    /**
     * Tests if any of the given patterns match this {@link SourcePath}.
     *
     * @param patterns the patterns to match, if {@code null} or empty matches nothing
     * @return {@code true} if this {@link SourcePath} matches, {@code false} otherwise
     */
    public boolean matches(Collection<String> patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (matches(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests if the given pattern matches this {@link SourcePath}.
     *
     * @param pattern the pattern to match, if {@code null} matches nothing
     * @return {@code true} if this {@link SourcePath} matches, {@code false} otherwise
     */
    public boolean matches(String pattern) {
        if (pattern == null) {
            return false;
        }
        if (pattern.isEmpty()) {
            return segments.length == 0;
        }
        return doRecursiveMatch(segments, 0, parseSegments(pattern), 0);
    }

    /**
     * Tests if the given path segments match the given pattern segments.
     *
     * @param path    the path segments to match
     * @param pattern the pattern segments to match
     * @return {@code true} if the path matches the pattern, {@code false} otherwise
     */
    public static boolean matches(String[] path, String[] pattern) {
        Objects.requireNonNull(path);
        if (pattern == null) {
            return false;
        }
        if (pattern.length == 0) {
            return path.length == 0;
        }
        return doRecursiveMatch(path, 0, pattern, 0);
    }

    /**
     * Matches the given value with a pattern that may contain wildcard(s)
     * character that can filter any sub-sequence in the value.
     *
     * @param val     the string to filter
     * @param pattern the pattern to use for matching
     * @return returns {@code true} if pattern matches, {@code false} otherwise
     */
    public static boolean wildcardMatch(String val, String pattern) {

        Objects.requireNonNull(val);
        Objects.requireNonNull(pattern);

        if (pattern.isEmpty()) {
            // special case for empty pattern
            // matches if val is also empty
            return val.isEmpty();
        }

        int valIdx = 0;
        int patternIdx = 0;
        boolean matched = true;
        while (true) {
            int wildcardIdx = pattern.indexOf(WILDCARD_CHAR, patternIdx);
            if (wildcardIdx >= 0) {
                // pattern has unprocessed wildcard(s)
                int patternOffset = wildcardIdx - patternIdx;
                if (patternOffset > 0) {
                    // filter the sub pattern before the wildcard
                    String subPattern = pattern.substring(patternIdx, wildcardIdx);
                    int idx = val.indexOf(subPattern, valIdx);
                    if (patternIdx > 0 && pattern.charAt(patternIdx - 1) == WILDCARD_CHAR) {
                        // if expanding a wildcard
                        // the sub-segment needs to contain the sub-pattern
                        if (idx < valIdx) {
                            matched = false;
                            break;
                        }
                    } else if (idx != valIdx) {
                        // not expanding a wildcard
                        // the sub-segment needs to start with the sub-pattern
                        matched = false;
                        break;
                    }
                    valIdx = idx + subPattern.length();
                }
                patternIdx = wildcardIdx + 1;
            } else {
                String subPattern = pattern.substring(patternIdx);
                String subSegment = val.substring(valIdx);
                if (patternIdx > 0 && pattern.charAt(patternIdx - 1) == WILDCARD_CHAR) {
                    // if expanding a wildcard
                    // sub-segment needs to end with sub-pattern
                    if (!subSegment.endsWith(subPattern)) {
                        matched = false;
                    }
                } else if (!subSegment.equals(subPattern)) {
                    // not expanding a wildcard
                    // the sub-segment needs to strictly filter the sub-pattern
                    matched = false;
                }
                break;
            }
        }
        return matched;
    }

    /**
     * Convert this {@link SourcePath} into a {@code String}.
     *
     * @return the absolute {@code String} representation of this {@link SourcePath}
     * @see #asString(boolean)
     */
    public String asString() {
        return asString(true);
    }

    /**
     * Convert this {@link SourcePath} into a {@code String}.
     *
     * @param absolute {@code true} if the representation should start with a {@code /}
     * @return the {@code String} representation of this {@link SourcePath}
     */
    public String asString(boolean absolute) {
        StringBuilder sb = new StringBuilder(absolute ? "/" : "");
        for (int i = 0; i < segments.length; i++) {
            sb.append(segments[i]);
            if (i < segments.length - 1) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return asString(false);
    }

    private static class SourceFileComparator implements Comparator<SourcePath> {

        @Override
        public int compare(SourcePath o1, SourcePath o2) {
            for (int i = 0; i < o1.segments.length; i++) {
                int cmp = o1.segments[i].compareTo(o2.segments[i]);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        }
    }

    private static final Comparator<SourcePath> COMPARATOR = new SourceFileComparator();

    /**
     * Sort the given {@code List} of {@link SourcePath} with natural ordering.
     *
     * @param sourcePaths the {@code List} of {@link SourcePath} to sort
     * @return the sorted {@code List}
     */
    public static List<SourcePath> sort(List<SourcePath> sourcePaths) {
        Objects.requireNonNull(sourcePaths, "sourcePaths");
        sourcePaths.sort(COMPARATOR);
        return sourcePaths;
    }

    /**
     * Scan all files recursively as {@link SourcePath} instance in the given directory.
     *
     * @param dir the directory to scan
     * @return the {@code List} of scanned {@link SourcePath}
     */
    public static List<SourcePath> scan(File dir) {
        return scan(dir.toPath());
    }

    /**
     * Scan all files recursively as {@link SourcePath} instance in the given directory.
     *
     * @param dir the directory to scan
     * @return the {@code List} of scanned {@link SourcePath}
     */
    public static List<SourcePath> scan(Path dir) {
        return doScan(dir, dir);
    }

    private static List<SourcePath> doScan(Path root, Path dir) {
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            List<SourcePath> sourcePaths = new ArrayList<>();
            for (Path next : dirStream) {
                if (Files.isDirectory(next)) {
                    sourcePaths.addAll(doScan(root, next));
                } else {
                    sourcePaths.add(new SourcePath(root, next));
                }
            }
            return sort(sourcePaths);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
