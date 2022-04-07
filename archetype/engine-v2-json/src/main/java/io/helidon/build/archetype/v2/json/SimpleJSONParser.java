/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.archetype.v2.json;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import javax.json.JsonValue;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

/**
 * A JSON reader that reads a tree structure.
 */
final class SimpleJSONParser {

    /**
     * JSON reader exception.
     */
    static class JSONReaderException extends RuntimeException {

        /**
         * Create a new JSON reader exception.
         *
         * @param msg   message
         * @param cause cause
         */
        JSONReaderException(String msg, Throwable cause) {
            super(msg, cause);
        }

        /**
         * Create a new JSON reader exception.
         *
         * @param msg message
         */
        JSONReaderException(String msg) {
            super(msg);
        }
    }

    /**
     * Key info.
     */
    enum KeyInfo {

        /**
         * The value for this key is an element name.
         */
        NAME,

        /**
         * The value for this key maps to a JSON object that is not nested under {@link #CHILDREN}.
         */
        OBJECT,

        /**
         * The value for this key is a JSON array of the node children.
         */
        CHILDREN,

        /**
         * Everything else.
         */
        UNKNOWN
    }

    /**
     * Simple reader interface.
     */
    interface Reader {

        /**
         * Start of an element.
         *
         * @param qName      element name (i.e. the value for the key {@code "kind"})
         * @param attributes element attributes (i.e. every key value pair of the enclosing JSON object except
         *                   {@code "kind"} and {@code "children"})
         */
        default void startElement(String qName, Map<String, JsonValue> attributes) {
        }

        /**
         * End of an element.
         *
         * @param qName element name (i.e. the value for the key {@code "kind"})
         */
        default void endElement(String qName) {
        }

        /**
         * Describe a key.
         *
         * @param key key
         * @return State
         */
        KeyInfo keyInfo(String key);
    }

    /**
     * Create a new parser.
     *
     * @param is     InputStream containing the content to be parsed.
     * @param reader the reader to use.
     * @return new parser
     * @throws NullPointerException if the given InputStream or Reader is {@code null}
     */
    static SimpleJSONParser create(InputStream is, Reader reader) {
        return new SimpleJSONParser(JsonFactory.createParser(is), reader);
    }

    /**
     * Get the current line number.
     *
     * @return line number
     */
    public int lineNumber() {
        return (int) parser.getLocation().getLineNumber();
    }

    /**
     * Get the current line character number.
     *
     * @return line character number
     */
    public int charNumber() {
        return (int) parser.getLocation().getColumnNumber();
    }

    private enum State {
        NODE,
        NAME,
        OBJECT,
        CHILDREN,
        UNKNOWN
    }

    private static final class Context {

        private final Map<String, JsonValue> attributes;
        private final State state;
        private String qName;
        private boolean visited;

        Context(State state, String qName, Map<String, JsonValue> attributes) {
            this.state = state;
            this.qName = qName;
            this.attributes = attributes;
        }
    }

    private final Reader reader;
    private final JsonParser parser;
    private final LinkedList<Context> stack = new LinkedList<>();
    private String key;

    private SimpleJSONParser(JsonParser parser, Reader reader) {
        this.parser = parser;
        this.reader = Objects.requireNonNull(reader, "reader is null");
    }

    /**
     * Start parsing.
     */
    void parse() {
        boolean started = false;
        Context ctx = new Context(State.NODE, null, new HashMap<>());
        stack.push(ctx);
        reader.startElement(ctx.qName, ctx.attributes);
        ctx.visited = true;
        while (parser.hasNext()) {
            JsonLocation location = parser.getLocation();
            JsonParser.Event event = parser.next();
            ctx = stack.peek();
            if (ctx == null) {
                throw new JSONReaderException(String.format(
                        "Invalid state. {position=%s:%s}",
                        location.getLineNumber(),
                        location.getColumnNumber()));
            }
            switch (event) {
                case KEY_NAME:
                    key = parser.getString();
                    KeyInfo keyInfo = reader.keyInfo(key);
                    stack.push(new Context(State.valueOf(keyInfo.name()), key, Map.of()));
                    break;
                case START_OBJECT:
                    if (ctx.state == State.OBJECT) {
                        reader.startElement(key, Map.of());
                        ctx.visited = true;
                    } else if (started) {
                        stack.push(new Context(State.NODE, null, new HashMap<>()));
                    } else {
                        started = true;
                    }
                    break;
                case START_ARRAY:
                    if (ctx.state == State.UNKNOWN) {
                        reader.startElement(ctx.qName, ctx.attributes);
                        ctx.visited = true;
                    } else {
                        stack.pop();
                        if (ctx.state == State.CHILDREN) {
                            Context parentCtx = stack.peek();
                            if (parentCtx != null && parentCtx.state == State.NODE) {
                                if (!parentCtx.visited) {
                                    reader.startElement(parentCtx.qName, parentCtx.attributes);
                                    parentCtx.visited = true;
                                }
                            } else {
                                throw new JSONReaderException(String.format(
                                        "Invalid state. {position=%s:%s}",
                                        location.getLineNumber(),
                                        location.getColumnNumber()));
                            }
                        }
                    }
                    break;
                case END_OBJECT:
                    if (!ctx.visited) {
                        reader.startElement(ctx.qName, ctx.attributes);
                    }
                    reader.endElement(ctx.qName);
                    stack.pop();
                    break;
                case END_ARRAY:
                    if (ctx.state != State.NODE) {
                        reader.endElement(ctx.qName);
                        stack.pop();
                    }
                    break;
                default:
                    JsonValue value = parser.getValue();
                    stack.pop();
                    Context parentCtx = stack.peek();
                    if (parentCtx != null && parentCtx.state == State.NODE) {
                        if (ctx.state == State.NAME) {
                            parentCtx.qName = JsonFactory.readString(value);
                        } else {
                            parentCtx.attributes.put(key, value);
                        }
                    } else {
                        throw new JSONReaderException(String.format(
                                "Invalid state. {position=%s:%s}",
                                location.getLineNumber(),
                                location.getColumnNumber()));
                    }
            }
        }
        if (!stack.isEmpty()) {
            throw new JSONReaderException("Invalid state, end of parsing");
        }
    }
}
