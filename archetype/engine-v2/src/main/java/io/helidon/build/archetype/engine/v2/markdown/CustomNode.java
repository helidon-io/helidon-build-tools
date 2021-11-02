package io.helidon.build.archetype.engine.v2.markdown;

abstract class CustomNode extends Node {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
