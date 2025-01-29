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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import io.helidon.build.common.xml.XMLElement.XMLElementImpl;
import io.helidon.build.common.xml.XMLParser.Event;

/**
 * XML reader.
 */
public class XMLReader implements AutoCloseable {

    private final XMLParser parser;
    private final String source;

    /**
     * Create a new instance.
     *
     * @param is     input stream
     * @param source source
     */
    public XMLReader(InputStream is, String source) {
        this.parser = new XMLParser(is);
        this.source = source;
    }

    /**
     * Create a new instance.
     *
     * @param is input stream
     */
    public XMLReader(InputStream is) {
        this(is, "unknown");
    }

    /**
     * Get the current line number.
     *
     * @return line number
     */
    public int lineNumber() {
        return parser.lineNumber();
    }

    /**
     * Get the current column number.
     *
     * @return column number
     */
    public int colNumber() {
        return parser.colNumber();
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }

    /**
     * Read a DOM element.
     *
     * @return XMLElement, never null
     * @throws XMLException if an error occurs
     */
    public XMLElement readElement() {
        XMLElement.Builder builder = null;
        XMLElementImpl node = null;
        XMLElementImpl first = null;
        XMLElementImpl last = null;
        StringBuilder sb = new StringBuilder();
        while (parser.hasNext()) {
            Event event = parser.next();
            switch (event) {
                case ELT_START:
                    sb.setLength(0);
                    builder = XMLElement.builder()
                            .parent(builder)
                            .name(parser.value())
                            .attributes(readAttributes());
                    node = new XMLElementImpl(node, builder);
                    if (first == null) {
                        first = node;
                    }
                    if (builder.parent() != null) {
                        builder.parent().children().add(node);
                    }
                    break;
                case SELF_CLOSE:
                case ELT_CLOSE:
                    if (event == Event.ELT_CLOSE) {
                        String name = parser.value();
                        if (node == null || !name.equals(node.name())) {
                            throw unexpectedElement(name);
                        }
                    }
                    if (node != null) {
                        node.value(sb.toString());
                        sb.setLength(0);
                        XMLElementImpl parent = node.parent();
                        if (parent == null) {
                            last = node;
                        }
                        node = parent;
                        builder = builder.parent();
                    }
                    break;
                case CDATA:
                    sb.append(parser.value());
                    break;
                case TEXT:
                    String value = parser.value();
                    if (!value.isBlank()) {
                        sb.append(value);
                    }
                    break;
                default:
            }
        }
        if (last == null || first != last) {
            throw new XMLException("Invalid element");
        }
        return last;
    }

    /**
     * Read attributes.
     *
     * @return attributes
     */
    public Map<String, String> readAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        String key = null;
        while (parser.hasNext()) {
            Event next = parser.peek();
            switch (next) {
                case ATTR_NAME:
                    key = parser.value();
                    parser.skip();
                    break;
                case ATTR_VALUE:
                    attributes.put(key, parser.value());
                    key = null;
                    parser.skip();
                    break;
                default:
                    return attributes;
            }
        }
        throw new XMLException("Unexpected EOF");
    }

    /**
     * Read the next element name.
     *
     * @return name
     */
    public String readName() {
        while (parser.hasNext()) {
            Event event = parser.next();
            if (event == Event.ELT_START) {
                return parser.value();
            }
        }
        throw new XMLException("Unexpected EOF");
    }

    /**
     * Read.
     *
     * @param predicate predicate
     */
    public void read(Predicate<String> predicate) {
        Deque<String> names = new ArrayDeque<>();
        while (parser.hasNext()) {
            Event event = parser.peek();
            String name;
            switch (event) {
                case ELT_START:
                    parser.skip();
                    name = parser.value();
                    names.push(name);
                    if (!predicate.test(name)) {
                        return;
                    }
                    break;
                case SELF_CLOSE:
                case ELT_CLOSE:
                    if (names.isEmpty()) {
                        return;
                    }
                    name = parser.value();
                    String expected = names.pop();
                    if (event == Event.ELT_CLOSE) {
                        if (!name.equals(expected)) {
                            throw unexpectedElement(name);
                        }
                    }
                    parser.skip();
                    break;
                default:
                    parser.skip();
            }
        }
    }

    /**
     * Read text.
     *
     * @return text or {@code null} if empty
     */
    public String readText() {
        boolean cdata = false;
        StringBuilder sb = null;
        while (parser.hasNext()) {
            Event event = parser.peek();
            String value;
            switch (event) {
                case TEXT:
                    parser.skip();
                    value = parser.value();
                    if (!value.isEmpty() && !cdata) {
                        if (sb == null) {
                            sb = new StringBuilder();
                        }
                        sb.append(value);
                    }
                    break;
                case CDATA:
                    parser.skip();
                    value = parser.value();
                    if (!cdata) {
                        sb = new StringBuilder();
                        cdata = true;
                    }
                    sb.append(value);
                    break;
                case SELF_CLOSE:
                case ELT_CLOSE:
                    return sb != null ? sb.toString() : null;
                default:
                    // ignore
            }
        }
        throw new IllegalStateException("Unexpected EOF");
    }

    private XMLException unexpectedElement(String elt) {
        return new XMLException(String.format(
                "Unexpected element: %s, location=%s:%d:%d",
                elt, source, parser.lineNumber(), parser.colNumber()));
    }
}
