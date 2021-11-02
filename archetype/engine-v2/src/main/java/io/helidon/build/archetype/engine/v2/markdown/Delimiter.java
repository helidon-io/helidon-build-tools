package io.helidon.build.archetype.engine.v2.markdown;

import java.util.List;

/**
 * Delimiter (emphasis, strong emphasis or custom emphasis).
 */
class Delimiter {

    public final List<Text> characters;
    public final char delimiterChar;
    private final int originalLength;

    // Can open emphasis, see spec.
    private final boolean canOpen;

    // Can close emphasis, see spec.
    private final boolean canClose;

    public Delimiter previous;
    public Delimiter next;

    public Delimiter(List<Text> characters, char delimiterChar, boolean canOpen, boolean canClose, Delimiter previous) {
        this.characters = characters;
        this.delimiterChar = delimiterChar;
        this.canOpen = canOpen;
        this.canClose = canClose;
        this.previous = previous;
        this.originalLength = characters.size();
    }

    public boolean canOpen() {
        return canOpen;
    }

    public boolean canClose() {
        return canClose;
    }

    public int length() {
        return characters.size();
    }

    public int originalLength() {
        return originalLength;
    }

    public Text getOpener() {
        return characters.get(characters.size() - 1);
    }

    public Text getCloser() {
        return characters.get(0);
    }

    public Iterable<Text> getOpeners(int length) {
        if (!(length >= 1 && length <= length())) {
            throw new IllegalArgumentException("length must be between 1 and " + length() + ", was " + length);
        }

        return characters.subList(characters.size() - length, characters.size());
    }

    public Iterable<Text> getClosers(int length) {
        if (!(length >= 1 && length <= length())) {
            throw new IllegalArgumentException("length must be between 1 and " + length() + ", was " + length);
        }

        return characters.subList(0, length);
    }
}
    