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

package io.helidon.build.archetype.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class to parse and match path segments.
 */
public class SourcePath {

    private static final char WILDCARD = '*';
    private static final String DOUBLE_WILDCARD = "**";
    private final String[] segments;

    /**
     * Create a new {@link SourcePath} instance for a file in a given directory.
     * The path represented will be the relative path of the file in the directory
     * @param dir the directory containing the file
     * @param file the filed contained in the directory
     */
    public SourcePath(File dir, File file){
        segments = parseSegments(getRelativePath(dir, file));
    }

    /**
     * Create a new {@link SourcePath} instance for the given path.
     * @param path the path to use as {@code String}
     */
    public SourcePath(String path) {
        segments = parseSegments(path);
    }

    private static String getRelativePath(File sourcedir, File source) {
        return sourcedir.toPath().relativize(source.toPath()).toString()
                // force UNIX style path on windows
                .replace("\\", "/");
    }

    private static String[] parseSegments(String path) throws IllegalArgumentException {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path is null or empty");
        }
        String[] tokens = path.split("/");
        if (tokens.length == 0) {
            throw new IllegalArgumentException("invalid path: " + path);
        }
        List<String> segments = new LinkedList<>();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if ((i < tokens.length - 1 && token.isEmpty())
                    || token.equals(".")) {
                continue;
            }
            segments.add(token);
        }
        return segments.toArray(new String[segments.size()]);
    }


    /**
     * Filter the given {@code Collection} of {@link SourcePath} with the given filter.
     * @param paths the paths to filter
     * @param includesPatterns a {@code Collection} of {@code String} as include patterns
     * @param excludesPatterns a {@code Collection} of {@code String} as exclude patterns
     * @return the filtered {@code Collection} of pages
     */
    public static List<SourcePath> filter(Collection<SourcePath> paths,
                                          Collection<String> includesPatterns,
                                          Collection<String> excludesPatterns){

        if (paths == null || paths.isEmpty()
                || includesPatterns == null || includesPatterns.isEmpty()) {
            return Collections.emptyList();
        }

        if (excludesPatterns == null) {
            excludesPatterns = Collections.emptyList();
        }

        List<SourcePath> included = new LinkedList<>();
        for (String includesPattern : includesPatterns) {
            for (SourcePath path : paths) {
                if (path.matches(includesPattern)) {
                    included.add(path);
                }
            }
        }

        List<SourcePath> matchedRoutes = new LinkedList<>();
        for (SourcePath path : included) {
            boolean matched = false;
            for (String excludesPattern : excludesPatterns) {
                if (path.matches(excludesPattern)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                matchedRoutes.add(path);
            }
        }
        return matchedRoutes;
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
                                            int pOffset){

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
            if (!pattern.equals(DOUBLE_WILDCARD)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests if the given pattern matches this {@link SourcePath}.
     * @param pattern the pattern to match
     * @return {@code true} if this {@link SourcePath} matches the pattern, {@code false} otherwise
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
     * Matches the given value with a pattern that may contain wildcard(s)
     * character that can filter any sub-sequence in the value.
     *
     * @param val the string to filter
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
        while (matched) {
            int wildcardIdx = pattern.indexOf(WILDCARD, patternIdx);
            if (wildcardIdx >= 0) {
                // pattern has unprocessed wildcard(s)
                int patternOffset = wildcardIdx - patternIdx;
                if (patternOffset > 0) {
                    // filter the sub pattern before the wildcard
                    String subPattern = pattern.substring(patternIdx, wildcardIdx);
                    int idx = val.indexOf(subPattern, valIdx);
                    if (patternIdx > 0 && pattern.charAt(patternIdx - 1) == WILDCARD) {
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
                if (patternIdx > 0 && pattern.charAt(patternIdx - 1) == WILDCARD) {
                    // if expanding a wildcard
                    // sub-segment needs to end with sub-pattern
                    if (!subSegment.endsWith(subPattern)) {
                        matched = false;
                    }
                } else if (!subSegment.equals(subPattern)) {
                    // not expanding a wildcard
                    // the sub-segment needs to stricly filter the sub-pattern
                    matched = false;
                }
                break;
            }
        }
        return matched;
    }

    /**
     * Convert this {@link SourcePath} into a {@code String}.
     * @return the {@code String} representation of this {@link SourcePath}
     */
    public String asString() {
        StringBuilder sb = new StringBuilder("/");
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
        return SourcePath.class.getSimpleName() + "{ " + asString() + " }";
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
     * @param sourcePaths the {@code List} of {@link SourcePath} to sort
     * @return the sorted {@code List}
     */
    public static List<SourcePath> sort(List<SourcePath> sourcePaths){
        Objects.requireNonNull(sourcePaths, "sourcePaths");
        sourcePaths.sort(COMPARATOR);
        return sourcePaths;
    }

    /**
     * Scan all files recursively as {@link SourcePath} instance in the given directory.
     * @param dir the directory to scan
     * @return the {@code List} of scanned {@link SourcePath}
     */
    public static List<SourcePath> scan(File dir) {
        return doScan(dir, dir);
    }

    private static List<SourcePath> doScan(File root, File dir) {
        List<SourcePath> sourcePaths = new ArrayList<>();
        DirectoryStream<Path> dirStream = null;
        try {
            dirStream = Files.newDirectoryStream(dir.toPath());
            Iterator<Path> it = dirStream.iterator();
            while (it.hasNext()) {
                Path next = it.next();
                if (Files.isDirectory(next)) {
                    sourcePaths.addAll(doScan(root, next.toFile()));
                } else {
                    sourcePaths.add(new SourcePath(root, next.toFile()));
                }
            }
        } catch (IOException ex) {
            if (dirStream != null) {
                try {
                    dirStream.close();
                } catch (IOException e) { }
            }
        }
        return sort(sourcePaths);
    }
}
