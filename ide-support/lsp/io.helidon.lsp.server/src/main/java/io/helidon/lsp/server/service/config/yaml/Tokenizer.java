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

package io.helidon.lsp.server.service.config.yaml;

import java.util.regex.Matcher;

class Tokenizer {

    private final String line;
    private int cursor;

    Tokenizer(String line) {
        this.line = line;
        this.cursor = 0;
    }

    boolean hasNext() {
        return cursor < line.length();
    }

    Token next() {
        if (!hasNext()) {
            return null;
        }
        String current = line.substring(cursor);
        for (Token.Type type : Token.Type.values()) {
            Matcher matcher = type.pattern().matcher(current);
            if (matcher.find()) {
                String value = matcher.group();
                cursor += value.length();
                return new Token(type, value);
            }
        }
        throw new IllegalStateException("Unexpected token - " + current);
    }
}
