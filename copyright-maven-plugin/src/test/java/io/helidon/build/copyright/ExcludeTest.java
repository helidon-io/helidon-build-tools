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

package io.helidon.build.copyright;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ExcludeTest {
    @Test
    void testSuffix() {
        Exclude exclude = Exclude.create(".ico");

        boolean excluded;
        excluded = exclude.exclude(FileRequest.create("src/main/icon.ico"));
        assertThat("Should exclude icon", excluded, is(true));

        excluded = exclude.exclude(FileRequest.create("scr/main/icon.ico.txt"));
        assertThat("Should not exclude icon.ico.txt", excluded, is(false));
    }

    @Test
    void testStartsWith() {
        // excludes with exact path from repository root
        Exclude exclude = Exclude.create("/etc/copyright.txt");

        boolean excluded;
        excluded = exclude.exclude(FileRequest.create("etc/copyright.txt"));
        assertThat("Should exclude exact match", excluded, is(true));

        excluded = exclude.exclude(FileRequest.create("webserver/etc/copyright.txt"));
        assertThat("Should not exclude subpath match", excluded, is(false));
    }

    @Test
    void testDirectory() {
        Exclude exclude = Exclude.create("target/");

        boolean excluded;
        excluded = exclude.exclude(FileRequest.create("webserver/target/classes/test.class"));
        assertThat("Should exclude nested target directory", excluded, is(true));

        excluded = exclude.exclude(FileRequest.create("target/classes/test.class"));
        assertThat("Should exclude target directory", excluded, is(true));

        excluded = exclude.exclude(FileRequest.create("targets/classes/test.class"));
        assertThat("Should not exclude targets directory", excluded, is(false));

        excluded = exclude.exclude(FileRequest.create("webserver/targets/classes/test.class"));
        assertThat("Should not exclude targets directory", excluded, is(false));

        excluded = exclude.exclude(FileRequest.create("webtarget/classes/test.class"));
        assertThat("Should not exclude webtarget directory", excluded, is(false));

        excluded = exclude.exclude(FileRequest.create("webserver/webtarget/classes/test.class"));
        assertThat("Should not exclude webtarget directory", excluded, is(false));
    }

    @Test
    void testName() {
        // test file name
        Exclude exclude = Exclude.create("copyright.txt");

        boolean excluded;
        excluded = exclude.exclude(FileRequest.create("copyright.txt"));
        assertThat("Should exclude copyright.txt", excluded, is(true));

        excluded = exclude.exclude(FileRequest.create("webserver/etc/copyright.txt"));
        assertThat("Should exclude copyright.txt in directory", excluded, is(true));
    }

    @Test
    void testContains() {
        Exclude exclude = Exclude.create("src/resources/bin");

        boolean excluded;
        excluded = exclude.exclude(FileRequest.create("src/resources/bin/copyright.txt"));
        assertThat("Should exclude directory", excluded, is(true));

        excluded = exclude.exclude(FileRequest.create("webserver/src/resources/bin/copyright.txt"));
        assertThat("Should exclude nested directory", excluded, is(true));

        excluded = exclude.exclude(FileRequest.create("src/resources/sbin/copyright.txt"));
        assertThat("Should not exclude directory", excluded, is(false));
    }
}
