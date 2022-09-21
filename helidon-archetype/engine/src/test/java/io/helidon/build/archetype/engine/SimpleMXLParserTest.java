/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Map;

import io.helidon.build.archetype.engine.SimpleXMLParser.Reader;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class SimpleMXLParserTest {

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
        assertThat(reader.bob.value, is("alice"));
        assertThat(reader.bob.attribute, is("name"));
    }

    private static final class Test1Reader implements Reader {

        final LinkedList<String> stack = new LinkedList<>();
        String rootQName;
        String foo;
        BobElement bob = new BobElement();
        String m2e;
        String bar;

        @Override
        public void startElement(String name, Map<String, String> attributes) {
            if (rootQName == null) {
                rootQName = name;
            } else if ("bob".equals(name)){
                attributes.computeIfPresent("attribute", (key, value) -> bob.attribute = value);
            } else if ("bar".equals(name)){
                attributes.computeIfPresent("attribute", (key, value) -> bar = value);
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
                    bob.value = data;
                }
            }
        }

        @Override
        public void processInstructionContent(String data) {
            String target = stack.peek();
            if (target != null) {
                if ("m2e".equals(target)) {
                    m2e = data;
                }
            }
        }
    }

    private static class BobElement {
        String attribute;
        String value;
    }
}
