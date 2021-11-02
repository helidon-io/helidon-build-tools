package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Custom delimiter processor.
 * <p>
 * Note that implementations of this need to be thread-safe, the same instance may be used by multiple parsers.
 */
interface DelimiterProcessor {

    /**
     * @return the character that marks the beginning of a delimited node, must not clash with any other special
     * characters
     */
    char getOpeningCharacter();

    /**
     * @return the character that marks the the ending of a delimited node, must not clash with any other special
     * characters. Note that for a symmetric delimiter such as "*", this is the same as the opening.
     */
    char getClosingCharacter();

    /**
     * Minimum number of delimiter characters that are needed to activate this. Must be at least 1.
     */
    int getMinLength();

    /**
     * Process the delimiter runs.
     * <p>
     * The processor can examine the runs and the nodes and decide if it wants to process or not. If not, it should not
     * change any nodes and return 0. If yes, it should do the processing (wrapping nodes, etc) and then return how many
     * delimiters were used.
     * <p>
     * Note that removal (unlinking) of the used delimiter {@link Text} nodes is done by the caller.
     *
     * @param opening the opening delimiter run
     * @param closing the closing delimiter run
     * @return how many delimiters were used; must not be greater than length of either opener or closer
     */
    int process(Delimiter opening, Delimiter closing);

}
