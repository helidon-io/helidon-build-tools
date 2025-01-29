/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * XML parser.
 */
public final class XMLParser implements AutoCloseable, Iterator<XMLParser.Event> {

    /**
     * Parser event.
     */
    public enum Event {
        /**
         * An element text value.
         */
        TEXT,
        /**
         * Comment.
         */
        COMMENT,
        /**
         * Cdata.
         */
        CDATA,
        /**
         * Doctype.
         */
        DOCTYPE,
        /**
         * Instruction.
         */
        INSTRUCTION,
        /**
         * Element start.
         */
        ELT_START,
        /**
         * Element close.
         */
        ELT_CLOSE,
        /**
         * Element self close.
         */
        SELF_CLOSE,
        /**
         * Attribute name.
         */
        ATTR_NAME,
        /**
         * Attribute value.
         */
        ATTR_VALUE
    }

    /**
     * Replace the known escaped XML entities with their non-escaped form.
     *
     * @param str input string
     * @return new string with replaced values
     */
    public static String decode(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '&') {
                int end = str.indexOf(';', i + 1);
                if (end != -1) {
                    switch (str.substring(i, end + 1)) {
                        case "&amp;":
                            sb.append('&');
                            break;
                        case "&gt;":
                            sb.append('>');
                            break;
                        case "&lt;":
                            sb.append('<');
                            break;
                        case "&apos;":
                            sb.append('\'');
                            break;
                        case "&quot;":
                            sb.append('"');
                            break;
                        default:
                            sb.append(c);
                            continue;
                    }
                    i = end;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private enum Token {
        WHITESPACE(XMLParser::consumeWhiteSpace),
        DOCTYPE("<!DOCTYPE"),
        CDATA("<![CDATA["),
        COMMENT("<!--"),
        INSTRUCTION("<?"),
        CLOSE("</"),
        SELF_CLOSE("/>"),
        LOWER_THAN('<'),
        GREATER_THAN('>'),
        SINGLE_QUOTE("='"),
        DOUBLE_QUOTE("=\""),
        EQUAL('='),
        UNKNOWN(t -> true);

        private final Function<XMLParser, Boolean> reader;

        Token(Function<XMLParser, Boolean> function) {
            reader = function;
        }

        Token(char ch) {
            reader = t -> t.consumeChar(ch);
        }

        Token(String str) {
            reader = t -> t.consumeString(str);
        }
    }

    private enum State {
        TOKEN(null, p -> false, 0),
        TEXT(Event.TEXT, '<', 0),
        DOCTYPE(Event.DOCTYPE, '>', 0),
        SINGLE_QUOTE(Event.ATTR_VALUE, '\'', 1),
        DOUBLE_QUOTE(Event.ATTR_VALUE, '"', 1),
        CDATA(Event.CDATA, "]]>"),
        COMMENT(Event.COMMENT, "-->"),
        INSTRUCTION(Event.INSTRUCTION, "?>"),
        ELEMENT(Event.ELT_START, XMLParser::readEndElement, 0),
        CLOSE(Event.ELT_CLOSE, XMLParser::readEndElement, 0),
        ATTRIBUTE(Event.ATTR_NAME, XMLParser::readEndAttribute, 0);

        private final Event event;
        private final Function<XMLParser, Boolean> reader;
        private final int offset;

        State(Event event, Function<XMLParser, Boolean> reader, int offset) {
            this.event = event;
            this.reader = reader;
            this.offset = offset;
        }

        State(Event event, char c, int offset) {
            this.event = event;
            this.reader = p -> p.readChar(c);
            this.offset = offset;
        }

        State(Event event, String str) {
            this.event = event;
            this.reader = p -> p.readString(str);
            this.offset = str.length() - 1;
        }
    }

    private final int bufferSize;
    private char[] buf;
    private final Reader reader;
    private boolean eof = false;
    private int limit;
    private int position;
    private int lastPosition;
    private int valuePosition;
    private int lineNo = 1;
    private int colNo = 0;
    private State lastState = State.TOKEN;
    private State state = State.TOKEN;
    private Event event;

    /**
     * Create a new instance.
     *
     * @param is   input stream
     * @param size initial buffer size
     * @throws NullPointerException if the given input stream is {@code null}
     */
    public XMLParser(InputStream is, int size) {
        reader = new InputStreamReader(is);
        bufferSize = size;
        buf = new char[bufferSize];
    }

    /**
     * Create a new instance.
     *
     * @param is InputStream containing the content to be parsed
     * @throws NullPointerException if the given InputStream is {@code null}
     */
    public XMLParser(InputStream is) {
        this(is, 1024);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public Event next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Event next = event;
        event = null;
        return next;
    }

    @Override
    public boolean hasNext() {
        while (ensureBuffer(1) && event == null) {
            char c = buf[position];
            if (c == '\n') {
                lineNo++;
                colNo = 1;
            }
            lastPosition = position;
            if (state == State.TOKEN) {
                for (Token token : Token.values()) {
                    if (token.reader.apply(this)) {
                        valuePosition = position;
                        switch (token) {
                            case WHITESPACE:
                                // ignore
                                break;
                            case LOWER_THAN:
                                nextState(State.ELEMENT);
                                break;
                            case GREATER_THAN:
                                nextState(State.TEXT);
                                break;
                            case SELF_CLOSE:
                                event = Event.SELF_CLOSE;
                                nextState(State.TEXT);
                                break;
                            case CLOSE:
                                nextState(State.CLOSE);
                                break;
                            case EQUAL:
                                nextState(State.TOKEN);
                                break;
                            case SINGLE_QUOTE:
                                nextState(State.SINGLE_QUOTE);
                                break;
                            case DOUBLE_QUOTE:
                                nextState(State.DOUBLE_QUOTE);
                                break;
                            case DOCTYPE:
                                nextState(State.DOCTYPE);
                                break;
                            case COMMENT:
                                nextState(State.COMMENT);
                                break;
                            case CDATA:
                                nextState(State.CDATA);
                                break;
                            case INSTRUCTION:
                                nextState(State.INSTRUCTION);
                                break;
                            default:
                                switch (lastState) {
                                    case ELEMENT:
                                    case SINGLE_QUOTE:
                                    case DOUBLE_QUOTE:
                                        nextState(State.ATTRIBUTE);
                                        break;
                                    default:
                                        nextState(State.TEXT);
                                }
                        }
                        break;
                    }
                }
            } else {
                if (state.reader.apply(this)) {
                    position += state.offset;
                    if (state != State.TEXT || lastPosition > valuePosition) {
                        event = state.event;
                    }
                    nextState(State.TOKEN);
                } else {
                    position++;
                }
            }
            colNo += (position - lastPosition);
        }
        return event != null;
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
     * Get the current column number.
     *
     * @return column number
     */
    public int colNumber() {
        return colNo;
    }

    /**
     * Get the current event.
     *
     * @return current event, may be {@code null}
     */
    public Event peek() {
        return event;
    }

    /**
     * Skip the current event.
     */
    public void skip() {
        event = null;
    }

    /**
     * Get the current event value.
     *
     * @return event value
     */
    public String value() {
        if (state == State.TOKEN && lastPosition >= valuePosition) {
            String value = String.valueOf(buf, valuePosition, lastPosition - valuePosition);
            switch (lastState) {
                case SINGLE_QUOTE:
                case DOUBLE_QUOTE:
                case TEXT:
                    return decode(value);
                default:
                    return value;
            }
        }
        return "";
    }

    private void nextState(State nextState) {
        lastState = state;
        state = nextState;
    }

    private boolean readChar(char c) {
        return buf[position] == c;
    }

    private boolean readEndAttribute() {
        char c = buf[position];
        return c == '"'
               || c == '\''
               || c == '>'
               || c == '/'
               || c == '='
               || Character.isWhitespace(c);
    }

    private boolean readEndElement() {
        char c = buf[position];
        return c == '>'
               || c == '/'
               || Character.isWhitespace(c);
    }

    private boolean consumeWhiteSpace() {
        char c = buf[position];
        if (Character.isWhitespace(c)) {
            position++;
            return true;
        }
        return false;
    }

    private boolean consumeChar(char expected) {
        char c = buf[position];
        if (c == expected) {
            position++;
            return true;
        }
        return false;
    }

    private boolean consumeString(String expected) {
        if (readString(expected)) {
            position += expected.length();
            return true;
        }
        return false;
    }

    private boolean readString(String str) {
        if (ensureBuffer(str.length())) {
            return str.equals(String.valueOf(buf, position, str.length()));
        }
        return false;
    }

    @SuppressWarnings("DuplicatedCode")
    private boolean ensureBuffer(int length) {
        int newLimit = position + length;
        if (newLimit > limit) {
            if (eof) {
                return false;
            }
            int offset = limit - valuePosition;
            if (newLimit > buf.length) {
                char[] tmp = new char[buf.length + bufferSize];
                System.arraycopy(buf, valuePosition, tmp, 0, offset);
                buf = tmp;
                limit = offset;
                position -= valuePosition;
                lastPosition -= valuePosition;
                valuePosition = 0;
            }
            try {
                int read = reader.read(buf, offset, buf.length - offset);
                if (read == -1) {
                    eof = true;
                    return false;
                } else {
                    limit = offset + read;
                    return true;
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return true;
    }
}
