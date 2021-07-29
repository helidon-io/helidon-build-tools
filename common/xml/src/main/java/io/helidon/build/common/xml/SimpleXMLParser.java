/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simple XML parser.
 */
public final class SimpleXMLParser {

    /**
     * XML Reader.
     */
    public interface Reader {

        /**
         * Receive notification of the start of an element.
         *
         * @param name       the element name
         * @param attributes the element attributes
         * @throws Exception if any error occurs
         */
        default void startElement(String name, Map<String, String> attributes) {
        }

        /**
         * Receive notification of the end of an element.
         *
         * @param name the element name
         * @throws Exception if any error occurs
         */
        default void endElement(String name) {
        }

        /**
         * Receive notification of text data inside an element.
         *
         * @param data the text data
         * @throws Exception if any error occurs
         */
        default void elementText(String data) {
        }

        /**
         * Continue action, can be overridden to stop parsing.
         *
         * @return {@code true} to keep parsing, {@code false} to stop parsing
         */
        default boolean keepParsing() {
            return true;
        }

        /**
         * Validate that a child element has a given name.
         *
         * @param child  expected child name
         * @param parent parent name
         * @param qName  element name to be compared
         * @throws IllegalStateException if the child name does not match qName
         */
        default void validateChild(String child, String parent, String qName) throws IllegalStateException {
            if (!child.equals(qName)) {
                throw new IllegalStateException(String.format(
                        "Invalid child for '%s', node: '%s'", parent, qName));
            }
        }

        /**
         * Read an attribute and fallback to a default value if not present.
         *
         * @param name         attribute name
         * @param qName        element name
         * @param attr         attributes
         * @param defaultValue the fallback value, may be {@code null}
         * @return attribute value, may be {@code null} if fallback is null
         */
        default String readAttribute(String name, String qName, Map<String, String> attr, String defaultValue) {
            String value = attr.get(name);
            if (value == null) {
                return defaultValue;
            }
            return value;
        }

        /**
         * Read a required attribute.
         *
         * @param name  attribute name
         * @param qName element name
         * @param attr  attributes
         * @return attribute value, never {@code null}
         * @throws IllegalStateException if the attribute is not found
         */
        default String readRequiredAttribute(String name, String qName, Map<String, String> attr)
                throws IllegalStateException {

            String value = attr.get(name);
            if (value == null) {
                throw new IllegalStateException(String.format(
                        "Missing required attribute '%s', element: '%s'", name, qName));
            }
            return value;
        }

        /**
         * Read an attribute as a comma separate list.
         *
         * @param name  attribute name
         * @param qName element name
         * @param attr  attributes
         * @return list of values, empty if the attribute is not found
         */
        default List<String> readAttributeList(String name, String qName, Map<String, String> attr) {
            String value = attr.get(name);
            if (value == null) {
                return Collections.emptyList();
            }
            List<String> values = new LinkedList<>();
            Collections.addAll(values, value.split(","));
            return values;
        }
    }

    private enum STATE {
        START,
        PROLOG,
        COMMENT,
        CDATA,
        ELEMENT,
        END_ELEMENT,
        NAME,
        TEXT,
        ATTRIBUTES,
        ATTRIBUTE_VALUE,
        SINGLE_QUOTED_VALUE,
        DOUBLE_QUOTED_VALUE,
    }

    private static final String PROLOG_START = "<?";
    private static final String PROLOG_END = "?>";
    private static final String COMMENT_START = "<!--";
    private static final String COMMENT_END = "-->";
    private static final String ELEMENT_SELF_CLOSE = "/>";
    private static final String CLOSE_MARKUP_START = "</";
    private static final String CDATA_START = "<![CDATA[";
    private static final String CDATA_END = "]]>";
    private static final char MARKUP_START = '<';
    private static final char MARKUP_END = '>';
    private static final char ATTRIBUTE_VALUE = '=';
    private static final char DOUBLE_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char[] ALLOWED_CHARS = new char[]{'_', '.', '-', ':'};

    private final char[] buf = new char[1024];
    private char c;
    private int position;
    private int limit = 0;
    private int lineNo = 1;
    private int charNo = 0;
    private StringBuilder nameBuilder = new StringBuilder();
    private StringBuilder textBuilder = new StringBuilder();
    private StringBuilder attrNameBuilder = new StringBuilder();
    private StringBuilder attrValueBuilder = new StringBuilder();
    private Map<String, String> attributes = new HashMap<>();
    private STATE state = STATE.START;
    private STATE resumeState = null;
    private final LinkedList<String> stack = new LinkedList<>();
    private final Reader reader;
    private final InputStreamReader isr;

    SimpleXMLParser(InputStream is, Reader reader) {
        this.isr = new InputStreamReader(Objects.requireNonNull(is, "input stream is null"));
        this.reader = Objects.requireNonNull(reader, "reader is null");
    }

