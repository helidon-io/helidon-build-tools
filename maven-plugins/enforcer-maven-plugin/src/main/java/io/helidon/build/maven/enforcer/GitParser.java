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
import java.util.List;

import io.helidon.build.maven.enforcer.FileMatcher.ContainsMatcher;
import io.helidon.build.maven.enforcer.FileMatcher.DirectoryMatcher;
import io.helidon.build.maven.enforcer.FileMatcher.EndsWithMatcher;
import io.helidon.build.maven.enforcer.FileMatcher.GitIncludeMatcher;
import io.helidon.build.maven.enforcer.FileMatcher.MiddleDirectoryMatcher;
import io.helidon.build.maven.enforcer.FileMatcher.NameEndExclude;
import io.helidon.build.maven.enforcer.FileMatcher.NameMatcher;
import io.helidon.build.maven.enforcer.FileMatcher.NameStartExclude;
import io.helidon.build.maven.enforcer.FileMatcher.StartsWithMatcher;
import io.helidon.build.maven.enforcer.FileMatcher.SuffixMatcher;

import static io.helidon.build.maven.enforcer.FileMatcher.PatternFormat.GITIGNORE;
import static io.helidon.build.maven.enforcer.FileMatcher.create;

/**
 * Utility class for gitignore parsing.
 */
class GitParser {

    private GitParser() {
    }

    /**
     * Add {@link FileMatcher} from gitignore file.
     *
     * @param gitRepoDir directory of gitignore
     * @param excludes   exclusive file matchers
     * @param includes   inclusive file matchers
     */
    static void addGitIgnore(Path gitRepoDir, List<FileMatcher> excludes, List<FileMatcher> includes) {
        Path gitIgnore = gitRepoDir.resolve(".gitignore");

        excludes.addAll(create(".git/", GITIGNORE));

        FileSystem.toLines(gitIgnore)
                .stream()
                .filter(it -> !it.startsWith("#"))
                .filter(it -> !it.isBlank())
                .map(GitParser::createGitIgnore)
                .forEach(matcher -> {
                    if (matcher instanceof FileMatcher.GitIncludeMatcher) {
                        if (isParentExcluded(matcher.pattern(), excludes)) {
                            includes.add(matcher);
                        }
                        return;
                    }
                    excludes.add(matcher);
                });
    }

    /**
     * Create matcher from git pattern.
     *
     * @param pattern pattern to parse
     * @return matcher that can be matched against a {@link io.helidon.build.maven.enforcer.FileRequest}
     */
    static FileMatcher createGitIgnore(String pattern) {
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
}
