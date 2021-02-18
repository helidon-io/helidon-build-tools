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

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link ExecutionEntry}.
 */
class ExecutionEntryTest {

    @Test
    void testFromXml() throws IOException, XmlPullParserException {
        ExecutionEntry execution = ExecutionEntry.fromXml(Xpp3DomBuilder.build(new StringReader(
                "<execution " +
                        "groupId=\"com.acme\" " +
                        "artifactId=\"my-plugin\" " +
                        "version=\"1.0\" " +
                        "goal=\"do-something\" " +
                        "id=\"default-do-something\" " +
                        "><configuration /></execution>"
        )));
        assertThat(execution.groupId(), is("com.acme"));
        assertThat(execution.artifactId(), is("my-plugin"));
        assertThat(execution.version(), is("1.0"));
        assertThat(execution.goal(), is("do-something"));
        assertThat(execution.executionId(), is("default-do-something"));
    }

    @Test
    void testToXml() {
        Xpp3Dom execution = new ExecutionEntry(
                "com.acme",
                "my-plugin",
                "1.0",
                "do-something",
                "default-do-something",
                null)
                .toXml();
        assertThat(execution.getAttribute("groupId"), is("com.acme"));
        assertThat(execution.getAttribute("artifactId"), is("my-plugin"));
        assertThat(execution.getAttribute("version"), is("1.0"));
        assertThat(execution.getAttribute("goal"), is("do-something"));
        assertThat(execution.getAttribute("id"), is("default-do-something"));
    }

    @Test
    void testMatch() {
        ExecutionEntry entry = new ExecutionEntry(
                "com.acme",
                "my-plugin",
                "1.0",
                "do-something",
                "default-do-something",
                null);
        assertThat(entry.match(null, null), is(true));
        assertThat(entry.match(List.of(), null), is(true));
        assertThat(entry.match(null, List.of("*")), is(false));
        assertThat(entry.match(List.of("foo*"), null), is(false));
        assertThat(entry.match(List.of("com.acme:my-plugin:1.0:do-something@default-do-something"), null), is(true));
        assertThat(entry.match(List.of("*@default-do-something"), null), is(true));
        assertThat(entry.match(List.of("*:*:*:*@*"), null), is(true));
        assertThat(entry.match(List.of("com.acme*"), null), is(true));
        assertThat(entry.match(null, List.of("com.acme:my-plugin:1.0:do-something@default-do-something")), is(false));
        assertThat(entry.match(null, List.of("*@default-do-something")), is(false));
        assertThat(entry.match(null, List.of("*:*:*:*@*")), is(false));
        assertThat(entry.match(null, List.of("com.acme*")), is(false));
    }
}
