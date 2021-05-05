/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer;

import java.util.List;

/**
 * A matcher for files created from a list of excludes or includes.
 * Supports the following patterns:
 * <ul>
 *     <li>{@code .txt} - matches files with this suffix and files with the same exact file name</li>
 *     <li>{@code *.txt} - matches files with this suffix</li>
 *     <li>{@code /etc/txt} - matches files with relative path starting with the provided String</li>
 *     <li>{@code main/src/} - matches directory (would match both /main/src and /module/main/src)</li>
 *     <li>{@code Dockerfile.native} - matches exact file name</li>
 *     <li>Other text - matches a substring of the relative path</li>
 * </ul>
 */
public interface FileMatcher {
    /**
     * Create matcher(s) from pattern.
     *
     * @param pattern  pattern to parse
     * @return one or more matchers that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    static List<FileMatcher> create(String pattern) {
        // if starts with ., it is a suffix
        if (pattern.startsWith(".") && !pattern.endsWith("/")) {
            // .ico
            return List.of(new SuffixMatcher(pattern), new NameMatcher(pattern));
        }
        if (pattern.startsWith("*.")) {
            // *.ico (.gitignore)
            return List.of(new SuffixMatcher(pattern.substring(1)));
        }
        if (pattern.startsWith("/")) {
            // /etc/txt - from repo root
            return List.of(new StartsWithMatcher(pattern.substring(1)));
        }
        if (pattern.endsWith("/")) {
            // src/main/proto/
            return List.of(new DirectoryMatcher(pattern));
        }
        if (pattern.contains(".") && !pattern.contains("/")) {
            // jaxb.index
            return List.of(new NameMatcher(pattern));
        }
        return List.of(new ContainsMatcher(pattern));
    }

    /**
     * Whether the file matches.
     *
     * @param file file information
     * @return true if the file matches the rule
     */
    boolean matches(FileRequest file);

    /**
     * Matches relative paths that start with the provided string.
     */
    class StartsWithMatcher implements FileMatcher {
        // exact path from repository root, such as etc/copyright.txt
        private final String pattern;

        StartsWithMatcher(String pattern) {
            this.pattern = pattern;
        }

        /**
         * Create a new matcher from pattern. Note that the pattern must not have leading slash.
         *
         * @param startsWith string the relative path must start with to match
         * @return new matcher
         */
        public static StartsWithMatcher create(String startsWith) {
            return new StartsWithMatcher(startsWith);
        }

        @Override
        public boolean matches(FileRequest file) {
            return file.relativePath().startsWith(pattern);
        }
    }

    /**
     * Matches relative paths that contain the directory.
     */
    class DirectoryMatcher implements FileMatcher {
        private final ContainsMatcher contains;
        private final StartsWithMatcher startWith;

        DirectoryMatcher(String directory) {
            // either the directory is within the tree
            this.contains = new ContainsMatcher("/" + directory);
            // or the tree starts with it
            this.startWith = new StartsWithMatcher(directory);
        }

        /**
         * Create a new matcher from a directory string.
         *
         * @param directoryString directory to match (can be more than one), never with leading slash
         * @return a new matcher
         */
        public static DirectoryMatcher create(String directoryString) {
            return new DirectoryMatcher(directoryString);
        }

        @Override
        public boolean matches(FileRequest file) {
            return contains.matches(file) || startWith.matches(file);
        }
    }

    /**
     * Matches relative paths that contain the string.
     */
    class ContainsMatcher implements FileMatcher {
        // such as src/main/proto
        private final String contains;

        ContainsMatcher(String contains) {
            this.contains = contains;
        }

        /**
         * Create a new matcher.
         *
         * @param contains string that must be contained ({@link java.lang.String#contains(CharSequence)}) in the relative path
         * @return a new matcher
         */
        public static ContainsMatcher create(String contains) {
            return new ContainsMatcher(contains);
        }

        @Override
        public boolean matches(FileRequest file) {
            return file.relativePath().contains(contains);
        }
    }

    /**
     * Matches file name.
     */
    class NameMatcher implements FileMatcher {
        private final String name;

        NameMatcher(String name) {
            this.name = name;
        }

        /**
         * A new matcher that matches the provided file name.
         *
         * @param name name of the file to match, in any directory (such as {@code README.md})
         * @return a new matcher
         */
        public static NameMatcher create(String name) {
            return new NameMatcher(name);
        }

        @Override
        public boolean matches(FileRequest file) {
            return file.fileName().equals(name);
        }
    }

    /**
     * Matches file suffix.
     */
    class SuffixMatcher implements FileMatcher {
        private final String suffix;

        SuffixMatcher(String suffix) {
            this.suffix = suffix;
        }

        /**
         * Create a new suffix matcher.
         *
         * @param suffix suffix to match, must include the leading dot
         * @return a new matcher
         */
        public static SuffixMatcher create(String suffix) {
            return new SuffixMatcher(suffix);
        }

        @Override
        public boolean matches(FileRequest file) {
            return file.suffix().equals(suffix);
        }
    }
}
