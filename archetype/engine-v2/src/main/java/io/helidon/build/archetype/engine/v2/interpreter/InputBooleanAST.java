package io.helidon.build.archetype.engine.v2.interpreter;

public class InputBooleanAST extends InputNodeAST {

    private final String name;
    private final boolean value;

    InputBooleanAST(String label, String name, boolean value) {
        super(label);
        this.name = name;
        this.value = value;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }
}
