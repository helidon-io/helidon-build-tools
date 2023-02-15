/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
import java.util.regex.Pattern;

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
     * Create default matcher(s) from pattern.
     *
     * @param pattern pattern to parse
     * @return one or more matchers that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    static List<FileMatcher> create(String pattern) {
        return create(pattern, PatternFormat.DEFAULT);
    }

    /**
     * Create matcher(s) from pattern using {@link PatternFormat}.
     *
     * @param pattern pattern to parse
     * @param format  pattern format
     * @return one or more matchers that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    static List<FileMatcher> create(String pattern, PatternFormat format) {
        if (format == PatternFormat.GITIGNORE) {
            return List.of(createGitIgnore(pattern));
        }
        return createDefault(pattern);
    }

    /**
     * Create matcher from git pattern.
     *
     * @param pattern pattern to parse
     * @return matcher that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    static FileMatcher createGitIgnore(String pattern) {
        //Start with '!', negates the pattern. It is not possible to re-include a file if a parent directory
        //of that file is excluded.
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

    /**
     * Create default matcher(s) from pattern.
     *
     * @param pattern  pattern to parse
     * @return one or more matchers that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    private static List<FileMatcher> createDefault(String pattern) {
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
     * Get file matcher pattern.
     *
     * @return pattern
     */
    String pattern();

    /**
     * {@link FileMatcher} base implementation class.
     */
    abstract class FileMatcherBase implements FileMatcher {
        /**
         * Regular expression matcher.
         */
        private final RegexMatcher matcher;
        /**
         * File matcher pattern.
         */
        private final String pattern;

        protected FileMatcherBase(String pattern) {
            this(pattern, pattern);
        }

        protected FileMatcherBase(String pattern, String regex) {
            this.matcher = RegexMatcher.create(regex);
            this.pattern = pattern;
        }

        @Override
        public String pattern() {
            return this.pattern;
        }

        RegexMatcher matcher() {
            return this.matcher;
        }
    }

    /**
     * Matches relative paths that start with the provided string.
     */
    class StartsWithMatcher extends FileMatcherBase {

        StartsWithMatcher(String pattern) {
            super(pattern);
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
            if (matcher() == null) {
                return file.relativePath().startsWith(pattern());
            }
            String path = file.relativePath().startsWith("/")
                    ? file.relativePath().substring(1)
                    : file.relativePath();
            if (path.length() < matcher().length()) {
                return false;
            }
            return matcher().matches(path.substring(0, matcher().length()));
        }
    }

    /**
     * Matches relative paths that contain the directory.
     */
    class DirectoryMatcher extends FileMatcherBase {
        private final ContainsMatcher contains;
        private final StartsWithMatcher startWith;

        DirectoryMatcher(String directory) {
            super(directory);
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
    class ContainsMatcher extends FileMatcherBase {

        ContainsMatcher(String contains) {
            super(contains);
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
            if (matcher() == null) {
                return file.relativePath().contains(pattern());
            }
            String path = file.relativePath();
            if (matcher().length() > path.length()) {
                return false;
            }
            int i = 0;
            while (i < path.length() - matcher().length()) {
                if (matcher().matches(path.substring(i, i + matcher().length()))) {
                    return true;
                }
                i++;
            }
            return false;
        }
    }

    /**
     * Matches file name.
     */
    class NameMatcher extends FileMatcherBase {

        NameMatcher(String name) {
            super(name);
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
            return matcher() == null
                    ? file.fileName().equals(pattern())
                    : matcher().matches(file.fileName());
        }
    }

    /**
     * Matches file suffix.
     */
    class SuffixMatcher extends FileMatcherBase {

        SuffixMatcher(String suffix) {
            super(suffix);
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
            return matcher() == null
                    ? file.suffix().equals(pattern())
                    : matcher().matches(file.suffix());

        }
    }

    /**
     * Matches file name that end with the provided string.
     */
    class NameEndExclude extends FileMatcherBase {

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
    class NameStartExclude extends FileMatcherBase {

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
    class MiddleDirectoryMatcher implements FileMatcher {
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
            return (start.matches(file) || startSlash .matches(file))
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
    class EndsWithMatcher extends FileMatcherBase {

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
    class GitIncludeMatcher extends FileMatcherBase {
        private final FileMatcher matcher;

        GitIncludeMatcher(String pattern) {
            super(pattern.substring(2));
            //remove "\!"
            this.matcher = createGitIgnore(pattern());
        }

        @Override
        public boolean matches(FileRequest file) {
            return matcher.matches(file);
        }
    }

    /**
     * Matches file name with regular expressions.
     */
    class RegexMatcher implements FileMatcher {

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

        private int length() {
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

    private static boolean containsRegex(String pattern) {
        if (pattern == null) {
            return false;
        }
        return pattern.contains("?") || (pattern.contains("[") && pattern.contains("]"));
    }

    /**
     * File matcher pattern format.
     */
    enum PatternFormat {
        /**
         * Default.
         */
        DEFAULT,
        /**
         * GitIgnore.
         */
        GITIGNORE
    }
}
