package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Parser for a specific block node.
 */
interface BlockParser {
    /**
     * Return true if the block that is parsed is a container (contains other blocks), or false if it's a leaf.
     */
    default boolean isContainer(){
        return false;
    }

    /**
     * Return true if the block can have lazy continuation lines.
     */
    default boolean canHaveLazyContinuationLines(){
        return false;
    }

    default boolean canContain(Block childBlock){
        return false;
    }

    Block getBlock();

    BlockContinue tryContinue(ParserState parserState);

    default void addLine(SourceLine line) {
    }

    default void closeBlock() {
    }

    default void parseInlines(InlineParser inlineParser) {
    }
}