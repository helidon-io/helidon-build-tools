package io.helidon.build.archetype.engine.v2.markdown;

/**
 * A line or part of a line from the input source.
 */
class SourceLine {

    private final CharSequence content;

    public static SourceLine of(CharSequence content) {
        return new SourceLine(content);
    }

    private SourceLine(CharSequence content) {
        if (content == null) {
            throw new NullPointerException("content must not be null");
        }
        this.content = content;
    }

    public CharSequence getContent() {
        return content;
    }

    public SourceLine substring(int beginIndex, int endIndex) {
        CharSequence newContent = content.subSequence(beginIndex, endIndex);
        return SourceLine.of(newContent);
    }
}
