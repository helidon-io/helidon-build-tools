/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
import java.util.LinkedList;
import java.util.Map;

import io.helidon.build.common.xml.SimpleXMLParser.Reader;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for class {@link SimpleXMLParser}.
 */
class SimpleMXLParserTest {

    @Test
    void testParseProcessInstruction() throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("test1.xml");
        assertThat(inputStream, is(not(nullValue())));
        Test1Reader reader = new Test1Reader();
        SimpleXMLParser.parse(inputStream, reader);
        assertThat(reader.m2e, is("execute onConfiguration,onIncremental"));
    }

    @Test
    void testParse() throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("test1.xml");
        assertThat(inputStream, is(not(nullValue())));
        Test1Reader reader = new Test1Reader();
        SimpleXMLParser.parse(inputStream, reader);
        assertThat(reader.rootQName, is("document"));
        assertThat(reader.foo, is("bar"));
        assertThat(reader.bob, is("alice"));
    }

    @Test
    void testKeepParsingFalse() throws IOException {
        try {
            SimpleXMLParser.parse(new ByteArrayInputStream("INVALID".getBytes(UTF_8)), new Reader() {
                @Override
                public boolean keepParsing() {
                    return false;
                }
            });
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }

    @Test
    void testPartialParsing() throws IOException {
        try {
            SimpleXMLParser.parse(new ByteArrayInputStream("<foo>bar</foo>BOB".getBytes(UTF_8)), new Reader() {
                boolean keepParsing = true;

                @Override
                public void endElement(String name) {
                    if ("foo".equals(name)) {
                        keepParsing = false;
                    }
                }

                @Override
                public boolean keepParsing() {
                    return keepParsing;
                }
            });
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }

    @Test
    void testCdataParsing() throws IOException {
        try {
            SimpleXMLParser.parse(new ByteArrayInputStream("<help><![CDATA[ some help ]]></help>".getBytes(UTF_8)), new Reader() {

                @Override
                public void startElement(String name, Map<String, String> attributes) {
                    assertThat("help", is(name));
                }

                @Override
                public void elementText(String data) {
                    assertThat(" some help ", is(data));
                }

                @Override
                public void endElement(String name) {
                    assertThat("help", is(name));
                }

            });
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }

    private static final class Test1Reader implements Reader {

        final LinkedList<String> stack = new LinkedList<>();
        String rootQName;
        String foo;
        String bob;
        String m2e;

        @Override
        public void startElement(String name, Map<String, String> attributes) {
            if (rootQName == null) {
                rootQName = name;
            }
            stack.push(name);
        }

        @Override
        public void endElement(String name) {
            stack.pop();
        }

        @Override
        public void elementText(String data) {
            String qName = stack.peek();
            if (qName != null) {
                if ("foo".equals(qName)) {
                    foo = data;
                } else if ("bob".equals(qName)) {
                    bob = data;
                }
            }
        }

        @Override
        public void processingInstruction(String target, String data) {
            if (target != null) {
                if ("m2e".equals(target)) {
                    m2e = data;
                }
            }
        }
    }
}
