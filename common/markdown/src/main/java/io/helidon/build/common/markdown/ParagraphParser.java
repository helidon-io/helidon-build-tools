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

import java.util.ArrayList;
import java.util.List;

class ParagraphParser implements BlockParser {

    private final Paragraph block = new Paragraph();
    private final List<SourceLine> paragraphLines = new ArrayList<>();

    @Override
    public boolean canHaveLazyContinuationLines() {
        return true;
    }

    @Override
    public Block block() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        if (!state.blank()) {
            return BlockContinue.atIndex(state.index());
        } else {
            return BlockContinue.none();
        }
    }

    @Override
    public void addLine(SourceLine line) {
        paragraphLines.add(line);
    }

    @Override
    public void closeBlock() {
        if (paragraphLines.isEmpty()) {
            block.unlink();
        }
    }

    @Override
    public void parseInlines(InlineParser inlineParser) {
        SourceLines lines = SourceLines.of(paragraphLines);
        if (!lines.isEmpty()) {
            inlineParser.parse(lines, block);
        }
    }

    public SourceLines paragraphLines() {
        return SourceLines.of(paragraphLines);
    }
}
