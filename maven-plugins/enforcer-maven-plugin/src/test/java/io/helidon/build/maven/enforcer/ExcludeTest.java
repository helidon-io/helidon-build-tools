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

import org.junit.jupiter.api.Test;

import static io.helidon.build.maven.enforcer.FileMatcher.PatternFormat.GITIGNORE;
import static io.helidon.build.maven.enforcer.FileMatcher.create;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ExcludeTest {
    @Test
    void testSuffix() {
        FileMatcher exclude = new FileMatcher.SuffixMatcher(".ico");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("src/main/icon.ico"));
        assertThat("Should matches icon", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("scr/main/icon.ico.txt"));
        assertThat("Should not matches icon.ico.txt", excluded, is(false));
    }

    @Test
    void testStartsWith() {
        // excludes with exact path from repository root
        FileMatcher exclude = new FileMatcher.StartsWithMatcher("etc/copyright.txt");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("etc/copyright.txt"));
        assertThat("Should matches exact match", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("webserver/etc/copyright.txt"));
        assertThat("Should not matches subpath match", excluded, is(false));
    }

    @Test
    void testDirectory() {
        FileMatcher exclude = new FileMatcher.DirectoryMatcher("target/");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("webserver/target/classes/test.class"));
        assertThat("Should matches nested target directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("target/classes/test.class"));
        assertThat("Should matches target directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("targets/classes/test.class"));
        assertThat("Should not matches targets directory", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webserver/targets/classes/test.class"));
        assertThat("Should not matches targets directory", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webtarget/classes/test.class"));
        assertThat("Should not matches webtarget directory", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webserver/webtarget/classes/test.class"));
        assertThat("Should not matches webtarget directory", excluded, is(false));
    }

    @Test
    void testName() {
        // test file name
        FileMatcher exclude = new FileMatcher.NameMatcher("copyright.txt");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("copyright.txt"));
        assertThat("Should matches copyright.txt", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("webserver/etc/copyright.txt"));
        assertThat("Should matches copyright.txt in directory", excluded, is(true));
    }

    @Test
    void testContains() {
        FileMatcher exclude = new FileMatcher.ContainsMatcher("src/resources/bin");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("src/resources/bin/copyright.txt"));
        assertThat("Should matches directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("webserver/src/resources/bin/copyright.txt"));
        assertThat("Should matches nested directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("src/resources/sbin/copyright.txt"));
        assertThat("Should not matches directory", excluded, is(false));
    }

    @Test
    void testNameEnd() {
        FileMatcher exclude = new FileMatcher.NameEndExclude("bar");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("foo.bar"));
        assertThat("Should matches foo.bar file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/foo/bar/foo.bar"));
        assertThat("Should matches nested directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo/bar/foo.bar"));
        assertThat("Should matches nested directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo.bar.foo"));
        assertThat("Should not matches foo.bar.foo file", excluded, is(false));
    }

    @Test
    void testNameStart() {
        FileMatcher exclude = new FileMatcher.NameStartExclude("foo");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("foo.bar"));
        assertThat("Should matches foo.bar file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/foo/bar/foo.bar"));
        assertThat("Should matches nested directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo/bar/foo.bar"));
        assertThat("Should matches nested directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("bar.foo.bar"));
        assertThat("Should not matches foo.bar.foo file", excluded, is(false));
    }

    @Test
    void testRegexMatcher() {
        FileMatcher exclude = new FileMatcher.RegexMatcher("[a-z]oo");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("foo"));
        assertThat("Should matches foo file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("bar/foo"));
        assertThat("Should not matches bar/foo file", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("1oo"));
        assertThat("Should not matches 1oo file", excluded, is(false));

        exclude = new FileMatcher.RegexMatcher("foo-[a-z]oo");
        excluded = exclude.matches(FileRequest.create("foo-foo"));
        assertThat("Should matches foo-foo file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo-1oo"));
        assertThat("Should not matches foo-1oo file", excluded, is(false));

        exclude = new FileMatcher.RegexMatcher("foo-[a-z]oo-[a-z]oo");
        excluded = exclude.matches(FileRequest.create("foo-foo-foo"));
        assertThat("Should matches foo-foo-foo file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo-1oo-foo"));
        assertThat("Should not matches foo-1oo-foo file", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("foo-foo-1oo"));
        assertThat("Should not matches foo-foo-1oo file", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("foo-1oo-1oo"));
        assertThat("Should not matches foo-1oo-1oo file", excluded, is(false));
    }

    @Test
    void testMiddleDirectory() {
        FileMatcher exclude = new FileMatcher.MiddleDirectoryMatcher("a/**/b");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("a/c/b"));
        assertThat("Should matches a/c/b directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/a/c/b"));
        assertThat("Should matches /a/c/b directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("a/c/b/"));
        assertThat("Should matches a/c/b/ directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/a/c/b/"));
        assertThat("Should matches /a/c/b/ directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("a/foo/bar/b"));
        assertThat("Should matches a/foo/bar/b", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("a/foo/bar/bar"));
        assertThat("Should not matches a/foo/bar/bar", excluded, is(false));
    }

    @Test
    void testEndsWith() {
        FileMatcher exclude = new FileMatcher.EndsWithMatcher("src/main/java");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("src/main/java"));
        assertThat("Should matches src/main/java directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("root/src/main/java"));
        assertThat(exclude, instanceOf(FileMatcher.EndsWithMatcher.class));
        assertThat("Should matches root/src/main/java directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("src/main/java/directory"));
        assertThat(exclude, instanceOf(FileMatcher.EndsWithMatcher.class));
        assertThat("Should not matches src/main/java/directory directory", excluded, is(false));
    }

    @Test
    void testInclude() {
        FileMatcher exclude = new FileMatcher.GitIncludeMatcher("\\!etc/copyright.txt");

        boolean excluded;
        excluded = exclude.matches(FileRequest.create("etc/copyright.txt"));
        assertThat("Should matches etc/copyright.txt file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("etc/copyright"));
        assertThat("Should not matches etc/copyright file", excluded, is(false));
    }

    @Test
    void testDefaultFileMatcher() {
        List<FileMatcher> matchers = create(".foo");

        assertThat(matchers.size(), is(2));
        assertThat(matchers.get(0), instanceOf(FileMatcher.SuffixMatcher.class));

        matchers = create("*.foo");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.SuffixMatcher.class));

        matchers = create("foo/bar/");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.DirectoryMatcher.class));

        matchers = create("**/bar/");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.DirectoryMatcher.class));

        matchers = create("/foo/bar");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.StartsWithMatcher.class));

        matchers = create("foo/bar");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.ContainsMatcher.class));
    }

    @Test
    void testGitFileMatcherInstance() {
        List<FileMatcher> matchers = createGitIgnore(".foo");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.NameMatcher.class));

        matchers = createGitIgnore("foo");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.NameMatcher.class));

        matchers = createGitIgnore("*.foo");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.SuffixMatcher.class));

        matchers = createGitIgnore("foo/bar/");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.DirectoryMatcher.class));

        matchers = createGitIgnore("/foo/bar");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.StartsWithMatcher.class));

        matchers = createGitIgnore("foo/bar");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.ContainsMatcher.class));

        matchers = createGitIgnore("**.foo");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.NameEndExclude.class));

        matchers = createGitIgnore("**/foo");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.EndsWithMatcher.class));

        matchers = createGitIgnore("foo.*");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.NameStartExclude.class));

        matchers = createGitIgnore("[a-z]oo");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.NameMatcher.class));

        matchers = createGitIgnore("\\!foo");
        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.GitIncludeMatcher.class));
    }

    @Test
    void testCombinedName() {
        List<FileMatcher> matchers = createGitIgnore("[a-z]o?");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("foo"));
        assertThat(exclude, instanceOf(FileMatcher.NameMatcher.class));
        assertThat("Should matches foo file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("f"));
        assertThat(exclude, instanceOf(FileMatcher.NameMatcher.class));
        assertThat("Should not matches f file", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webserver/etc/foo"));
        assertThat(exclude, instanceOf(FileMatcher.NameMatcher.class));
        assertThat("Should matches foo file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("webserver/etc/fo/"));
        assertThat(exclude, instanceOf(FileMatcher.NameMatcher.class));
        assertThat("Should not matches 'webserver/etc/fo/'", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("1oo"));
        assertThat(exclude, instanceOf(FileMatcher.NameMatcher.class));
        assertThat("Should not matches 1oo file", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webserver/etc/1oo"));
        assertThat(exclude, instanceOf(FileMatcher.NameMatcher.class));
        assertThat("Should not matches webserver/etc/1oo file", excluded, is(false));
    }

    @Test
    void testCombinedSuffix() {
        List<FileMatcher> matchers = createGitIgnore("*.[a-z]oo");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("src/main/icon.foo"));
        assertThat(exclude, instanceOf(FileMatcher.SuffixMatcher.class));
        assertThat("Should matches icon", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("scr/main/icon.foo.txt"));
        assertThat("Should not matches icon.foo.txt", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("scr/main/icon.1oo"));
        assertThat("Should not matches icon.1oo", excluded, is(false));
    }

    @Test
    void testCombinedStartsWith() {
        List<FileMatcher> matchers = createGitIgnore("/etc/[a-z]opyright.?xt");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("/etc/copyright.txt"));
        assertThat(exclude, instanceOf(FileMatcher.StartsWithMatcher.class));
        assertThat("Should matches /etc/copyright.txt file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("etc/copyright.txt"));
        assertThat("Should matches etc/copyright.txt file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("webserver/etc/copyright.txt"));
        assertThat("Should not matches subpath match", excluded, is(false));
    }

    @Test
    void testCombinedDirectory() {
        List<FileMatcher> matchers = createGitIgnore("ta[a-z]get/");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("webserver/target/classes/test.class"));
        assertThat(exclude, instanceOf(FileMatcher.DirectoryMatcher.class));
        assertThat("Should matches nested target directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("target/classes/test.class"));
        assertThat("Should matches target directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("targets/classes/test.class"));
        assertThat("Should not matches targets directory", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webserver/targets/classes/test.class"));
        assertThat("Should not matches targets directory", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webtarget/classes/test.class"));
        assertThat("Should not matches webtarget directory", excluded, is(false));

        excluded = exclude.matches(FileRequest.create("webserver/webtarget/classes/test.class"));
        assertThat("Should not matches webtarget directory", excluded, is(false));
    }

    @Test
    void testCombinedContains() {
        List<FileMatcher> matchers = createGitIgnore("src/[a-z]esources/bin");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("src/resources/bin/copyright.txt"));
        assertThat(exclude, instanceOf(FileMatcher.ContainsMatcher.class));
        assertThat("Should matches directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("webserver/src/resources/bin/copyright.txt"));
        assertThat("Should matches nested directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("src/resources/sbin/copyright.txt"));
        assertThat("Should not matches directory", excluded, is(false));
    }

    @Test
    void testCombinedNameEnd() {
        List<FileMatcher> matchers = createGitIgnore("*[a-z]ar");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("foo.bar"));
        assertThat(exclude, instanceOf(FileMatcher.NameEndExclude.class));
        assertThat("Should matches foo.bar file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/foo/bar/foo.bar"));
        assertThat("Should matches /foo/bar/foo.bar", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo/bar/foo.bar"));
        assertThat("Should matches foo/bar/foo.bar file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo.bar.foo"));
        assertThat("Should not matches foo.bar.foo file", excluded, is(false));
    }

    @Test
    void testCombinedNameStart() {
        List<FileMatcher> matchers = createGitIgnore("[a-z]oo*");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("foo.bar"));
        assertThat(exclude, instanceOf(FileMatcher.NameStartExclude.class));
        assertThat("Should matches foo.bar file", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/foo/bar/foo.bar"));
        assertThat("Should matches /foo/bar/foo.bar", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo/bar/foo.bar"));
        assertThat("Should matches foo/bar/foo.bar", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("bar.foo.bar"));
        assertThat("Should not matches foo.bar.foo file", excluded, is(false));
    }

    @Test
    void testCombinedEndsWith() {
        FileMatcher exclude = new FileMatcher.EndsWithMatcher("foo/[a-z]ar");
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("foo/bar"));
        assertThat("Should matches foo/bar", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/foo/rar"));
        assertThat("Should matches /foo/rar", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("foo/bar/foo/1ar"));
        assertThat("Should not matches foo/bar/foo/1ar", excluded, is(false));
    }

    @Test
    void testCombinedMiddleDirectory() {
        List<FileMatcher> matchers = createGitIgnore("[a-z]/**/[a-z]");
        assertThat(matchers.size(), is(1));

        FileMatcher exclude = matchers.get(0);
        boolean excluded;
        excluded = exclude.matches(FileRequest.create("a/c/b"));
        assertThat(exclude, instanceOf(FileMatcher.MiddleDirectoryMatcher.class));
        assertThat("Should matches a/c/b directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/a/c/b"));
        assertThat("Should matches /a/c/b directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("a/c/b/"));
        assertThat("Should matches a/c/b/ directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("/a/c/b/"));
        assertThat("Should matches /a/c/b/ directory", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("a/foo/bar/b"));
        assertThat("Should matches a/foo/bar/b", excluded, is(true));

        excluded = exclude.matches(FileRequest.create("a/foo/bar/bar"));
        assertThat("Should matches a/foo/bar/bar", excluded, is(false));
    }

    private static List<FileMatcher> createGitIgnore(String pattern) {
        return create(pattern, GITIGNORE);
    }
}
