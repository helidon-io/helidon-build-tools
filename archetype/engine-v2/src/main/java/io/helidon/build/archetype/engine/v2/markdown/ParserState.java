package io.helidon.build.archetype.engine.v2.markdown;

/**
 * State of the parser that is used in block parsers.
 */
interface ParserState {
    /**
     * @return the current source line being parsed (full line)
     */
    SourceLine getLine();

    /**
     * @return the current index within the line (0-based)
     */
    int getIndex();

    /**
     * @return the index of the next non-space character starting from {@link #getIndex()} (may be the same) (0-based)
     */
    int getNextNonSpaceIndex();

    /**
     * @return the indentation in columns (either by spaces or tab stop of 4)
     */
    int getIndent();

    /**
     * @return true if the current line is blank starting from the index
     */
    boolean isBlank();

    /**
     * @return the deepest open block parser
     */
    BlockParser getActiveBlockParser();

}
