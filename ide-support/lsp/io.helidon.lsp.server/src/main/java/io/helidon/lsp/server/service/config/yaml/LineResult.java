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

import java.util.Queue;

public class LineResult {

    private final int line;
    private final int indent;
    private final Queue<Token> tokens;

    public LineResult(int line, int indent, Queue<Token> tokens) {
        this.line = line;
        this.indent = indent;
        this.tokens = tokens;
    }

    public int indent() {
        return indent;
    }

    public Queue<Token> tokens() {
        return tokens;
    }

    public int line() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LineResult that = (LineResult) o;

        return line == that.line;
    }

    @Override
    public int hashCode() {
        return line;
    }
}
