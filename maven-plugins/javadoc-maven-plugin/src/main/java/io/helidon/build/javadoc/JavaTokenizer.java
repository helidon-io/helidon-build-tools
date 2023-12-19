/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.javadoc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Java source code tokenizer.
 */
class JavaTokenizer implements AutoCloseable, Iterator<JavaTokenizer.Symbol> {

    /**
     * Token.
     */
    enum Token {
        WHITESPACE(JavaTokenizer::consumeWhiteSpace),
        OPEN_PARENT('('),
        CLOSE_PARENT(')'),
        OPEN_SQUARE('['),
        CLOSE_SQUARE(']'),
        OPEN_CURLY('{'),
        CLOSE_CURLY('}'),
        ANNOTATION('@'),
        SINGLE_QUOTE('\''),
        DOUBLE_QUOTE('"'),
        EOL_COMMENT("//"),
        COMMENT_START("/*"),
        DOT('.'),
        COMMA(','),
        SEMI_COLON(';'),
        EQUALS("=="),
        ASSIGN('='),
        INCREMENT("++"),
        PLUS_EQUAL("+="),
        PLUS('+'),
        DECREMENT("--"),
        MINUS_EQUAL("-="),
        MINUS('-'),
        MULTIPLY_EQUAL("*="),
        MULTIPLY('*'),
        DIVIDE_EQUAL("/="),
        DIVIDE('/'),
        MODULO_EQUAL("%="),
        MODULO('%'),
        LOGICAL_AND("&&"),
        LOGICAL_OR("||"),
        LOGICAL_NOT('!'),
        NOT_EQUAL("!="),
        LOGICAL_XOR('^'),
        BIT_AND_EQUAL("&="),
        BIT_OR_EQUAL("|="),
        XOR_EQUAL("^="),
        SHR_EQUAL(">>="),
        SHL_EQUAL("<<="),
        SHR(">>"),
        SHL("<<"),
        GREATER_EQUAL(">="),
        LOWER_EQUAL("<="),
        GREATER('>'),
        LOWER('<'),
        BIT_OR('|'),
        BIT_AND('&'),
        BIT_COMP('~');

        private interface Type {
            boolean read(JavaTokenizer tokenizer);
        }

        private record CharType(char ch) implements Type {

            @Override
            public boolean read(JavaTokenizer tokenizer) {
                return tokenizer.consumeChar(ch);
            }
        }

        private record StringType(String str) implements Type {

            @Override
            public boolean read(JavaTokenizer tokenizer) {
                return tokenizer.consumeString(str);
            }
        }

        private record FunctionType(Function<JavaTokenizer, Boolean> fn) implements Type {

            @Override
            public boolean read(JavaTokenizer tokenizer) {
                return fn.apply(tokenizer);
            }
        }

        private final Type type;
        private final Symbol symbol;

        Token(Function<JavaTokenizer, Boolean> function) {
            type = new FunctionType(function);
            symbol = new Symbol(Symbol.Type.TOKEN, new Symbol.TokenValue(this));
        }

        Token(char ch) {
            type = new CharType(ch);
            symbol = new Symbol(Symbol.Type.TOKEN, new Symbol.TokenValue(this));
        }

        Token(String str) {
            type = new StringType(str);
            symbol = new Symbol(Symbol.Type.TOKEN, new Symbol.TokenValue(this));
        }

        String text() {
            if (type instanceof CharType t) {
                return String.valueOf(t.ch);
            }
            if (type instanceof StringType t) {
                return t.str;
            }
            return "<" + name() + ">";
        }
    }

    /**
     * Symbol.
     */
    record Symbol(Type type, Value value) {

        /**
         * Create a {@link Type#COMMENT} symbol.
         *
         * @param rawValue raw value
         * @return symbol
         */
        static Symbol comment(String rawValue) {
            return new Symbol(Type.COMMENT, new StringValue(rawValue));
        }

