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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import io.helidon.build.common.logging.Log;

import static io.helidon.build.common.Strings.normalizePath;

/**
 * Utility class for gitignore parsing.
 */
class GitIgnore implements FileMatcher {

    private final List<Pattern> excludes;
    private final List<Pattern> includes;

    private GitIgnore() {
        this.excludes = new LinkedList<>();
        this.includes = new LinkedList<>();
    }

    private void include(List<Pattern> pattern) {
        includes.addAll(pattern);
    }

    private void exclude(List<Pattern> pattern) {
        excludes.addAll(pattern);
    }

    /**
     * Create {@link FileMatcher} from gitignore file.
     *
     * @param gitRepoDir directory of gitignore
     * @return a git file matcher
     */
    static GitIgnore create(Path gitRepoDir) {
        GitIgnore ignore = new GitIgnore();
        Path gitIgnore = gitRepoDir.resolve(".gitignore");
        if (!Files.exists(gitIgnore)) {
            return ignore;
        }

        FileSystem.toLines(gitIgnore)
                .stream()
                .filter(it -> !it.startsWith("#"))
                .filter(it -> !it.isBlank())
                .forEach(ignore::parsePattern);
        return ignore;
    }

    static GitIgnore create(List<String> patterns) {
        GitIgnore ignore = new GitIgnore();
        patterns.forEach(ignore::parsePattern);
        return ignore;
    }

    private void parsePattern(String pattern) {
        if (pattern.startsWith("\\!")) {
            String exclude = pattern.substring(2);
            if (isParentExcluded(exclude)) {
                include(create(exclude));
            }
            return;
        }
        exclude(create(pattern));
    }

    /**
     * Create matcher from git pattern.
     *
     * @param pattern pattern to parse
     * @return matcher that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    private static List<Pattern> create(String pattern) {
        List<Pattern> patterns = new LinkedList<>();

        pattern = pattern.trim();
        pattern = pattern.replaceAll("\\?", "[^/]");
        if (pattern.contains("*")) {
            pattern = Pattern.compile("(^|[^*])\\*([^*]|$)")
                    .matcher(pattern)
                    .replaceAll(result -> result.group().replace("*", "([^/]*)"));
        }
        if (pattern.contains("**")) {
            pattern = Pattern.compile("(^|[^*])\\*\\*([^*]|$)")
                    .matcher(pattern)
                    .replaceAll(result -> result.group().replace("**", "([^;]*)"));
        }
        if (pattern.startsWith("/")) {
            pattern = pattern.replaceFirst("/", "/?");
        }
        pattern = pattern.replaceAll("\\.", "\\\\.");
        if (pattern.endsWith("/")) {
            pattern = pattern + "([^;]*)";
            patterns.add(Pattern.compile("([^;]*)/" + pattern));
        }
        if (pattern.contains("/([^;]*)/")) {
            patterns.add(Pattern.compile(pattern.replaceAll("/\\(\\[\\^;]\\*\\)/", "/")));
        }
        if (pattern.contains("([^;]*)/([^/]*)")) {
            patterns.add(Pattern.compile(
                    pattern.replaceAll("\\(\\[\\^;]\\*\\)/\\(\\[\\^/]\\*\\)", "([^/]+)")));
        }
        patterns.add(Pattern.compile(pattern));
        return patterns;
    }

    @Override
    public boolean matches(FileRequest file) {
        String path = file.relativePath();
        for (Pattern pattern : includes) {
            if (pattern.matcher(path).matches()) {
                Log.debug("Including " + path);
                return false;
            }
        }
        for (Pattern pattern : excludes) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isParentExcluded(String pattern) {
        pattern = "/" + pattern;
        String parent = normalizePath(Path.of(pattern).getParent().toString());
        if (!parent.endsWith("/")) {
            //if Windows path then replace the separator
            pattern = parent.toString().replaceAll("\\\\","/") + "/";
        }
        for (Pattern exclude : excludes) {
            if (exclude.matcher(pattern).matches()) {
                return false;
            }
        }
        return true;
    }
}
