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

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link XMLGenerator}.
 */
class XMLGeneratorTest {

    @Test
    void testEmptyElement() {
        StringWriter buf = new StringWriter();
        XMLGenerator generator = new XMLGenerator(buf, true);
        generator.startElement("foo").endElement();
        assertThat(buf.toString(), is("<foo/>"));
    }

    @Test
    void testProlog() {
        StringWriter buf = new StringWriter();
        XMLGenerator generator = new XMLGenerator(buf, true);
        generator.prolog().startElement("foo").endElement();
        assertThat(buf.toString(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo/>"));
    }

    @Test
    void testEmptyElementWithAttributes() {
        StringWriter buf = new StringWriter();
        XMLGenerator generator = new XMLGenerator(buf, true);
        generator.startElement("foo").attribute("bar", "true").endElement();
        assertThat(buf.toString(), is("<foo bar=\"true\"/>"));
    }

    @Test
    void testElementWithValue() {
        StringWriter buf = new StringWriter();
        XMLGenerator generator = new XMLGenerator(buf, true);
        generator.startElement("foo").value("bar").endElement();
        assertThat(buf.toString(), is("<foo>bar</foo>"));
    }

    @Test
    void testElementWithEmptyChild() {
        StringWriter buf = new StringWriter();
        XMLGenerator generator = new XMLGenerator(buf, true);
        generator.startElement("foo").startElement("bar").endElement().endElement();
        assertThat(buf.toString(), is("<foo>\n    <bar/>\n</foo>"));
    }

    @Test
    void testElementWithChildren() {
        StringWriter buf = new StringWriter();
        XMLGenerator generator = new XMLGenerator(buf, true);
        generator.startElement("foo").startElement("bar").value("true").endElement().endElement();
        assertThat(buf.toString(), is("<foo>\n    <bar>true</bar>\n</foo>"));
    }

    @Test
    void testElementWithTextAndChild() {
        StringWriter buf = new StringWriter();
        XMLGenerator generator = new XMLGenerator(buf, true);
        XMLElement element = XMLElement.builder()
                .name("a").value("b")
                .child(c -> c.name("c").value("d"));
        generator.append(element);
        assertThat(buf.toString(), is("<a>\n    <c>d</c>b</a>"));
    }
}
