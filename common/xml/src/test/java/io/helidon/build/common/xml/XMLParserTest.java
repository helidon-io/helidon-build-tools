/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.helidon.build.common.xml.XMLParser.Event;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link XMLParser}.
 */
class XMLParserTest {

    @Test
    void testParse() throws IOException {
        List<ValueEvent> events = parseEvents(getClass().getResourceAsStream("/test1.xml"));
        Iterator<ValueEvent> it = events.iterator();
        assertThat(it.next(), isEvent(Event.INSTRUCTION, "xml version=\"1.0\" encoding=\"UTF-8\""));
        assertThat(it.next(), isEvent(Event.TEXT, "\n"));
        assertThat(it.next(), isEventRegex(Event.COMMENT, "[\\s\\S]*Copyright[\\s\\S]*"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n"));
        assertThat(it.next(), isEvent(Event.ELT_START, "project"));
        assertThat(it.next(), isEvent(Event.ATTR_NAME, "xmlns"));
        assertThat(it.next(), isEvent(Event.ATTR_VALUE, "http://maven.apache.org/POM/4.0.0"));
        assertThat(it.next(), isEvent(Event.ATTR_NAME, "xmlns:xsi"));
        assertThat(it.next(), isEvent(Event.ATTR_VALUE, "http://www.w3.org/2001/XMLSchema-instance"));
        assertThat(it.next(), isEvent(Event.ATTR_NAME, "xsi:schemaLocation"));
        assertThat(it.next(), isEventRegex(Event.ATTR_VALUE, "[\\w:/\\.-]+ [\\w:/\\.-]+"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.ELT_START, "modelVersion"));
        assertThat(it.next(), isEvent(Event.TEXT, "4.0.0"));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "modelVersion"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.ELT_START, "parent"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n        "));
        assertThat(it.next(), isEvent(Event.ELT_START, "groupId"));
        assertThat(it.next(), isEvent(Event.TEXT, "com.acme"));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "groupId"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n        "));
        assertThat(it.next(), isEvent(Event.ELT_START, "artifactId"));
        assertThat(it.next(), isEvent(Event.TEXT, "foo-parent"));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "artifactId"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n        "));
        assertThat(it.next(), isEvent(Event.ELT_START, "version"));
        assertThat(it.next(), isEvent(Event.TEXT, "1.0.0-SNAPSHOT"));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "version"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "parent"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.ELT_START, "artifactId"));
        assertThat(it.next(), isEvent(Event.TEXT, "foo"));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "artifactId"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.ELT_START, "name"));
        assertThat(it.next(), isEvent(Event.TEXT, "Foo"));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "name"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.INSTRUCTION, "m2e execute onConfiguration,onIncremental"));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.ELT_START, "dependencies"));
        assertThat(it.next(), isEvent(Event.SELF_CLOSE, ""));
        assertThat(it.next(), isEvent(Event.TEXT, "\n    "));
        assertThat(it.next(), isEvent(Event.ELT_START, "build"));
        assertThat(it.next(), isEvent(Event.SELF_CLOSE, ""));
        assertThat(it.next(), isEvent(Event.TEXT, "\n"));
        assertThat(it.next(), isEvent(Event.ELT_CLOSE, "project"));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testEmpty() throws IOException {
        List<ValueEvent> events = parseEvents("");
        assertThat(events, is(empty()));
    }

    @Test
    void testInstructionWithoutContent() throws IOException {
        List<ValueEvent> events = parseEvents("<?abc?>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.INSTRUCTION, "abc"))));
    }

    @Test
    void testInstructionWithContent() throws IOException {
        List<ValueEvent> events = parseEvents("<?abc foo bar?>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.INSTRUCTION, "abc foo bar"))));
    }

    @Test
    void testTextAfterInstruction() throws IOException {
        List<ValueEvent> events = parseEvents("<?abc foo?>\n    <elt>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.INSTRUCTION, "abc foo"),
                new ValueEvent(Event.TEXT, "\n    "),
                new ValueEvent(Event.ELT_START, "elt"))));
    }

    @Test
    void testTextAfterClose() throws IOException {
        List<ValueEvent> events = parseEvents("<elt1>\n    <elt2></elt2>\n    <elt3/>\n    <elt4></elt4></elt1>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt1"),
                new ValueEvent(Event.TEXT, "\n    "),
                new ValueEvent(Event.ELT_START, "elt2"),
                new ValueEvent(Event.ELT_CLOSE, "elt2"),
                new ValueEvent(Event.TEXT, "\n    "),
                new ValueEvent(Event.ELT_START, "elt3"),
                new ValueEvent(Event.SELF_CLOSE, ""),
                new ValueEvent(Event.TEXT, "\n    "),
                new ValueEvent(Event.ELT_START, "elt4"),
                new ValueEvent(Event.ELT_CLOSE, "elt4"),
                new ValueEvent(Event.ELT_CLOSE, "elt1"))));
    }

    @Test
    void testTrailingText() throws IOException {
        List<ValueEvent> events = parseEvents("<elt>foo</elt>BAR");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.TEXT, "foo"),
                new ValueEvent(Event.ELT_CLOSE, "elt"))));
    }

    @Test
    void testSelfClosed() throws IOException {
        List<ValueEvent> events = parseEvents("<elt/>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.SELF_CLOSE, ""))));
    }

    @Test
    void testSelfClosedWithAttributes() throws IOException {
        List<ValueEvent> events = parseEvents("<elt key=\"value\"/>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "key"),
                new ValueEvent(Event.ATTR_VALUE, "value"),
                new ValueEvent(Event.SELF_CLOSE, ""))));
    }

    @Test
    void testElement() throws IOException {
        List<ValueEvent> events = parseEvents("<elt></elt>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ELT_CLOSE, "elt"))));
    }

    @Test
    void testOnlyText() throws IOException {
        List<ValueEvent> events = parseEvents("some text");
        assertThat(events, is(empty()));
    }

    @Test
    void testElementWithText() throws IOException {
        List<ValueEvent> events = parseEvents("<elt>='\"</elt>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.TEXT, "='\""),
                new ValueEvent(Event.ELT_CLOSE, "elt"))));
    }

    @Test
    void testDoubleQuotedAttribute() throws IOException {
        List<ValueEvent> events = parseEvents("<elt key=\"value\">");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "key"),
                new ValueEvent(Event.ATTR_VALUE, "value"))));
    }

    @Test
    void testEmptyDoubleQuotedAttribute() throws IOException {
        List<ValueEvent> events = parseEvents("<elt key=\"\">");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "key"),
                new ValueEvent(Event.ATTR_VALUE, ""))));
    }

    @Test
    void testDoubleQuotedAttributes() throws IOException {
        List<ValueEvent> events = parseEvents("<elt key1=\"value1\" key2=\"value2\">");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "key1"),
                new ValueEvent(Event.ATTR_VALUE, "value1"),
                new ValueEvent(Event.ATTR_NAME, "key2"),
                new ValueEvent(Event.ATTR_VALUE, "value2"))));
    }

    @Test
    void testSingleQuotedAttribute() throws IOException {
        List<ValueEvent> events = parseEvents("<elt key='value'>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "key"),
                new ValueEvent(Event.ATTR_VALUE, "value"))));
    }

    @Test
    void testEmptySingleQuotedAttribute() throws IOException {
        List<ValueEvent> events = parseEvents("<elt key=''>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "key"),
                new ValueEvent(Event.ATTR_VALUE, ""))));
    }

    @Test
    void testSingleQuotedAttributes() throws IOException {
        List<ValueEvent> events = parseEvents("<elt key1='value1' key2='value2'>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "key1"),
                new ValueEvent(Event.ATTR_VALUE, "value1"),
                new ValueEvent(Event.ATTR_NAME, "key2"),
                new ValueEvent(Event.ATTR_VALUE, "value2"))));
    }

    @Test
    void testDoctype() throws IOException {
        List<ValueEvent> events = parseEvents("<!DOCTYPE html SYSTEM \"about:legacy-compat\">");
        assertThat(events, is(List.of(
                new ValueEvent(Event.DOCTYPE, " html SYSTEM \"about:legacy-compat\""))));
    }

    @Test
    void testCdata() throws IOException {
        List<ValueEvent> events = parseEvents("<![CDATA[\nfoo\nbar\n]]>\n");
        assertThat(events, is(List.of(
                new ValueEvent(Event.CDATA, "\nfoo\nbar\n"))));
    }

    @Test
    void testElementWithCdata() throws IOException {
        List<ValueEvent> events = parseEvents("<help><![CDATA[ some help ]]></help>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "help"),
                new ValueEvent(Event.CDATA, " some help "),
                new ValueEvent(Event.ELT_CLOSE, "help"))));
    }

    @Test
    void testComment() throws IOException {
        List<ValueEvent> events = parseEvents("<!--\nfoo\nbar\n-->\n");
        assertThat(events, is(List.of(
                new ValueEvent(Event.COMMENT, "\nfoo\nbar\n"))));
    }

    @Test
    void testDecode() {
        assertThat(XMLParser.decode("&quot;&apos;&lt;&gt;&amp;"), is("\"'<>&"));
    }

    @Test
    void testEncodedAttribute() throws IOException {
        List<ValueEvent> events = parseEvents("<elt a=\"&gt;&gt;\"/>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.ATTR_NAME, "a"),
                new ValueEvent(Event.ATTR_VALUE, ">>"),
                new ValueEvent(Event.SELF_CLOSE, ""))));
    }

    @Test
    void testEncodedText() throws IOException {
        List<ValueEvent> events = parseEvents("<elt>&gt;</elt>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.ELT_START, "elt"),
                new ValueEvent(Event.TEXT, ">"),
                new ValueEvent(Event.ELT_CLOSE, "elt"))));
    }

    @Test
    void testEncodedComment() throws IOException {
        List<ValueEvent> events = parseEvents("<!-- &gt; -->");
        assertThat(events, is(List.of(
                new ValueEvent(Event.COMMENT, " &gt; "))));
    }

    @Test
    void testEncodedCdata() throws IOException {
        List<ValueEvent> events = parseEvents("<![CDATA[ &gt; ]]>");
        assertThat(events, is(List.of(
                new ValueEvent(Event.CDATA, " &gt; "))));
    }

    static List<ValueEvent> parseEvents(String str) throws IOException {
        return parseEvents(new ByteArrayInputStream(str.getBytes(UTF_8)));
    }

    static List<ValueEvent> parseEvents(InputStream is) throws IOException {
        try (XMLParser parser = new XMLParser(is)) {
            List<ValueEvent> events = new ArrayList<>();
            while (parser.hasNext()) {
                events.add(new ValueEvent(parser.next(), parser.value()));
            }
            return events;
        }
    }

    static final class ValueEvent {
        private final Event event;
        private final String value;

        ValueEvent(Event event, String value) {
            this.event = event;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ValueEvent)) {
                return false;
            }
            ValueEvent that = (ValueEvent) o;
            return event == that.event &&
                   Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(event, value);
        }

        @Override
        public String toString() {
            return "ValueEvent{"
                   + "kind=" + event
                   + ", value='" + value + '\''
                   + '}';
        }
    }

    static Matcher<ValueEvent> isEvent(Event kind, String value) {
        return new EventMatcher(kind, value, false);
    }

    static Matcher<ValueEvent> isEventRegex(Event kind, String value) {
        return new EventMatcher(kind, value, true);
    }

    static final class EventMatcher extends TypeSafeMatcher<ValueEvent> {

        private final Event event;
        private final String value;
        private final boolean regex;

        EventMatcher(Event event, String value, boolean regex) {
            this.event = event;
            this.value = value;
            this.regex = regex;
        }

        @Override
        protected boolean matchesSafely(ValueEvent item) {
            return event == item.event
                   && regex ? item.value.matches(value) : Objects.equals(item.value, value);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("ValueEvent{"
                                   + "event=" + event
                                   + ", value='" + regex + '\''
                                   + '}');
        }
    }
}
