/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import io.helidon.build.common.logging.Log;

/**
 * Utility class for gitignore parsing.
 */
class GitIgnore implements FileMatcher {

    private final List<FileMatcher> excludes;
    private final List<FileMatcher> includes;

    private GitIgnore(List<FileMatcher> excludes, List<FileMatcher> includes) {
        this.excludes = excludes;
        this.includes = includes;
    }

    /**
     * Create {@link FileMatcher} from gitignore file.
     *
     * @param gitRepoDir directory of gitignore
     * @return a git file matcher
     */
    static GitIgnore create(Path gitRepoDir) {
        List<FileMatcher> includes = new LinkedList<>();
        List<FileMatcher> excludes = new LinkedList<>(FileMatcher.create(".git/"));
        Path gitIgnore = gitRepoDir.resolve(".gitignore");

        FileSystem.toLines(gitIgnore)
                .stream()
                .filter(it -> !it.startsWith("#"))
                .filter(it -> !it.isBlank())
                .map(GitIgnore::create)
                .forEach(matcher -> {
                    if (matcher instanceof GitIncludeMatcher) {
                        if (isParentExcluded(matcher.pattern(), excludes)) {
                            includes.add(matcher);
                        }
                        return;
                    }
                    excludes.add(matcher);
                });
        return new GitIgnore(excludes, includes);
    }

