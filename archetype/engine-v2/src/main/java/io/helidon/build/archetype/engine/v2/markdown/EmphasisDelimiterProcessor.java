package io.helidon.build.archetype.engine.v2.markdown;

abstract class EmphasisDelimiterProcessor implements DelimiterProcessor {

    private final char delimiterChar;

    protected EmphasisDelimiterProcessor(char delimiterChar) {
        this.delimiterChar = delimiterChar;
    }

    @Override
    public char getOpeningCharacter() {
        return delimiterChar;
    }

    @Override
    public char getClosingCharacter() {
        return delimiterChar;
    }

    @Override
    public int getMinLength() {
        return 1;
    }

    @Override
    public int process(Delimiter opening, Delimiter closing) {
        // "multiple of 3" rule for internal delimiter runs
        if ((opening.canClose() || closing.canOpen()) &&
                closing.originalLength() % 3 != 0 &&
                (opening.originalLength() + closing.originalLength()) % 3 == 0) {
            return 0;
        }

        int usedDelimiters;
        Node emphasis;
        // calculate actual number of delimiters used from this closer
        if (opening.length() >= 2 && closing.length() >= 2) {
            usedDelimiters = 2;
            emphasis = new StrongEmphasis(String.valueOf(delimiterChar) + delimiterChar);
        } else {
            usedDelimiters = 1;
            emphasis = new Emphasis(String.valueOf(delimiterChar));
        }

        SourceSpans sourceSpans = SourceSpans.empty();
        sourceSpans.addAllFrom(opening.getOpeners(usedDelimiters));

        Text opener = opening.getOpener();
        for (Node node : Nodes.between(opener, closing.getCloser())) {
            emphasis.appendChild(node);
            sourceSpans.addAll(node.getSourceSpans());
        }

        sourceSpans.addAllFrom(closing.getClosers(usedDelimiters));

        emphasis.setSourceSpans(sourceSpans.getSourceSpans());
        opener.insertAfter(emphasis);

        return usedDelimiters;
    }
}
