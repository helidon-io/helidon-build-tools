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

package io.helidon.build.common.markdown;

class FencedCodeBlock extends Block {

    private char fenceChar;
    private int fenceLength;
    private int fenceIndent;

    private String info;
    private String literal;

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public char fenceChar() {
        return fenceChar;
    }

    public void fenceChar(char fenceChar) {
        this.fenceChar = fenceChar;
    }

    public int fenceLength() {
        return fenceLength;
    }

    public void fenceLength(int fenceLength) {
        this.fenceLength = fenceLength;
    }

    public int fenceIndent() {
        return fenceIndent;
    }

    public void fenceIndent(int fenceIndent) {
        this.fenceIndent = fenceIndent;
    }

    public String info() {
        return info;
    }

    public void info(String info) {
        this.info = info;
    }

    public String literal() {
        return literal;
    }

    public void literal(String literal) {
        this.literal = literal;
    }
}
