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
    void testFileMatcherCreation() {
        List<FileMatcher> matchers = FileMatcher.create(".foo");

        assertThat(matchers.size(), is(2));
        assertThat(matchers.get(0), instanceOf(FileMatcher.SuffixMatcher.class));

        matchers = FileMatcher.create("*.foo");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.SuffixMatcher.class));

        matchers = FileMatcher.create("foo/bar/");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.DirectoryMatcher.class));

        matchers = FileMatcher.create("**/bar/");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.DirectoryMatcher.class));

        matchers = FileMatcher.create("/foo/bar");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.StartsWithMatcher.class));

        matchers = FileMatcher.create("foo/bar");

        assertThat(matchers.size(), is(1));
        assertThat(matchers.get(0), instanceOf(FileMatcher.ContainsMatcher.class));
    }

    @Test
    void testCreateFromGitPattern() {
        FileMatcher matcher = FileMatcher.createFromGitPattern(".foo");
        assertThat(matcher, instanceOf(FileMatcher.NameMatcher.class));

        matcher = FileMatcher.createFromGitPattern("foo");
        assertThat(matcher, instanceOf(FileMatcher.NameMatcher.class));

        matcher = FileMatcher.createFromGitPattern("*.foo");
        assertThat(matcher, instanceOf(FileMatcher.SuffixMatcher.class));

        matcher = FileMatcher.createFromGitPattern("foo/bar/");
        assertThat(matcher, instanceOf(FileMatcher.DirectoryMatcher.class));

        matcher = FileMatcher.createFromGitPattern("/foo/bar");
        assertThat(matcher, instanceOf(FileMatcher.StartsWithMatcher.class));

        matcher = FileMatcher.createFromGitPattern("foo/bar");
        assertThat(matcher, instanceOf(FileMatcher.ContainsMatcher.class));

        matcher = FileMatcher.createFromGitPattern("**.foo");
        assertThat(matcher, instanceOf(FileMatcher.NameEndExclude.class));

        matcher = FileMatcher.createFromGitPattern("foo.*");
        assertThat(matcher, instanceOf(FileMatcher.NameStartExclude.class));
    }
}
