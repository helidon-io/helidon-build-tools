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

import java.util.LinkedList;
import java.util.Queue;

import io.helidon.lsp.server.utils.LspStringUtils;

public class InitialHandler implements Handler {

    private static YamlParser parser;
    private static InitialHandler instance;

    private InitialHandler(YamlParser yamlParser) {
        parser = yamlParser;
    }

    public static InitialHandler INSTANCE(YamlParser yamlParser) {
        if (instance != null && parser != null && parser.equals(yamlParser)) {
            return instance;
        }
        instance = new InitialHandler(yamlParser);
        return instance;
    }

    @Override
    public LineResult process(String line) {
        Tokenizer tokenizer = new Tokenizer(line);
        Token token = tokenizer.next();
        if (token.type() == Token.Type.KEY) {
            Queue<Token> tokens = new LinkedList<>();
            Token keyToken = new Token(
                    token.type(),
                    token.type().pattern().matcher(token.value())
                         .results()
                         .map(matchResult -> matchResult.group(1))
                         .findFirst()
                         .orElseThrow(() -> new IllegalArgumentException(
                                 "Incorrect token value '" + token.value() + "' for token type " + token.type().name()))
            );
            tokens.add(keyToken);
            int indent = LspStringUtils.indentSize(token.value());
            return new LineResult(indent, tokens);
        }
        return null;
    }
}
