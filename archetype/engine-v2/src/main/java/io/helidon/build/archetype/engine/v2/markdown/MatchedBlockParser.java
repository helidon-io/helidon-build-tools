package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Open block parser that was last matched during the continue phase. This is different from the currently active
 * block parser, as an unmatched block is only closed when a new block is started.
 */
interface MatchedBlockParser {

    BlockParser getMatchedBlockParser();

    /**
     * Returns the current paragraph lines if the matched block is a paragraph.
     *
     * @return paragraph content or an empty list
     */
    SourceLines getParagraphLines();

}