    /**
     * Parse the content of the given {@link InputStream} instance as XML using the specified reader.
     *
     * @param is     InputStream containing the content to be parsed.
     * @param reader the reader to use.
     * @throws NullPointerException if the given InputStream or Reader is {@code null}
     * @throws IOException          If any IO error occurs
     */
    public static void parse(InputStream is, Reader reader) throws IOException {
        new SimpleXMLParser(is, reader).doParse();
    }

    private void processStart() throws IOException {
        if (hasToken(PROLOG_START)) {
            state = STATE.PROLOG;
            position += PROLOG_START.length();
        } else if (hasToken(MARKUP_START)) {
            state = STATE.ELEMENT;
        } else {
            position++;
        }
    }

    private void processProlog() throws IOException {
        if (hasToken(PROLOG_END)) {
            state = STATE.ELEMENT;
            position += PROLOG_END.length();
        } else {
            position++;
        }
    }

    private void processElement() throws IOException {
        if (hasToken(CDATA_START)) {
            state = STATE.CDATA;
            resumeState = STATE.ELEMENT;
            position += CDATA_START.length();
        } else if (hasToken(CDATA_END)) {
            state = STATE.END_ELEMENT;
            position +=  CDATA_END.length();
        } else if (hasToken(COMMENT_START)) {
            state = STATE.COMMENT;
            resumeState = STATE.ELEMENT;
            position += COMMENT_START.length();
        } else if (hasToken(CLOSE_MARKUP_START)) {
            state = STATE.END_ELEMENT;
            position++;
        } else if (hasToken(MARKUP_START)) {
            resumeState = STATE.ATTRIBUTES;
            state = STATE.NAME;
            position++;
        } else if (Character.isWhitespace(c)) {
            position++;
        } else {
            state = STATE.TEXT;
            textBuilder.append(c);
            position++;
        }
    }

    private void processName() throws IOException {
        if (hasToken(MARKUP_END) || hasToken(ELEMENT_SELF_CLOSE)) {
            state = resumeState;
        } else if (Character.isWhitespace(c)) {
            position++;
            state = resumeState;
        } else {
            validateNameChar(c, nameBuilder.length() == 0);
            position++;
            nameBuilder.append(c);
        }
    }

