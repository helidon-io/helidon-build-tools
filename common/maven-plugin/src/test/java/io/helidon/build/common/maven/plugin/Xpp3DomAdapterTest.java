/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.plugin;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.xml.XMLElement;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.FunctionalMatchers.hasProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests {@link Xpp3DomAdapter}.
 */
class Xpp3DomAdapterTest {

    @Test
    void testXMLElementContract() throws Exception {
        Xpp3Dom orig = xpp3("""
                <root attr="value">
                    <child name="first">text</child>
                    <child name="second">
                        <nested>nested-text</nested>
                    </child>
                </root>
                """);

        XMLElement root = Xpp3DomAdapter.create(orig);
        assertThat(root, allOf(List.of(
                hasProperty("parent", XMLElement::parent, is(nullValue())),
                hasProperty("name", XMLElement::name, is("root")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("attr", "value"))),
                hasProperty("children", XMLElement::children, contains(List.of(
                        allOf(
                                hasProperty("name", XMLElement::name, is("child")),
                                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "first"))),
                                hasProperty("value", XMLElement::value, is("text"))
                        ),
                        allOf(
                                hasProperty("name", XMLElement::name, is("child")),
                                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "second"))),
                                hasProperty("children", XMLElement::children, contains(List.of(
                                        allOf(
                                                hasProperty("name", XMLElement::name, is("nested")),
                                                hasProperty("value", XMLElement::value, is("nested-text"))
                                        )
                                )))
                        )
                )))
        )));
    }

    @Test
    void testDetach() throws Exception {
        Xpp3Dom orig = xpp3("""
                <root>
                    <child name="first">
                        <nested>nested-text</nested>
                    </child>
                </root>
                """);

        XMLElement root = Xpp3DomAdapter.create(orig);
        XMLElement detached = root.children().get(0).detach();

        assertThat(detached, allOf(List.of(
                hasProperty("parent", XMLElement::parent, is(nullValue())),
                hasProperty("name", XMLElement::name, is("child")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "first"))),
                hasProperty("children", XMLElement::children, contains(List.of(
                        allOf(
                                hasProperty("parent", XMLElement::parent, sameInstance(detached)),
                                hasProperty("name", XMLElement::name, is("nested")),
                                hasProperty("value", XMLElement::value, is("nested-text"))
                        )
                )))
        )));

        XMLElement leaf = root.children().get(0).children().get(0).detach();
        assertThat(leaf, allOf(List.of(
                hasProperty("parent", XMLElement::parent, is(nullValue())),
                hasProperty("name", XMLElement::name, is("nested")),
                hasProperty("value", XMLElement::value, is("nested-text"))
        )));
    }

    @Test
    void testDetachRoot() throws Exception {
        Xpp3Dom raw = xpp3("""
                <root>
                    <child>text</child>
                </root>
                """);

        XMLElement root = Xpp3DomAdapter.create(raw);
        XMLElement detached = root.detach();
        assertThat(detached, sameInstance(root));
    }

    @Test
    void testDetachedNode() throws Exception {
        Xpp3Dom orig = xpp3("""
                <root>
                    <child>
                        <nested>nested-text</nested>
                    </child>
                </root>
                """);
        XMLElement root = Xpp3DomAdapter.create(orig);
        XMLElement detached = root.children().get(0).detach();

        // mutate original
        orig.getChild("child").getChild("nested").setValue("changed");
        orig.getChild("child").addChild(new Xpp3Dom("added"));

        assertThat(detached, allOf(List.of(
                hasProperty("name", XMLElement::name, is("child")),
                hasProperty("children", XMLElement::children, contains(List.of(
                        allOf(
                                hasProperty("name", XMLElement::name, is("nested")),
                                hasProperty("value", XMLElement::value, is("nested-text"))
                        )
                )))
        )));
    }

    static Xpp3Dom xpp3(String xml) throws Exception {
        return Xpp3DomBuilder.build(new StringReader(xml));
    }
}
