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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.helidon.build.javadoc.JavaTokenizer.Keyword;
import io.helidon.build.javadoc.JavaTokenizer.Symbol;
import io.helidon.build.javadoc.JavaTokenizer.Token;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Tests {@link JavaTokenizer}.
 */
class JavaTokenizerTest {

    @Test
    void testMultiLineComments() {
        String src = """
                /*
                foo
                bar
                */
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.comment("\nfoo\nbar\n"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testEndOfLineComment() {
        String src = """
                // foo
                // bar
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.eolComment(" foo"),
                Symbol.eolComment(" bar")
        ));
    }

    @Test
    void testClass() {
        String src = """
                class Foo {
                }
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.keyword(Keyword.CLASS),
                Symbol.token(Token.WHITESPACE),
                Symbol.identifier("Foo"),
                Symbol.token(Token.WHITESPACE),
                Symbol.token(Token.OPEN_CURLY),
                Symbol.token(Token.WHITESPACE),
                Symbol.token(Token.CLOSE_CURLY),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testModule() {
        String src = """
                module com.acme {
                }
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.keyword(Keyword.MODULE),
                Symbol.token(Token.WHITESPACE),
                Symbol.identifier("com"),
                Symbol.token(Token.DOT),
                Symbol.identifier("acme"),
                Symbol.token(Token.WHITESPACE),
                Symbol.token(Token.OPEN_CURLY),
                Symbol.token(Token.WHITESPACE),
                Symbol.token(Token.CLOSE_CURLY),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testSimpleClassWithComments() {
        String src = """
                class/*a*/Foo/*b*/{/*c*/}
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.keyword(Keyword.CLASS),
                Symbol.comment("a"),
                Symbol.identifier("Foo"),
                Symbol.comment("b"),
                Symbol.token(Token.OPEN_CURLY),
                Symbol.comment("c"),
                Symbol.token(Token.CLOSE_CURLY),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testPackage() {
        String src = """
                package com.acme.Foo;
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.keyword(Keyword.PACKAGE),
                Symbol.token(Token.WHITESPACE),
                Symbol.identifier("com"),
                Symbol.token(Token.DOT),
                Symbol.identifier("acme"),
                Symbol.token(Token.DOT),
                Symbol.identifier("Foo"),
                Symbol.token(Token.SEMI_COLON),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testAnnotation() {
        String src = """
                @Foo
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.token(Token.ANNOTATION),
                Symbol.identifier("Foo"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testStringLiteral() {
        String src = """
                "{ \\"foo\\": \\"bar\\"}"
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.stringLiteral("{ \\\"foo\\\": \\\"bar\\\"}"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testStringLiterals() {
        String src = """
                "foo""bar"
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.stringLiteral("foo"),
                Symbol.stringLiteral("bar"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testUTF8StringLiteral() {
        String src = """
                "世界您好"
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.stringLiteral("世界您好"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testEscapedUnicodeStringLiteral() {
        String src = """
                "\\u4E16\\u754C\\u60A8\\597D"
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.stringLiteral("\\u4E16\\u754C\\u60A8\\597D"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testCharLiteral() {
        String src = """
                '\\''
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.charLiteral("\\'"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testCharLiterals() {
        String src = """
                'a''b'
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.charLiteral("a"),
                Symbol.charLiteral("b"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testUTF8CharLiteral() {
        String src = """
                '世'
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.charLiteral("世"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    @Test
    void testEscapedUnicodeChar(){
        String src = """
                '\\u4E16'
                """;
        List<Symbol> symbols = parse(src);
        assertThat(symbols, contains(
                Symbol.charLiteral("\\u4E16"),
                Symbol.token(Token.WHITESPACE)
        ));
    }

    private List<Symbol> parse(String src) {
        JavaTokenizer tokenizer = new JavaTokenizer(new ByteArrayInputStream(src.getBytes(StandardCharsets.UTF_8)));
        Spliterator<Symbol> spliterator = Spliterators.spliteratorUnknownSize(tokenizer, Spliterator.ORDERED);
        Stream<Symbol> stream = StreamSupport.stream(spliterator, false);
        return stream.toList();
    }
}