    private void processEndElement() {
        if (Character.isWhitespace(c)) {
            position++;
        } else if (hasToken(MARKUP_END)) {
            String name = nameBuilder.toString();
            if (stack.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "Missing opening element: %s", name));
            }
            String parentName = stack.pop();
            if (!name.equals(parentName)) {
                throw new IllegalStateException(String.format(
                        "Invalid closing element: %s, expecting: %s, line: %d, char: %d",
                        name, parentName, lineNo, charNo));
            }
            position++;
            state = STATE.ELEMENT;
            reader.elementText(decode(textBuilder.toString()));
            reader.endElement(name);
            nameBuilder = new StringBuilder();
            textBuilder = new StringBuilder();
        } else {
            resumeState = STATE.END_ELEMENT;
            state = STATE.NAME;
            position++;
        }
    }

    private void processAttributes() throws IOException {
        if (Character.isWhitespace(c)) {
            position++;
        } else if (hasToken(ELEMENT_SELF_CLOSE)) {
            position += ELEMENT_SELF_CLOSE.length();
            state = STATE.ELEMENT;
            String name = nameBuilder.toString();
            reader.startElement(name, attributes);
            reader.endElement(name);
            nameBuilder = new StringBuilder();
            attributes = new HashMap<>();
        } else if (hasToken(MARKUP_END)) {
            position++;
            state = STATE.ELEMENT;
            String name = nameBuilder.toString();
            stack.push(name);
            reader.startElement(name, attributes);
            nameBuilder = new StringBuilder();
            attributes = new HashMap<>();
        } else if (hasToken(ATTRIBUTE_VALUE)) {
            position++;
            state = STATE.ATTRIBUTE_VALUE;
        } else {
            position++;
            validateNameChar(c, attrNameBuilder.length() == 0);
            attrNameBuilder.append(c);
        }
    }

    private void processAttributeValue() {
        if (Character.isWhitespace(c)) {
            position++;
        } else if (hasToken(SINGLE_QUOTE)) {
            position++;
            state = STATE.SINGLE_QUOTED_VALUE;
        } else if (hasToken(DOUBLE_QUOTE)) {
            position++;
            state = STATE.DOUBLE_QUOTED_VALUE;
        } else {
            throw new IllegalStateException(String.format(
                    "Invalid state, line: %d, char: %d", lineNo, charNo));
        }
    }

    private void processSingleQuoteValue() {
        if (hasToken(SINGLE_QUOTE)) {
            position++;
            state = STATE.ATTRIBUTES;
            attributes.put(attrNameBuilder.toString(), decode(attrValueBuilder.toString()));
            attrNameBuilder = new StringBuilder();
            attrValueBuilder = new StringBuilder();
        } else {
            validateAttrValueChar(c);
            position++;
            attrValueBuilder.append(c);
        }
    }

    private void processDoubleQuotedValue() {
        if (hasToken(DOUBLE_QUOTE)) {
            position++;
            state = STATE.ATTRIBUTES;
            attributes.put(attrNameBuilder.toString(), decode(attrValueBuilder.toString()));
            attrNameBuilder = new StringBuilder();
            attrValueBuilder = new StringBuilder();
        } else if (hasToken(SINGLE_QUOTE)) {
            throw new IllegalStateException(String.format(
                    "Invalid single quote, attribute=%s, name=%s, line: %d, char: %d",
                    attrNameBuilder, nameBuilder, lineNo, charNo));
        } else {
            validateAttrValueChar(c);
            position++;
            attrValueBuilder.append(c);
        }
    }

    private void processText() throws IOException {
        if (hasToken(COMMENT_START)) {
            state = STATE.COMMENT;
            resumeState = STATE.TEXT;
            position += COMMENT_START.length();
        } else if (hasToken(MARKUP_START)) {
            state = STATE.ELEMENT;
        } else {
            textBuilder.append(buf[position]);
            position++;
        }
    }

    private void processComment() throws IOException {
        if (hasToken(COMMENT_END)) {
            state = resumeState;
            position += COMMENT_END.length();
        } else {
            position++;
        }
    }

    private void processCdata() throws IOException {
        if (hasToken(CDATA_END)) {
            state = resumeState;
            position += CDATA_END.length();
        } else {
            textBuilder.append(buf[position]);
            position++;
        }
    }

    private void doParse() throws IOException {
        while (limit >= 0) {
            position = 0;
            limit = isr.read(buf);
            while (position < limit && reader.keepParsing()) {
                c = buf[position];
                if (c == '\n') {
                    lineNo++;
                    charNo = 1;
                }
                int lastPosition = position;
                switch (state) {
                    case START:
                        processStart();
                        break;
                    case PROLOG:
                        processProlog();
                        break;
                    case ELEMENT:
                        processElement();
                        break;
                    case END_ELEMENT:
                        processEndElement();
                        break;
                    case NAME:
                        processName();
                        break;
                    case ATTRIBUTES:
                        processAttributes();
                        break;
                    case ATTRIBUTE_VALUE:
                        processAttributeValue();
                        break;
                    case SINGLE_QUOTED_VALUE:
                        processSingleQuoteValue();
                        break;
                    case DOUBLE_QUOTED_VALUE:
                        processDoubleQuotedValue();
                        break;
                    case TEXT:
                        processText();
                        break;
                    case COMMENT:
                        processComment();
                        break;
                    case CDATA:
                        processCdata();
                        break;
                    default:
                        throw new IllegalStateException(String.format(
                                "Unknown state: %s, line: %d, char: %d", state, lineNo, charNo));
                }
                charNo += (position - lastPosition);
            }
        }
        if (reader.keepParsing()) {
            if (!stack.isEmpty()) {
                throw new IllegalStateException(String.format("Unclosed element: %s", stack.peek()));
            }
            if (state != STATE.ELEMENT) {
                throw new IllegalStateException(String.format("Invalid state: %s", state));
            }
        }
    }

    private void validateAttrValueChar(char c) {
        if (c == MARKUP_START || c == MARKUP_END || c == '&') {
            throw new IllegalStateException(String.format(
                    "Invalid character found in value: '%c', line: %d, char: %d", c, lineNo, charNo));
        }
    }

    private void validateNameChar(char c, boolean firstChar) {
        if (firstChar && !(Character.isLetter(c) || c == '_') || !isAllowedChar(c)) {
            throw new IllegalStateException(String.format(
                    "Invalid character found in name: '%c', line: %d, char: %d", c, lineNo, charNo));
        }
    }

    boolean hasToken(char expected) {
        return position < limit && buf[position] == expected;
    }

    boolean hasToken(CharSequence expected) throws IOException {
        int len = expected.length();
        if (position + len > limit) {
            int offset = limit - position;
            System.arraycopy(buf, position, buf, 0, offset);
            int read = isr.read(buf, offset, buf.length - offset);
            limit = offset + (read == -1  ? 0 : read);
            position = 0;
        }
        return String.valueOf(buf, position, expected.length()).contentEquals(expected);
    }

    private static String decode(String value) {
        return value.replaceAll("&gt;", ">")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&apos;", "'");
    }

    private static boolean isAllowedChar(char c) {
        if (Character.isLetter(c) || Character.isDigit(c)) {
            return true;
        }
        for (char a : ALLOWED_CHARS) {
            if (a == c) {
                return true;
            }
        }
        return false;
    }
}
