/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * XML parser.
 */
public final class XMLParser {

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
        PI_TARGET,
        PI_CONTENT
    }

    private static final String PROLOG_START = "<?xml";
    private static final String PROLOG_END = "?>";
    private static final String PI_START = "<?";
    private static final String PI_END = "?>";
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
    private int lastPosition;
    private int limit = 0;
    private int lineNo = 1;
    private int charNo = 0;
    private final StringBuilder nameBuilder = new StringBuilder();
    private final StringBuilder textBuilder = new StringBuilder();
    private final StringBuilder attrNameBuilder = new StringBuilder();
    private final StringBuilder attrValueBuilder = new StringBuilder();
    private Map<String, String> attributes = new HashMap<>();
    private STATE state = STATE.START;
    private STATE resumeState = null;
    private final Deque<Context> stack = new ArrayDeque<>();
    private final XMLReader reader;
    private final InputStreamReader isr;

    XMLParser(InputStream is, XMLReader reader) {
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
    public static void parse(InputStream is, XMLReader reader) throws IOException {
        new XMLParser(is, reader).parse();
    }

    /**
     * Create a new parser.
     *
     * @param is     InputStream containing the content to be parsed.
     * @param reader the reader to use.
     * @return new parser
     * @throws NullPointerException if the given InputStream or Reader is {@code null}
     */
    public static XMLParser create(InputStream is, XMLReader reader) {
        return new XMLParser(is, reader);
    }

    /**
     * Replace XML entities with their non escaped values.
     *
     * @param input string containing XML entities
     * @return new string with replaced values
     */
    public static String processXmlEscapes(String input) {
        return input.replaceAll("&quot;", "\"")
                .replaceAll("&apos", "'")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");
    }

    /**
     * Get the current line number.
     *
     * @return line number
     */
    public int lineNumber() {
        return lineNo;
    }

    /**
     * Get the current line character number.
     *
     * @return line character number
     */
    public int charNumber() {
        return charNo;
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
            position += CDATA_END.length();
        } else if (hasToken(COMMENT_START)) {
            state = STATE.COMMENT;
            resumeState = STATE.ELEMENT;
            position += COMMENT_START.length();
        } else if (hasToken(CLOSE_MARKUP_START)) {
            state = STATE.END_ELEMENT;
            position++;
        } else if (hasToken(PI_START)) {
            resumeState = STATE.PI_CONTENT;
            state = STATE.PI_TARGET;
            position += PI_START.length();
        } else if (hasToken(PI_END)) {
            state = STATE.END_ELEMENT;
            position += PI_END.length();
        } else if (hasToken(MARKUP_START)) {
            resumeState = STATE.ATTRIBUTES;
            state = STATE.NAME;
            position++;
        } else if (Character.isWhitespace(c)) {
            position++;
        } else {
            state = STATE.TEXT;
            context().textBuilder.append(c);
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
            Context ctx = stack.pop();
            if (!name.equals(ctx.name)) {
                throw new IllegalStateException(String.format(
                        "Invalid closing element: %s, expecting: %s, line: %d, char: %d",
                        name, ctx.name, lineNo, charNo));
            }
            position++;
            state = STATE.ELEMENT;
            reader.elementText(decode(ctx.textBuilder.toString()));
            reader.endElement(name);
            nameBuilder.setLength(0);
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
            nameBuilder.setLength(0);
            attributes = new HashMap<>();
        } else if (hasToken(MARKUP_END)) {
            position++;
            state = STATE.ELEMENT;
            String name = nameBuilder.toString();
            stack.push(new Context(name));
            reader.startElement(name, attributes);
            nameBuilder.setLength(0);
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

    private void processQuoteValue(char token) {
        if (hasToken(token)) {
            position++;
            state = STATE.ATTRIBUTES;
            attributes.put(attrNameBuilder.toString(), decode(attrValueBuilder.toString()));
            attrNameBuilder.setLength(0);
            attrValueBuilder.setLength(0);
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
            context().textBuilder.append(buf[position]);
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
            context().textBuilder.append(textBuilder);
            textBuilder.setLength(0);
        } else {
            textBuilder.append(buf[position]);
            position++;
        }
    }

    private void processPIContent() throws IOException {
        if (hasToken(PI_END)) {
            position += PI_END.length();
            state = STATE.ELEMENT;
            String target = nameBuilder.toString();
            reader.startElement(target, attributes);
            reader.processingInstruction(target, decode(textBuilder.toString()));
            reader.endElement(target);
            nameBuilder.setLength(0);
            textBuilder.setLength(0);
        } else {
            position++;
            textBuilder.append(c);
        }
    }

    private void processPITarget() throws IOException {
        if (hasToken(PI_END)) {
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

    private Context context() {
        if (stack.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Invalid state, line: %d, char: %d", lineNo, charNo));
        }
        return stack.peek();
    }

    /**
     * Start parsing.
     *
     * @throws IOException if an IO error occurs
     */
    public void parse() throws IOException {
        while (limit >= 0) {
            position = 0;
            limit = isr.read(buf);
            while (position < limit && reader.keepParsing()) {
                c = buf[position];
                if (c == '\n') {
                    lineNo++;
                    charNo = 1;
                }
                lastPosition = position;
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
                        processQuoteValue(SINGLE_QUOTE);
                        break;
                    case DOUBLE_QUOTED_VALUE:
                        processQuoteValue(DOUBLE_QUOTE);
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
                    case PI_TARGET:
                        processPITarget();
                        break;
                    case PI_CONTENT:
                        processPIContent();
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
        if (c == MARKUP_START || c == MARKUP_END) {
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
            limit = offset + (read == -1 ? 0 : read);
            position = 0;
            lastPosition = 0;
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

    private static final class Context {

        private final String name;
        private final StringBuilder textBuilder = new StringBuilder();

        Context(String name) {
            this.name = name;
        }
    }
}
