/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link XMLReader}.
 */
class XMLReaderTest {

    @Test
    void testXMLElementSelfClosed() throws IOException {
        XMLElement element = readElement("<elt/>");
        assertThat(element.name(), is("elt"));
    }

    @Test
    void testXMLElement() throws IOException {
        XMLElement element = readElement("<elt></elt>");
        assertThat(element.name(), is("elt"));
    }

    @Test
    void testXMLElementWithText() throws IOException {
        XMLElement element = readElement("<elt>='\"</elt>");
        assertThat(element.name(), is("elt"));
        assertThat(element.value(), is("='\""));
    }

    @Test
    void testXMLElementWithDoubleQuotedAttribute() throws IOException {
        XMLElement element = readElement("<elt key=\"value\"/>");
        assertThat(element.name(), is("elt"));
        assertThat(element.attributes(), is(Map.of("key", "value")));
    }

    @Test
    void testXMLElementWithSingleQuotedAttribute() throws IOException {
        XMLElement element = readElement("<elt key='value'/>");
        assertThat(element.name(), is("elt"));
        assertThat(element.attributes(), is(Map.of("key", "value")));
    }

    @Test
    void testXMLElementWithCdataValue() throws IOException {
        XMLElement element = readElement("<elt><![CDATA[\nfoo\nbar\n]]>\n</elt>");
        assertThat(element.name(), is("elt"));
        assertThat(element.value(), is("\nfoo\nbar\n"));
    }

    @Test
    void testXMLRootParentNull() throws IOException {
        XMLElement elt = readElement("<elt></elt>");
        assertThat(elt.name(), is("elt"));
        assertThat(elt.parent(), is(nullValue()));
    }

    @Test
    void testXMLElementSiblingsWithText() throws IOException {
        XMLElement elt = readElement("<a><b>c</b><d>e</d></a>");
        assertThat(elt.parent(), is(nullValue()));
        assertThat(elt.name(), is("a"));
        assertThat(elt.value(), is(""));
        assertThat(elt.children().size(), is(2));
        assertThat(elt.children().get(0).name(), is("b"));
        assertThat(elt.children().get(0).value(), is("c"));
        assertThat(elt.children().get(1).name(), is("d"));
        assertThat(elt.children().get(1).value(), is("e"));
    }

    @Test
    void testXMLElementWithTextAndChild() throws IOException {
        XMLElement elt = readElement("<a>b1\n    <c>d</c>\n</a>");
        assertThat(elt.name(), is("a"));
        assertThat(elt.value(), is(""));
        assertThat(elt.children().size(), is(1));
        assertThat(elt.children().get(0).name(), is("c"));
        assertThat(elt.children().get(0).value(), is("d"));
    }

    @Test
    void testXMLElementUnexpectedClose() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement("<elt1></elt2></elt1>"));
        assertThat(ex.getMessage(), startsWith("Unexpected element"));
    }

    @Test
    void testXMLElementUnClosed() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement("<elt1><elt2></elt1>"));
        assertThat(ex.getMessage(), startsWith("Unexpected element"));
    }

    @Test
    void testXMLElementUnmatchedClosed() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement("</elt1>"));
        assertThat(ex.getMessage(), startsWith("Unexpected element"));
    }

    @Test
    void testXMLElementNoRoot() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement("<elt1></elt1><elt2></elt2>"));
        assertThat(ex.getMessage(), is("Invalid element"));
    }

    @Test
    void testXMLElementCdataOnly() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement("<![CDATA[\nfoo\nbar\n]]>"));
        assertThat(ex.getMessage(), is("Invalid element"));
    }

    @Test
    void testXMLElementTextOnly() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement("some text"));
        assertThat(ex.getMessage(), is("Invalid element"));
    }

    @Test
    void testXMLElementCommentOnly() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement("<!--\nfoo\nbar\n-->\n"));
        assertThat(ex.getMessage(), is("Invalid element"));
    }

    @Test
    void testXMLElementEmpty() {
        XMLException ex = assertThrows(XMLException.class, () -> readElement(""));
        assertThat(ex.getMessage(), is("Invalid element"));
    }

    // TODO test readText()

    static XMLElement readElement(String str) throws IOException {
        try (XMLReader reader = new XMLReader(new ByteArrayInputStream(str.getBytes(UTF_8)))) {
            return reader.readElement();
        }
    }
}
