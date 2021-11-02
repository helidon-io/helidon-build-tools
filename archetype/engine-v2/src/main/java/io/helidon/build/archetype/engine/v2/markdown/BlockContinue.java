package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Result object for continuing parsing of a block, see static methods for constructors.
 */
class BlockContinue {

    private final int newIndex;
    private final int newColumn;
    private final boolean finalize;

    public BlockContinue(int newIndex, int newColumn, boolean finalize) {
        this.newIndex = newIndex;
        this.newColumn = newColumn;
        this.finalize = finalize;
    }

    public int getNewIndex() {
        return newIndex;
    }

    public int getNewColumn() {
        return newColumn;
    }

    public boolean isFinalize() {
        return finalize;
    }

    public static BlockContinue none() {
        return null;
    }

    public static BlockContinue atIndex(int newIndex) {
        return new BlockContinue(newIndex, -1, false);
    }

    public static BlockContinue atColumn(int newColumn) {
        return new BlockContinue(-1, newColumn, false);
    }

    public static BlockContinue finished() {
        return new BlockContinue(-1, -1, true);
    }
}
