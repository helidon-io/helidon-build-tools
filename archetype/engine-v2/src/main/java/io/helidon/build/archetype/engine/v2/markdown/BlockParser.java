package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Parser for a specific block node.
 */
public interface BlockParser {
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

    /**
     * Add a source span of the currently parsed block. The default implementation in {@link AbstractBlockParser} adds
     * it to the block. Unless you have some complicated parsing where you need to check source positions, you don't
     * need to override this.
     *
     * @since 0.16.0
     */
    default void addSourceSpan(SourceSpan sourceSpan) {
        getBlock().addSourceSpan(sourceSpan);
    }

    default void closeBlock() {
    }

    default void parseInlines(InlineParser inlineParser) {
    }
}