        /**
         * Create a {@link Type#EOL_COMMENT} symbol.
         *
         * @param rawValue raw value
         * @return symbol
         */
        static Symbol eolComment(String rawValue) {
            return new Symbol(Type.EOL_COMMENT, new StringValue(rawValue));
        }

        /**
         * Create a {@link Type#IDENTIFIER} symbol.
         *
         * @param rawValue raw value
         * @return symbol
         */
        static Symbol identifier(String rawValue) {
            return new Symbol(Type.IDENTIFIER, new StringValue(rawValue));
        }

        /**
         * Create a {@link Type#STRING_LITERAL} symbol.
         *
         * @param rawValue raw value
         * @return symbol
         */
        static Symbol stringLiteral(String rawValue) {
            return new Symbol(Type.STRING_LITERAL, new StringValue(rawValue));
        }

        /**
         * Create a {@link Type#CHAR_LITERAL} symbol.
         *
         * @param rawValue raw value
         * @return symbol
         */
        static Symbol charLiteral(String rawValue) {
            return new Symbol(Type.CHAR_LITERAL, new StringValue(rawValue));
        }

        /**
         * Get the {@link Type#KEYWORD} symbol.
         *
         * @param keyword keyword
         * @return symbol
         */
        static Symbol keyword(Keyword keyword) {
            return keyword.symbol;
        }

        /**
         * Get the {@link Type#TOKEN} symbol.
         *
         * @param token token
         * @return symbol
         */
        static Symbol token(Token token) {
            return token.symbol;
        }

        private interface Value {
        }

        private record TokenValue(Token token) implements Value {
        }

        private record StringValue(String rawValue) implements Value {
        }

        private record KeywordValue(Keyword keyword) implements Value {
        }

        /**
         * Symbol type.
         */
        enum Type {
            TOKEN,
            EOL_COMMENT,
            COMMENT,
            IDENTIFIER,
            KEYWORD,
            CHAR_LITERAL,
            STRING_LITERAL
        }

        /**
         * Test if this symbol is a token.
         *
         * @return {@code true} if this symbol is a token
         */
        boolean isToken() {
            return type == Type.TOKEN;
        }

        /**
         * Test if this symbol is {@link Token#DOT}.
         *
         * @return {@code true} if matches
         */
        boolean isDot() {
            return Token.DOT.symbol == this;
        }

        /**
         * Test if this symbol is an identifier.
         *
         * @return {@code true} if this symbol is an identifier.
         */
        boolean isIdentifier() {
            return type == Type.IDENTIFIER;
        }

        /**
         * Test if this symbol is a keyword.
         *
         * @return {@code true} if this symbol is a keyword
         */
        boolean isKeyword() {
            return type == Type.KEYWORD;
        }

        /**
         * Test if this symbol is a contextual keyword.
         *
         * @return {@code true} if this symbol is a contextual keyword
         */
        boolean isContextualKeyword() {
            return isKeyword() && !keyword().isReserved();
        }

        /**
         * Test if this symbol is concrete.
         *
         * @return {@code true} if concrete
         */
        boolean isConcrete() {
            return switch (type) {
                case EOL_COMMENT, COMMENT -> false;
                case TOKEN -> this != Token.WHITESPACE.symbol;
                default -> true;
            };
        }

        /**
         * Get the value as a {@link Token}.
         *
         * @return Token
         */
        Token token() {
            if (value instanceof TokenValue v) {
                return v.token;
            }
            throw new IllegalStateException("Expected a TokenValue but got: " + value);
        }

        /**
         * Get the value as a {@link String}.
         *
         * @return String
         */
        String raw() {
            if (value instanceof StringValue v) {
                return v.rawValue;
            }
            throw new IllegalStateException("Expected a StringValue but got: " + value);
        }

        /**
         * Get the value as a {@link Keyword}.
         *
         * @return Keyword
         */
        Keyword keyword() {
            if (value instanceof KeywordValue v) {
                return v.keyword;
            }
            throw new IllegalStateException("Expected a KeywordValue but got: " + value);
        }

