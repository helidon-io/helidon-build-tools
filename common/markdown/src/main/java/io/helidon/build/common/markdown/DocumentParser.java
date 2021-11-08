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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DocumentParser implements ParserState {

    private static final Set<Class<? extends Block>> CORE_FACTORY_TYPES = new LinkedHashSet<>(
            List.of(FencedCodeBlock.class));

    private static final Map<Class<? extends Block>, BlockStartFactory> NODES_TO_CORE_FACTORIES;

    static {
        NODES_TO_CORE_FACTORIES = Map.of(FencedCodeBlock.class, new FencedCodeBlockParser.Factory());
    }

    private SourceLine line;

    /**
     * Line index (0-based).
     */
    private int lineIndex = -1;

    /**
     * current index (offset) in input line (0-based).
     */
    private int index = 0;

    /**
     * current column of input line (tab causes column to go to next 4-space tab stop) (0-based).
     */
    private int column = 0;

    /**
     * if the current column is within a tab character (partially consumed tab).
     */
    private boolean columnIsInTab;

    private int nextNonSpace = 0;
    private int nextNonSpaceColumn = 0;
    private int indent = 0;
    private boolean blank;

    private final List<BlockStartFactory> blockStartFactories;
    private final List<DelimiterProcessor> delimiterProcessors;
    private final DocumentBlockParser documentBlockParser;

    private final List<DocumentParser.OpenBlockParser> openBlockParsers = new ArrayList<>();
    private final List<BlockParser> allBlockParsers = new ArrayList<>();

    DocumentParser(
            List<BlockStartFactory> blockStartFactories,
            List<DelimiterProcessor> delimiterProcessors
    ) {
        this.blockStartFactories = blockStartFactories;
        this.delimiterProcessors = delimiterProcessors;

        this.documentBlockParser = new DocumentBlockParser();
        activateBlockParser(new DocumentParser.OpenBlockParser(documentBlockParser, 0));
    }

    public static Set<Class<? extends Block>> defaultBlockParserTypes() {
        return CORE_FACTORY_TYPES;
    }

    public static List<BlockStartFactory> calculateBlockParserFactories(List<BlockStartFactory> customBlockStartFactories,
                                                                        Set<Class<? extends Block>> enabledBlockTypes) {
        List<BlockStartFactory> list = new ArrayList<>(customBlockStartFactories);
        for (Class<? extends Block> blockType : enabledBlockTypes) {
            list.add(NODES_TO_CORE_FACTORIES.get(blockType));
        }
        return list;
    }

    /**
     * The main parsing function. Returns a parsed document AST.
     */
    public Document parse(String input) {
        int lineStart = 0;
        int lineBreak;
        while ((lineBreak = Parsing.findLineBreak(input, lineStart)) != -1) {
            String line = input.substring(lineStart, lineBreak);
            parseLine(line);
            if (lineBreak + 1 < input.length() && input.charAt(lineBreak) == '\r' && input.charAt(lineBreak + 1) == '\n') {
                lineStart = lineBreak + 2;
            } else {
                lineStart = lineBreak + 1;
            }
        }
        if (input.length() > 0 && (lineStart == 0 || lineStart < input.length())) {
            String line = input.substring(lineStart);
            parseLine(line);
        }

        return finalizeAndProcess();
    }

    public Document parse(Reader input) throws IOException {
        BufferedReader bufferedReader;
        if (input instanceof BufferedReader) {
            bufferedReader = (BufferedReader) input;
        } else {
            bufferedReader = new BufferedReader(input);
        }

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            parseLine(line);
        }

        return finalizeAndProcess();
    }

    @Override
    public SourceLine line() {
        return line;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public int nextNonSpaceIndex() {
        return nextNonSpace;
    }

    @Override
    public int indent() {
        return indent;
    }

    @Override
    public boolean blank() {
        return blank;
    }

    @Override
    public BlockParser activeBlockParser() {
        return openBlockParsers.get(openBlockParsers.size() - 1).blockParser;
    }

    /**
     * Analyze a line of text and update the document appropriately. We parse markdown text by calling this on each
     * line of input, then finalizing the document.
     */
    private void parseLine(CharSequence ln) {
        line(ln);

        int matches = 1;
        for (int i = 1; i < openBlockParsers.size(); i++) {
            DocumentParser.OpenBlockParser openBlockParser = openBlockParsers.get(i);
            BlockParser blockParser = openBlockParser.blockParser;
            findNextNonSpace();

            BlockContinue result = blockParser.tryContinue(this);
            if (result != null) {
                openBlockParser.sourceIndex = index();
                if (result.shouldFinalize()) {
                    closeBlockParsers(openBlockParsers.size() - i);
                    return;
                } else {
                    if (result.newIndex() != -1) {
                        newIndex(result.newIndex());
                    } else if (result.newColumn() != -1) {
                        newColumn(result.newColumn());
                    }
                    matches++;
                }
            } else {
                break;
            }
        }

        int unmatchedBlocks = openBlockParsers.size() - matches;
        BlockParser blockParser = openBlockParsers.get(matches - 1).blockParser;
        boolean startedNewBlock = false;

        int lastIndex = index;

        boolean tryBlockStarts = blockParser.block() instanceof Paragraph || blockParser.container();
        while (tryBlockStarts) {
            lastIndex = index;
            findNextNonSpace();

            if (blank() || (indent < Parsing.codeBlockIndent() && Parsing.letter(this.line.content(), nextNonSpace))) {
                newIndex(nextNonSpace);
                break;
            }

            BlockStart blockStart = findBlockStart(blockParser);
            if (blockStart == null) {
                newIndex(nextNonSpace);
                break;
            }

            startedNewBlock = true;
            int sourceIndex = index();

            if (unmatchedBlocks > 0) {
                closeBlockParsers(unmatchedBlocks);
                unmatchedBlocks = 0;
            }

            if (blockStart.newIndex() != -1) {
                newIndex(blockStart.newIndex());
            } else if (blockStart.newColumn() != -1) {
                newColumn(blockStart.newColumn());
            }

            for (BlockParser newBlockParser : blockStart.blockParsers()) {
                addChild(new DocumentParser.OpenBlockParser(newBlockParser, sourceIndex));
                blockParser = newBlockParser;
                tryBlockStarts = newBlockParser.container();
            }
        }

        if (!startedNewBlock
                && !blank()
                && activeBlockParser().canHaveLazyContinuationLines()
        ) {
            openBlockParsers.get(openBlockParsers.size() - 1).sourceIndex = lastIndex;
            addLine();
        } else {
            if (unmatchedBlocks > 0) {
                closeBlockParsers(unmatchedBlocks);
            }

            if (!blockParser.container()) {
                addLine();
            } else if (!blank()) {
                ParagraphParser paragraphParser = new ParagraphParser();
                addChild(new DocumentParser.OpenBlockParser(paragraphParser, lastIndex));
                addLine();
            }
        }
    }

    private void line(CharSequence ln) {
        lineIndex++;
        index = 0;
        column = 0;
        columnIsInTab = false;

        CharSequence lineContent = Parsing.prepareLine(ln);
        this.line = SourceLine.of(lineContent);
    }

    private void findNextNonSpace() {
        int i = index;
        int cols = column;

        blank = true;
        int length = line.content().length();
        while (i < length) {
            char c = line.content().charAt(i);
            switch (c) {
                case ' ':
                    i++;
                    cols++;
                    continue;
                case '\t':
                    i++;
                    cols += (4 - (cols % 4));
                    continue;
                default:
            }
            blank = false;
            break;
        }

        nextNonSpace = i;
        nextNonSpaceColumn = cols;
        indent = nextNonSpaceColumn - column;
    }

    private void newIndex(int newIndex) {
        if (newIndex >= nextNonSpace) {
            index = nextNonSpace;
            column = nextNonSpaceColumn;
        }
        int length = line.content().length();
        while (index < newIndex && index != length) {
            advance();
        }
        columnIsInTab = false;
    }

    private void newColumn(int newColumn) {
        if (newColumn >= nextNonSpaceColumn) {
            index = nextNonSpace;
            column = nextNonSpaceColumn;
        }
        int length = line.content().length();
        while (column < newColumn && index != length) {
            advance();
        }
        if (column > newColumn) {
            index--;
            column = newColumn;
            columnIsInTab = true;
        } else {
            columnIsInTab = false;
        }
    }

    private void advance() {
        char c = line.content().charAt(index);
        index++;
        if (c == '\t') {
            column += Parsing.columnsToNextTabStop(column);
        } else {
            column++;
        }
    }

    /**
     * Add line content to the active block parser. We assume it can accept lines -- that check should be done before
     * calling this.
     */
    private void addLine() {
        CharSequence content;
        if (columnIsInTab) {
            int afterTab = index + 1;
            CharSequence rest = line.content().subSequence(afterTab, line.content().length());
            int spaces = Parsing.columnsToNextTabStop(column);
            StringBuilder sb = new StringBuilder(spaces + rest.length());
            for (int i = 0; i < spaces; i++) {
                sb.append(' ');
            }
            sb.append(rest);
            content = sb.toString();
        } else if (index == 0) {
            content = line.content();
        } else {
            content = line.content().subSequence(index, line.content().length());
        }
        activeBlockParser().addLine(SourceLine.of(content));
    }

    private BlockStart findBlockStart(BlockParser blockParser) {
        MatchedBlockParser matchedBlockParser = new DocumentParser.MatchedBlockParserImpl(blockParser);
        for (BlockStartFactory blockStartFactory : blockStartFactories) {
            BlockStart result = blockStartFactory.tryStart(this, matchedBlockParser);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Finalize a block. Close it and do any necessary postprocessing, e.g. setting the content of blocks and
     * collecting link reference definitions from paragraphs.
     */
    private void finalize(BlockParser blockParser) {
        blockParser.closeBlock();
    }

    /**
     * Walk through a block & children recursively, parsing string content into inline content where appropriate.
     */
    private void processInlines() {
        InlineParserContext context = new InlineParserContext(delimiterProcessors);
        InlineParser inlineParser = new InlineParser(context);

        for (BlockParser blockParser : allBlockParsers) {
            blockParser.parseInlines(inlineParser);
        }
    }

    /**
     * Add block of type tag as a child of the tip. If the tip can't accept children, close and finalize it and try
     * its parent, and so on until we find a block that can accept children.
     */
    private void addChild(DocumentParser.OpenBlockParser openBlockParser) {
        while (!activeBlockParser().canContain(openBlockParser.blockParser.block())) {
            closeBlockParsers(1);
        }

        activeBlockParser().block().appendChild(openBlockParser.blockParser.block());
        activateBlockParser(openBlockParser);
    }

    private void activateBlockParser(DocumentParser.OpenBlockParser openBlockParser) {
        openBlockParsers.add(openBlockParser);
    }

    private DocumentParser.OpenBlockParser deactivateBlockParser() {
        return openBlockParsers.remove(openBlockParsers.size() - 1);
    }

    private Document finalizeAndProcess() {
        closeBlockParsers(openBlockParsers.size());
        processInlines();
        return documentBlockParser.block();
    }

    private void closeBlockParsers(int count) {
        for (int i = 0; i < count; i++) {
            BlockParser blockParser = deactivateBlockParser().blockParser;
            finalize(blockParser);
            allBlockParsers.add(blockParser);
        }
    }

    private static class MatchedBlockParserImpl implements MatchedBlockParser {

        private final BlockParser matchedBlockParser;

        MatchedBlockParserImpl(BlockParser matchedBlockParser) {
            this.matchedBlockParser = matchedBlockParser;
        }

        @Override
        public BlockParser matchedBlockParser() {
            return matchedBlockParser;
        }

        @Override
        public SourceLines paragraphLines() {
            if (matchedBlockParser instanceof ParagraphParser) {
                ParagraphParser paragraphParser = (ParagraphParser) matchedBlockParser;
                return paragraphParser.paragraphLines();
            }
            return SourceLines.empty();
        }
    }

    private static class OpenBlockParser {
        private final BlockParser blockParser;
        private int sourceIndex;

        OpenBlockParser(BlockParser blockParser, int sourceIndex) {
            this.blockParser = blockParser;
            this.sourceIndex = sourceIndex;
        }
    }
}
