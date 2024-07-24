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
package io.helidon.build.common.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link XMLElement}.
 */
class XMLElementTest {

    @Test
    void testBuilder() {
        XMLElement elt = XMLElement.builder()
                .name("foo")
                .attributes(Map.of("fk1", "fv1"))
                .child(builder -> builder
                        .name("bar")
                        .attributes(Map.of("bk1", "bv1")))
                .build();
        assertThat(elt.parent(), is(nullValue()));
        assertThat(elt.name(), is("foo"));
        assertThat(elt.attributes(), is(Map.of("fk1", "fv1")));
        assertThat(elt.children().size(), is(1));

        XMLElement child = elt.children().get(0);
        assertThat(child.parent(), is(elt));
        assertThat(child.name(), is("bar"));
        assertThat(child.attributes(), is(Map.of("bk1", "bv1")));
        assertThat(child.children().size(), is(0));
    }

    @Test
    void testParse1() throws IOException {
        XMLElement elt = parse("<foo fk1=\"fv1\"><bar bk1=\"bv1\"></bar></foo>");

        assertThat(elt.parent(), is(nullValue()));
        assertThat(elt.name(), is("foo"));
        assertThat(elt.attributes(), is(Map.of("fk1", "fv1")));
        assertThat(elt.children().size(), is(1));

        XMLElement child = elt.children().get(0);
        assertThat(child.parent(), is(elt));
        assertThat(child.name(), is("bar"));
        assertThat(child.attributes(), is(Map.of("bk1", "bv1")));
        assertThat(child.children().size(), is(0));
    }


    @Test
    void testParse2() throws IOException {
        XMLElement elt = parse("<a><b>c</b><d>e</d></a>");

        assertThat(elt.parent(), is(nullValue()));
        assertThat(elt.name(), is("a"));
        assertThat(elt.attributes(), is(Map.of()));
        assertThat(elt.value(), is(""));
        assertThat(elt.children().size(), is(2));

        XMLElement child1 = elt.children().get(0);
        assertThat(child1.parent(), is(elt));
        assertThat(child1.name(), is("b"));
        assertThat(child1.attributes(), is(Map.of()));
        assertThat(child1.value(), is("c"));
        assertThat(child1.children().size(), is(0));

        XMLElement child2 = elt.children().get(1);
        assertThat(child2.parent(), is(elt));
        assertThat(child2.name(), is("d"));
        assertThat(child2.attributes(), is(Map.of()));
        assertThat(child2.value(), is("e"));
        assertThat(child2.children().size(), is(0));
    }

    @Test
    void testToString() {
        XMLElement elt = XMLElement.builder()
                .name("foo")
                .attributes(Map.of("fk1", "fv1"))
                .child(builder -> builder
                        .name("bar")
                        .attributes(Map.of("bk1", "bv1")))
                .build();
        assertThat(elt.toString(), is("<foo fk1=\"fv1\">\n    <bar bk1=\"bv1\"/>\n</foo>"));
        assertThat(elt.children().get(0).toString(), is("<bar bk1=\"bv1\"/>"));
    }

    @Test
    void testElementWithTextAndChild() throws IOException {
        XMLElement elt = parse("<a>b1\n    <c>d</c>\n</a>");
        assertThat(elt.name(), is("a"));
        assertThat(elt.value(), is("b1\n    "));
        assertThat(elt.children().size(), is(1));
        assertThat(elt.children().get(0).name(), is("c"));
        assertThat(elt.children().get(0).value(), is("d"));
    }

    private static XMLElement parse(String str) throws IOException {
        return XMLElement.parse(new ByteArrayInputStream(str.getBytes()));
    }
}
