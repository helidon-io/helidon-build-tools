package io.helidon.build.archetype.engine.v2.markdown;

/**
 * A paragraph block, contains inline nodes such as {@link Text}
 */
class Paragraph extends Block {

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
