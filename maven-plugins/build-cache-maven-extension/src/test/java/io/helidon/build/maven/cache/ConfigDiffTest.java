/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test {@link ConfigDiff}.
 */
class ConfigDiffTest {

    @Test
    void valueTest() throws Exception {
        List<ConfigDiff> diffs = ConfigDiff.diff(parse("<a><b>c</b></a>"), parse("<a><b>not-c</b></a>"));
        assertThat(diffs.size(), is(1));
        assertThat(diffs.get(0).asString(), is("b was 'c' but is now 'not-c'"));
    }

    @Test
    void attributeTest() throws Exception {
        List<ConfigDiff> diffs;

        diffs = ConfigDiff.diff(parse("<a b=\"c\"/>"), parse("<a b=\"not-c\"/>"));
        assertThat(diffs.size(), is(1));
        assertThat(diffs.get(0).asString(), is("@b was 'c' but is now 'not-c'"));

        diffs = ConfigDiff.diff(parse("<a b=\"c\" d=\"e\"/>"), parse("<a b=\"not-c\" d=\"not-e\"/>"));
        assertThat(diffs.size(), is(2));
        assertThat(diffs.get(0).asString(), is("@b was 'c' but is now 'not-c'"));
        assertThat(diffs.get(1).asString(), is("@d was 'e' but is now 'not-e'"));
    }

    @Test
    void addTest() throws Exception {
        List<ConfigDiff> diffs;

        diffs = ConfigDiff.diff(parse("<a></a>"), parse("<a><b>c</b></a>"));
        assertThat(diffs.size(), is(1));
        assertThat(diffs.get(0).asString(), is("b has been added"));

        diffs = ConfigDiff.diff(parse("<a><b>c</b></a>"), parse("<a><b>c</b><b>d</b></a>"));
        assertThat(diffs.size(), is(1));
        assertThat(diffs.get(0).asString(), is("b[1] has been added"));
    }

    @Test
    void removeTest() throws Exception {
        List<ConfigDiff> diffs;

        diffs = ConfigDiff.diff(parse("<a><b>c</b></a>"), parse("<a></a>"));
        assertThat(diffs.size(), is(1));
        assertThat(diffs.get(0).asString(), is("b has been removed"));

        diffs = ConfigDiff.diff(parse("<a><b>c</b><d>e</d></a>"), parse("<a><b>c</b></a>"));
        assertThat(diffs.size(), is(1));
        assertThat(diffs.get(0).asString(), is("d has been removed"));
    }

    @Test
    void mixedChangesTest() throws Exception {
        List<ConfigDiff> diffs;

        diffs = ConfigDiff.diff(parse("<a><b>c</b></a>"), parse("<a><d>e</d></a>"));
        assertThat(diffs.size(), is(2));
        assertThat(diffs.get(0).asString(), is("b has been removed"));
        assertThat(diffs.get(1).asString(), is("d has been added"));

        diffs = ConfigDiff.diff(parse("<a><b>c</b><d>e</d></a>"), parse("<a><d>e</d></a>"));
        assertThat(diffs.size(), is(1));
        assertThat(diffs.get(0).asString(), is("b has been removed"));

        diffs = ConfigDiff.diff(parse("<a><b>c</b><d>e</d></a>"), parse("<a><d>e</d><f>g</f><h>i</h></a>"));
        assertThat(diffs.size(), is(3));
        assertThat(diffs.get(0).asString(), is("b has been removed"));
        assertThat(diffs.get(1).asString(), is("f has been added"));
        assertThat(diffs.get(2).asString(), is("h has been added"));
    }

    private static XMLElement parse(String str) throws IOException {
        return XMLElement.parse(new ByteArrayInputStream(str.getBytes()));
    }
}
