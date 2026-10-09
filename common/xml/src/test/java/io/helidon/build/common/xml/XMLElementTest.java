/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
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
        assertThat(elt.toString(), is("<foo fk1=\"fv1\">\n    <bar bk1=\"bv1\"/>\n</foo>\n"));
        assertThat(elt.children().get(0).toString(), is("<bar bk1=\"bv1\"/>\n"));
    }

    @Test
    void testVisitNoParent() {
        XMLElement elt = XMLElement.builder().name("foo");
        elt.children().add(XMLElement.builder().name("bar"));

        assertThrows(IllegalStateException.class, () -> elt.visit(new XMLElement.Visitor() {
            @Override
            public boolean visitElement(XMLElement elt) {
                return true;
            }
        }));
    }

    @Test
    void testVisitPrunesRejectedSubtree() {
        XMLElement elt = XMLElement.read("<root>"
                + "<first><first-child/></first>"
                + "<rejected><hidden><hidden-child/></hidden></rejected>"
                + "<last><last-child/></last>"
                + "</root>");
        List<String> callbacks = new ArrayList<>();

        elt.visit(new XMLElement.Visitor() {
            @Override
            public boolean visitElement(XMLElement element) {
                callbacks.add("pre:" + element.name());
                return !"rejected".equals(element.name());
            }

            @Override
            public void postVisitElement(XMLElement element) {
                callbacks.add("post:" + element.name());
            }
        });

        assertThat(callbacks, is(List.of(
                "pre:root",
                "pre:first",
                "pre:first-child",
                "post:first-child",
                "post:first",
                "pre:rejected",
                "post:rejected",
                "pre:last",
                "pre:last-child",
                "post:last-child",
                "post:last",
                "post:root")));
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

        List<String> ones = new ArrayList<>();
        for (XMLElement e : elt.traverse(element -> element.name().endsWith("1"))) {
            ones.add(e.name());
        }
        assertThat(ones, is(List.of("a1", "b1")));
    }

    @Test
    void testTraversePostVisit() {
        XMLElement elt = XMLElement.builder()
                .name("r")
                .child(b -> b.name("a")
                        .child(b1 -> b1.name("a1")
                                .child(b2 -> b2.name("a2"))))
                .child(b -> b.name("b")
                        .child(b1 -> b1.name("b1")
                                .child(b2 -> b2.name("b2"))))
                .build();

        List<String> selected = new ArrayList<>();
        List<String> postVisited = new ArrayList<>();
        for (XMLElement e : elt.traverse(element -> element.name().equals("a") || element.name().endsWith("2"),
                                         element -> postVisited.add(element.name()))) {
            selected.add(e.name());
        }

        assertThat(selected, is(List.of("a", "a2", "b2")));
        assertThat(postVisited, is(List.of("a2", "a1", "a", "b2", "b1", "b", "r")));
    }

    @Test
    void testTraversePattern() {
        XMLElement elt = XMLElement.read("<root>"
                + "<branch>"
                + "<leaf id=\"direct\"><other id=\"prefix\"/></leaf>"
                + "<leaves><leaf id=\"wrapped\"/></leaves>"
                + "<branch><leaves><leaf id=\"nested\"/></leaves></branch>"
                + "<model><leaf id=\"model\"/></model>"
                + "</branch>"
                + "<branches><branch><leaf id=\"sibling\"/></branch></branches>"
                + "<leaf id=\"root\"/>"
                + "</root>");
        Pattern pattern = Pattern.compile(
                "/root/(?:branches/)?branch/(?:(?:branches/)?branch/)*(?:leaves/)?leaf");

        List<String> values = new ArrayList<>();
        for (XMLElement e : elt.traverse(pattern)) {
            values.add(e.attribute("id"));
        }

        assertThat(values, is(List.of("direct", "wrapped", "nested", "sibling")));
    }

    @Test
    void testIndependentIteratorPaths() {
        XMLElement elt = XMLElement.read("<root><branch>"
                + "<leaf>one</leaf>"
                + "<leaf>two</leaf>"
                + "</branch></root>");
        Iterable<XMLElement> traversal = elt.traverse(Pattern.compile("/root/branch/leaf"));
        Iterator<XMLElement> it = traversal.iterator();

        assertThat(it.next().value(), is("one"));

        List<String> second = new ArrayList<>();
        traversal.forEach(element -> second.add(element.value()));
        assertThat(second, is(List.of("one", "two")));

        List<String> first = new ArrayList<>();
        it.forEachRemaining(element -> first.add(element.value()));
        assertThat(first, is(List.of("two")));
    }

    @Test
    void testDetachSingleChild() {
        XMLElement root = XMLElement.read("<root><child><leaf/></child></root>");
        XMLElement child = root.child("child").orElseThrow();
        XMLElement originalLeaf = child.child("leaf").orElseThrow();

        XMLElement detached = child.detach();
        XMLElement detachedLeaf = detached.child("leaf").orElseThrow();

        assertThat(detached.name(), is("child"));
        assertThat(detached.parent(), is(nullValue()));
        assertThat(detachedLeaf.name(), is("leaf"));
        assertThat(detachedLeaf.parent(), is(sameInstance(detached)));
        assertThat(detached, is(not(sameInstance(child))));
        assertThat(detachedLeaf, is(not(sameInstance(originalLeaf))));

        assertThat(root.child("child").orElseThrow(), is(sameInstance(child)));
        assertThat(child.parent(), is(sameInstance(root)));
        assertThat(child.child("leaf").orElseThrow(), is(sameInstance(originalLeaf)));
        assertThat(originalLeaf.parent(), is(sameInstance(child)));
    }

    @Test
    void testLocation() {
        XMLElement root = XMLElement.read("<root>\n    <child/>\n</root>", "test.xml", true);

        assertThat(root.location().toString(), is("test.xml:1:5"));
        assertThat(root.child("child").orElseThrow().location().toString(), is("test.xml:2:12"));
        assertThat(XMLElement.builder().name("elt").location(), is(XMLElement.Location.UNKNOWN));
    }
}
