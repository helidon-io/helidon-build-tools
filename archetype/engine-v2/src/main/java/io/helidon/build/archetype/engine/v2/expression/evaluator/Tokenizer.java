/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.expression.evaluator;

import java.util.regex.Matcher;

/**
 * Parse the string representation of the expression and extract tokens from it.
 */
public class Tokenizer {

    private String line;
    private int cursor;

    /**
     * Initialize fields of the instance.
     *
     * @param line
     */
    public void init(String line) {
        this.line = line;
        cursor = 0;
    }

    /**
     * Extract next token from the string representation of the expression if possible or {@code null} otherwise.
     *
     * @return next {@code Token} or {@code null}.
     * @throws ParserException if a parsing error occurs.
     */
    public Token getNextToken() throws ParserException {
        if (!hasMoreTokens()) {
            return null;
        }

        var current = line.substring(cursor);

        for (Token.Type type : Token.Type.values()) {
            Matcher matcher = type.getPattern().matcher(current);
            if (matcher.find()) {
                var value = matcher.group();
                cursor += value.length();

                if (type == Token.Type.SKIP) {
                    return getNextToken();
                }

                return new Token(type, value);
            }
        }

        throw new ParserException("unexpected token - " + current);
    }

    /**
     * Test if the string representation of the expression has more Tokens.
     *
     * @return {@code true} if the line has more tokens, {@code false} otherwise.
     */
    public boolean hasMoreTokens() {
        return cursor < line.length();
    }

}
