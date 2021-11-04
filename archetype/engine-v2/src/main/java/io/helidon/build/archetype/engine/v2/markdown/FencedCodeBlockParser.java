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

package io.helidon.build.archetype.engine.v2.markdown;

import static io.helidon.build.archetype.engine.v2.markdown.Escaping.unescapeString;

class FencedCodeBlockParser implements BlockParser {

    private final FencedCodeBlock block = new FencedCodeBlock();

    private String firstLine;
    private final StringBuilder otherLines = new StringBuilder();

    FencedCodeBlockParser(char fenceChar, int fenceLength, int fenceIndent) {
        block.setFenceChar(fenceChar);
        block.setFenceLength(fenceLength);
        block.setFenceIndent(fenceIndent);
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        int nextNonSpace = state.getNextNonSpaceIndex();
        int newIndex = state.getIndex();
        CharSequence line = state.getLine().getContent();
        if (state.getIndent() < Parsing.codeBlockIndent()
                && nextNonSpace < line.length()
                && line.charAt(nextNonSpace) == block.getFenceChar()
                && isClosing(line, nextNonSpace)
        ) {
            return BlockContinue.finished();
        } else {
            int i = block.getFenceIndent();
            int length = line.length();
            while (i > 0 && newIndex < length && line.charAt(newIndex) == ' ') {
                newIndex++;
                i--;
            }
        }
        return BlockContinue.atIndex(newIndex);
    }

    @Override
    public void addLine(SourceLine line) {
        if (firstLine == null) {
            firstLine = line.getContent().toString();
        } else {
            otherLines.append(line.getContent());
            otherLines.append('\n');
        }
    }

    @Override
    public void closeBlock() {
        block.setInfo(unescapeString(firstLine.trim()));
        block.setLiteral(otherLines.toString());
    }

    public static class Factory implements BlockStartFactory {

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            int indent = state.getIndent();
            if (indent >= Parsing.codeBlockIndent()) {
                return BlockStart.none();
            }

            int nextNonSpace = state.getNextNonSpaceIndex();
            FencedCodeBlockParser blockParser = checkOpener(state.getLine().getContent(), nextNonSpace, indent);
            if (blockParser != null) {
                return BlockStart.of(blockParser).atIndex(nextNonSpace + blockParser.block.getFenceLength());
            } else {
                return BlockStart.none();
            }
        }
    }

    private static FencedCodeBlockParser checkOpener(CharSequence line, int index, int indent) {
        int backticks = 0;
        int tildes = 0;
        int length = line.length();
        loop:
        for (int i = index; i < length; i++) {
            switch (line.charAt(i)) {
                case '`':
                    backticks++;
                    break;
                case '~':
                    tildes++;
                    break;
                default:
                    break loop;
            }
        }
        if (backticks >= 3 && tildes == 0) {
            if (Parsing.find('`', line, index + backticks) != -1) {
                return null;
            }
            return new FencedCodeBlockParser('`', backticks, indent);
        } else if (tildes >= 3 && backticks == 0) {
            return new FencedCodeBlockParser('~', tildes, indent);
        } else {
            return null;
        }
    }

    private boolean isClosing(CharSequence line, int index) {
        char fenceChar = block.getFenceChar();
        int fenceLength = block.getFenceLength();
        int fences = Parsing.skip(fenceChar, line, index, line.length()) - index;
        if (fences < fenceLength) {
            return false;
        }
        int after = Parsing.skipSpaceTab(line, index + fences, line.length());
        return after == line.length();
    }
}
