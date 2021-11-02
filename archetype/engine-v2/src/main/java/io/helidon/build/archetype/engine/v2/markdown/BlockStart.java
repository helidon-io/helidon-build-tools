package io.helidon.build.archetype.engine.v2.markdown;

class BlockStart {

    private final BlockParser[] blockParsers;
    private int newIndex = -1;
    private int newColumn = -1;

    public BlockStart(BlockParser... blockParsers) {
        this.blockParsers = blockParsers;
    }

    public BlockParser[] getBlockParsers() {
        return blockParsers;
    }

    public int getNewIndex() {
        return newIndex;
    }

    public int getNewColumn() {
        return newColumn;
    }

    public BlockStart atIndex(int newIndex) {
        this.newIndex = newIndex;
        return this;
    }

    public static BlockStart of(BlockParser... blockParsers) {
        return new BlockStart(blockParsers);
    }

    public BlockStart atColumn(int newColumn) {
        this.newColumn = newColumn;
        return this;
    }

    public static BlockStart none() {
        return null;
    }

}
