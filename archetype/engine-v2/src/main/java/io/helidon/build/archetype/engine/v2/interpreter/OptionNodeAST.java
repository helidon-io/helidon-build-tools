package io.helidon.build.archetype.engine.v2.interpreter;

public class OptionNodeAST extends InputNodeAST {

    private final String value;

    OptionNodeAST(String label, String value) {
        super(label);
        this.value = value;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

}
