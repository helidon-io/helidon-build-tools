/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void testChildrenByName() {
        XMLElement elt = XMLElement.builder()
                .name("a")
                .child(b -> b.name("b").value("b1"))
                .child(c -> c.name("b").value("b2"))
                .build();
        assertThat(elt.children("b").stream()
                .map(XMLElement::value)
                .collect(Collectors.toList()), is(List.of("b1", "b2")));
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
    void testVisitNoParent() {
        XMLElement elt = XMLElement.builder().name("foo");
        elt.children().add(XMLElement.builder().name("bar"));

        assertThrows(IllegalStateException.class, () -> elt.visit(new XMLElement.Visitor() {
            @Override
            public void visitElement(XMLElement elt) {
                // no-op
            }
        }));
    }

    @Test
    void testTraverse() {
        XMLElement elt = XMLElement.builder()
                .name("r")
                .child(b -> b.name("a")
                        .child(b1 -> b1.name("a1")
                                .child(b2 -> b2.name("a2"))))
                .child(b -> b.name("b")
                        .child(b1 -> b1.name("b1")
                                .child(b2 -> b2.name("b2"))))
                .build();

        List<String> names = new ArrayList<>();
        for (XMLElement e : elt.traverse()) {
            names.add(e.name());
        }
        assertThat(names, is(List.of("r", "a", "a1", "a2", "b", "b1", "b2")));
    }
}