        /**
         * Get the text representation of this symbol.
         *
         * @return text
         */
        String text() {
            return switch (type) {
                case TOKEN -> token().text();
                case KEYWORD -> keyword().text();
                default -> raw();
            };
        }
    }

    /**
     * Java keywords.
     */
    enum Keyword {
        // reserved keywords
        ABSTRACT,
        ASSERT,
        BOOLEAN,
        BREAK,
        BYTE,
        CATCH,
        CHAR,
        CLASS,
        CONST,
        CONTINUE,
        DEFAULT,
        DO,
        DOUBLE,
        ELSE,
        ENUM,
        EXTENDS,
        FALSE,
        FINAL,
        FINALLY,
        FLOAT,
        FOR,
        GOTO,
        IF,
        IMPLEMENTS,
        IMPORT,
        INSTANCEOF,
        INT,
        INTERFACE,
        LONG,
        NATIVE,
        NEW,
        PACKAGE,
        PRIVATE,
        PROTECTED,
        PUBLIC,
        RETURN,
        SHORT,
        STATIC,
        STRICTFP,
        SUPER,
        SWITCH,
        SYNCHRONIZED,
        THIS,
        THROW,
        THROWS,
        TRANSIENT,
        TRUE,
        TRY,
        VOID,
        VOLATILE,
        WHILE,

        // contextual keywords
        EXPORTS(false),
        MODULE(false),
        NON_SEALED(false),
        OPEN(false),
        OPENS(false),
        PERMITS(false),
        PROVIDES(false),
        RECORD(false),
        REQUIRES(false),
        SEALED(false),
        TO(false),
        TRANSITIVE(false),
        USES(false),
        VAR(false),
        WHEN(false),
        WITH(false),
        YIELD(false);

        private final Symbol symbol;
        private final boolean reserved;
        private final String text;

        Keyword() {
            this(true);
        }

        Keyword(boolean reserved) {
            this.symbol = new Symbol(Symbol.Type.KEYWORD, new Symbol.KeywordValue(this));
            this.reserved = reserved;
            this.text = name().toLowerCase().replace('_', '-');
        }

        /**
         * Indicate if the keyword is reserved or contextual.
         *
         * @return {@code true} if reserved, {@code false} if contextual
         */
        boolean isReserved() {
            return reserved;
        }

        /**
         * Get the text representation.
         *
         * @return text
         */
        String text() {
            return text;
        }
    }

    private enum State {
        TOKEN,
        COMMENT,
        EOL_COMMENT,
        NAME,
        STRING_LITERAL,
        CHAR_LITERAL
    }

    private static final List<String> KEYWORDS = Arrays.stream(Keyword.values())
            .map(Keyword::text)
            .toList();

    private final int bufferSize;
    private char[] buf;
    private final Reader reader;
    private boolean eof = false;
    private int limit;
    private int position;
    private int lastPosition;
    private int valuePosition;
    private int lineNo = 1;
    private int charNo = 0;
    private State state = State.TOKEN;
    private Symbol symbol;

