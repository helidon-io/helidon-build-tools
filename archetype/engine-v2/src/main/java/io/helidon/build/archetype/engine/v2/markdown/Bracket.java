package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Opening bracket for links (<code>[</code>).
 */
class Bracket {

    public final Text node;

    /**
     * The position of the marker for the bracket (<code>[</code>)
     */
    public final Position markerPosition;

    /**
     * The position of the content (after the opening bracket)
     */
    public final Position contentPosition;

    /**
     * Previous bracket.
     */
    public final Bracket previous;

    /**
     * Previous delimiter (emphasis, etc) before this bracket.
     */
    public final Delimiter previousDelimiter;

    /**
     * Whether this bracket is allowed to form a link (also known as "active").
     */
    public boolean allowed = true;

    /**
     * Whether there is an unescaped bracket (opening or closing) anywhere after this opening bracket.
     */
    public boolean bracketAfter = false;

    static public Bracket link(Text node, Position markerPosition, Position contentPosition, Bracket previous,
                               Delimiter previousDelimiter) {
        return new Bracket(node, markerPosition, contentPosition, previous, previousDelimiter);
    }

    private Bracket(Text node, Position markerPosition, Position contentPosition, Bracket previous, Delimiter previousDelimiter) {
        this.node = node;
        this.markerPosition = markerPosition;
        this.contentPosition = contentPosition;
        this.previous = previous;
        this.previousDelimiter = previousDelimiter;
    }
}
