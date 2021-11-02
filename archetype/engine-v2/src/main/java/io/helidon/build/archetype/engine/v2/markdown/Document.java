package io.helidon.build.archetype.engine.v2.markdown;

class Document extends Block {

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