    /**
     * Create matcher from git pattern.
     *
     * @param pattern pattern to parse
     * @return matcher that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    static FileMatcher create(String pattern) {
        if (pattern.startsWith("\\!")) {
            return new GitIncludeMatcher(pattern);
        }
        if (pattern.contains("**")) {
            if (pattern.startsWith("**/")) {
                return new EndsWithMatcher(pattern.substring(2));
            }
            if (pattern.endsWith("/**")) {
                return new StartsWithMatcher(pattern.substring(0, pattern.length() - 2));
            }
            if (pattern.contains("/**/")) {
                return new MiddleDirectoryMatcher(pattern);
            }
        }
        if (pattern.contains("*")) {
            if (pattern.startsWith("*.")) {
                return new SuffixMatcher(pattern.substring(1));
            }
            if (pattern.startsWith("*")) {
                return new NameEndExclude(pattern.substring(1));
            }
            if (pattern.endsWith("*")) {
                return new NameStartExclude(pattern.substring(0, pattern.length() - 1));
            }
        }
        if (pattern.startsWith("/")) {
            return new StartsWithMatcher(pattern.substring(1));
        }
        if (pattern.endsWith("/")) {
            return new DirectoryMatcher(pattern);
        }
        if (!pattern.contains("/")) {
            return new NameMatcher(pattern);
        }
        return new ContainsMatcher(pattern);
    }

    private static boolean isParentExcluded(String pattern, List<FileMatcher> excludes) {
        String parent = Path.of(pattern).getParent().toString();
        FileRequest file = FileRequest.create(parent + "/");
        for (FileMatcher exclude : excludes) {
            if (exclude.matches(file)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsRegex(String pattern) {
        if (pattern == null) {
            return false;
        }
        return pattern.contains("?") || (pattern.contains("[") && pattern.contains("]"));
    }

    @Override
    public boolean matches(FileRequest file) {
        for (FileMatcher include : includes) {
            if (include.matches(file)) {
                Log.debug("Including " + file.relativePath());
                return false;
            }
        }
        for (FileMatcher exclude : excludes) {
            if (exclude.matches(file)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String pattern() {
        return null;
    }

    /**
     * Matches file name that end with the provided string.
     */
    static class NameEndExclude extends FileMatcherBase {

        /**
         * Create a {@link NameEndExclude} matcher.
         *
         * @param end end of file name
         */
        NameEndExclude(String end) {
            super(end);
        }

        @Override
        public boolean matches(FileRequest file) {
            if (matcher() == null) {
                return file.fileName().endsWith(pattern());
            }
            String path = file.relativePath();
            if (path.length() < matcher().length()) {
                return false;
            }
            return matcher().matches(path.substring(path.length() - matcher().length()));
        }
    }

    /**
     * Matches file name that start with the provided string.
     */
    static class NameStartExclude extends FileMatcherBase {

        /**
         * Create a {@link NameStartExclude} matcher.
         *
         * @param start start of file name
         */
        NameStartExclude(String start) {
            super(start);
        }

        @Override
        public boolean matches(FileRequest file) {
            if (matcher() == null) {
                return file.fileName().startsWith(pattern());
            }
            if (file.fileName().length() < matcher().length()) {
                return false;
            }
            return matcher().matches(file.fileName().substring(0, matcher().length()));
        }
    }

    /**
     * Matches all directories in between.
     */
    static class MiddleDirectoryMatcher implements FileMatcher {
        private final String pattern;
        private final StartsWithMatcher start;
        private final StartsWithMatcher startSlash;
        private final EndsWithMatcher end;
        private final EndsWithMatcher endSlash;

        /**
         * Create a {@link MiddleDirectoryMatcher} matcher.
         *
         * @param pattern directory pattern
         */
        MiddleDirectoryMatcher(String pattern) {
            String[] elements = pattern.split("\\*\\*");
            String first = elements[0];
            String second = elements[1];
            this.pattern = pattern;
            startSlash = first.startsWith("/")
                    ? new StartsWithMatcher(first)
                    : new StartsWithMatcher("/" + first);
            start = first.startsWith("/")
                    ? new StartsWithMatcher(first.substring(1))
                    : new StartsWithMatcher(first);
            endSlash = second.endsWith("/")
                    ? new EndsWithMatcher(second)
                    : new EndsWithMatcher(second + "/");
            end = second.endsWith("/")
                    ? new EndsWithMatcher(second.substring(0, second.length() - 1))
                    : new EndsWithMatcher(second);
        }

        @Override
        public boolean matches(FileRequest file) {
            return (start.matches(file) || startSlash.matches(file))
                    && (end.matches(file) || endSlash.matches(file));
        }

        @Override
        public String pattern() {
            return pattern;
        }
    }

    /**
     * Matches relative path that end with provided string.
     */
    static class EndsWithMatcher extends FileMatcherBase {

        /**
         * Create a {@link EndsWithMatcher} matcher.
         *
         * @param pattern to match
         */
        EndsWithMatcher(String pattern) {
            super(pattern);
        }

        @Override
        public boolean matches(FileRequest file) {
            if (matcher() == null) {
                return file.relativePath().endsWith(pattern());
            }
            int length = file.relativePath().length();
            if (length < matcher().length()) {
                return false;
            }
            return matcher().matches(file.relativePath().substring(length - matcher().length()));
        }
    }

    /**
     * Matches relative path to be included by git.
     */
    static class GitIncludeMatcher extends FileMatcherBase {
        private final FileMatcher matcher;

        GitIncludeMatcher(String pattern) {
            //remove "\!"
            super(pattern.substring(2));
            this.matcher = create(pattern());
        }

        @Override
        public boolean matches(FileRequest file) {
            return matcher.matches(file);
        }
    }

    /**
     * Matches file name with regular expressions.
     */
    static class RegexMatcher implements FileMatcher {

        private final Pattern pattern;

        /**
         * Create a {@link RegexMatcher} matcher.
         *
         * @param pattern pattern
         */
        RegexMatcher(String pattern) {
            if (pattern.startsWith("**")) {
                pattern = pattern.substring(2);
            }
            if (pattern.startsWith("*")) {
                pattern = pattern.substring(1);
            }
            if (pattern.endsWith("*")) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }
            pattern = pattern.replace("/**/", "([^;]*)");
            pattern = pattern.replace("?", "[^/]");
            this.pattern = Pattern.compile(pattern);
        }

        static RegexMatcher create(String pattern) {
            return containsRegex(pattern) ? new RegexMatcher(pattern) : null;
        }

        @Override
        public boolean matches(FileRequest file) {
            return pattern.matcher(file.relativePath()).matches();
        }

        @Override
        public String pattern() {
            return pattern.pattern();
        }

        boolean matches(String pattern) {
            return this.pattern.matcher(pattern).matches();
        }

        int length() {
            String pattern = this.pattern.pattern();
            if (pattern.contains("([^;]*)")) {
                pattern = pattern.replace("([^;]*)", "");
            }
            int start = 0;
            int end;
            int length = pattern.length();
            while (0 <= start) {
                start = pattern.indexOf("[", start);
                end = pattern.indexOf("]", start);
                if (start < 0) {
                    break;
                }
                length -= end - start;
                start = end;
            }
            return length;
        }
    }
}