    /**
     * Create a new instance.
     *
     * @param is   input stream
     * @param size initial buffer size
     */
    JavaTokenizer(InputStream is, int size) {
        try {
            reader = new InputStreamReader(is);
            bufferSize = size;
            buf = new char[bufferSize];
            limit = reader.read(buf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a new instance.
     *
     * @param is input stream
     */
    JavaTokenizer(InputStream is) {
        this(is, 1024);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Get the current cursor position.
     *
     * @return cursor
     */
    String cursor() {
        return String.format("line: %d, col: %d", lineNo, charNo);
    }

    @Override
    public boolean hasNext() {
        if (symbol == null) {
            symbol = parseNext();
        }
        return symbol != null;
    }

    @Override
    public Symbol next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Symbol next = symbol;
        symbol = null;
        return next;
    }

    /**
     * Get the current symbol.
     *
     * @return current symbol, may be {@code null}
     */
    Symbol peek() {
        return symbol;
    }

    /**
     * Skip the current symbol.
     */
    void skip() {
        symbol = null;
    }

    private Symbol parseNext() {
        while (position < limit && symbol == null) {
            char c = readChar();
            if (c == '\n') {
                lineNo++;
                charNo = 1;
            }
            lastPosition = position;
            switch (state) {
                case TOKEN:
                    boolean foundToken = false;
                    for (Token token : JavaTokenizer.Token.values()) {
                        if (token.type.read(this)) {
                            valuePosition = position;
                            switch (token) {
                                case EOL_COMMENT:
                                    state = State.EOL_COMMENT;
                                    break;
                                case COMMENT_START:
                                    state = State.COMMENT;
                                    break;
                                case SINGLE_QUOTE:
                                    state = State.CHAR_LITERAL;
                                    break;
                                case DOUBLE_QUOTE:
                                    state = State.STRING_LITERAL;
                                    break;
                                default:
                                    symbol = Symbol.token(token);
                            }
                            foundToken = true;
                            break;
                        }
                    }
                    if (!foundToken) {
                        valuePosition = position;
                        state = State.NAME;
                    }
                    break;
                case NAME:
                    if (Character.isJavaIdentifierPart(c)) {
                        if (valuePosition < 0) {
                            valuePosition = lastPosition;
                        }
                        position++;
                    } else {
                        String rawValue = symbolValue();
                        if (KEYWORDS.contains(rawValue)) {
                            Keyword keyword = Keyword.valueOf(rawValue.toUpperCase());
                            symbol = Symbol.keyword(keyword);
                        } else {
                            symbol = Symbol.identifier(rawValue);
                        }
                        state = State.TOKEN;
                    }
                    break;
                case EOL_COMMENT:
                    if (c == '\n') {
                        symbol = Symbol.eolComment(symbolValue());
                        state = State.TOKEN;
                    }
                    position++;
                    break;
                case COMMENT:
                    if (consumeString("*/")) {
                        symbol = Symbol.comment(symbolValue());
                        state = State.TOKEN;
                    } else {
                        position++;
                    }
                    break;
                case CHAR_LITERAL:
                    if (c == '\'') {
                        symbol = Symbol.charLiteral(symbolValue());
                        position++;
                        state = State.TOKEN;
                    } else if (c == '\\') {
                        position++;
                        if (!consumeChar('\'')) {
                            consumeChar('\\');
                        }
                    } else {
                        position++;
                    }
                    break;
                case STRING_LITERAL:
                    if (c == '\"') {
                        symbol = Symbol.stringLiteral(symbolValue());
                        position++;
                        state = State.TOKEN;
                    } else if (c == '\\') {
                        position++;
                        if (!consumeChar('\"')) {
                            consumeChar('\\');
                        }
                    } else {
                        position++;
                    }
                    break;
                default:
                    throw new IllegalStateException(String.format(
                            "State %s not supported at line: %d, char: %d", state, lineNo, charNo));
            }
            charNo += (position - lastPosition);
            if (position >= limit) {
                ensureBuffer(1);
            }
        }
        return symbol;
    }

    private String symbolValue() {
        return String.valueOf(buf, valuePosition, lastPosition - valuePosition);
    }

    private boolean consumeWhiteSpace() {
        char c = readChar();
        if (Character.isWhitespace(c)) {
            position++;
            return true;
        }
        return false;
    }

    private boolean consumeChar(char expected) {
        char actual = readChar();
        if (actual == expected) {
            position++;
            return true;
        }
        return false;
    }

    private boolean consumeString(String expected) {
        String actual = readString(expected.length());
        if (expected.equals(actual)) {
            position += expected.length();
            return true;
        }
        return false;
    }

    private char readChar() {
        if (ensureBuffer(1)) {
            return buf[position];
        }
        return '\0';
    }

    private String readString(int length) {
        if (ensureBuffer(length)) {
            return String.valueOf(buf, position, length);
        }
        return null;
    }

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
