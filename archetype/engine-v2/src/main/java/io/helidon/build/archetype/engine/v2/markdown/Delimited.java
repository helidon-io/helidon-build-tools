package io.helidon.build.archetype.engine.v2.markdown;

/**
 * A node that uses delimiters in the source form (e.g. <code>*bold*</code>).
 */
interface Delimited {

    /**
     * @return the opening (beginning) delimiter, e.g. <code>*</code>
     */
    String getOpeningDelimiter();

    /**
     * @return the closing (ending) delimiter, e.g. <code>*</code>
     */
    String getClosingDelimiter();
}
