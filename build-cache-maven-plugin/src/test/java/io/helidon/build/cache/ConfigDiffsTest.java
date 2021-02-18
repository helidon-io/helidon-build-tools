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
package io.helidon.build.cache;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cache.ConfigNodeTest.configNode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link ExecutionEntry}.
 */
class ConfigDiffsTest {

    @Test
    void valueTest() throws Exception {
        ConfigDiffs diffs = configNode("<a><b>c</b></a>").diff(configNode("<a><b>not-c</b></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0}/b was 'c' but is now 'not-c'"));
        assertThat(diffs.hasNext(), is(false));
    }

    @Test
    void attributeTest() throws Exception {
        ConfigDiffs diffs;

        diffs = configNode("<a b=\"c\"/>").diff(configNode("<a b=\"not-c\"/>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0}#b was 'c' but is now 'not-c'"));
        assertThat(diffs.hasNext(), is(false));

        diffs = configNode("<a b=\"c\" d=\"e\"/>").diff(configNode("<a b=\"not-c\" d=\"not-e\"/>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0}#b was 'c' but is now 'not-c'"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0}#d was 'e' but is now 'not-e'"));
        assertThat(diffs.hasNext(), is(false));
    }

    @Test
    void addTest() throws Exception {
        ConfigDiffs diffs;

        diffs = configNode("<a></a>").diff(configNode("<a><b>c</b></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0} has been added"));
        assertThat(diffs.hasNext(), is(false));

        diffs = configNode("<a><b>c</b></a>").diff(configNode("<a><b>c</b><d>e</d></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{1} has been added"));
        assertThat(diffs.hasNext(), is(false));
    }

    @Test
    void removeTest() throws Exception {
        ConfigDiffs diffs;

        diffs = configNode("<a><b>c</b></a>").diff(configNode("<a></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0} has been removed"));
        assertThat(diffs.hasNext(), is(false));

        diffs = configNode("<a><b>c</b><d>e</d></a>").diff(configNode("<a><b>c</b></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{1} has been removed"));
        assertThat(diffs.hasNext(), is(false));
    }

    @Test
    void mixedChangesTest() throws Exception {
        ConfigDiffs diffs;

        diffs = configNode("<a><b>c</b></a>").diff(configNode("<a><d>e</d></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0} was 'b=c' but is now 'd=e'"));
        assertThat(diffs.hasNext(), is(false));

        diffs = configNode("<a><b>c</b><d>e</d></a>").diff(configNode("<a><d>e</d></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0} was 'b=c' but is now 'd=e'"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{1} has been removed"));

        diffs = configNode("<a><b>c</b><d>e</d></a>").diff(configNode("<a><d>e</d><f>g</f><h>i</h></a>"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{0} was 'b=c' but is now 'd=e'"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{1} was 'd=e' but is now 'f=g'"));
        assertThat(diffs.hasNext(), is(true));
        assertThat(diffs.next().asString(), is("/a{2} has been added"));
    }
}
