package io.helidon.build.archetype.engine.v2.markdown;

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
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        if (!state.isBlank()) {
            return BlockContinue.atIndex(state.getIndex());
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

    public SourceLines getParagraphLines() {
        return SourceLines.of(paragraphLines);
    }
}
