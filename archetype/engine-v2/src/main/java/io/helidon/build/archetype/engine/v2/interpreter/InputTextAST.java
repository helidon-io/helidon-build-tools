package io.helidon.build.archetype.engine.v2.interpreter;

public class InputTextAST extends InputNodeAST {

    private final String name;
    private final String placeholder;

    public InputTextAST(String label, String name, String placeholder) {
        super(label);
        this.name = name;
        this.placeholder = placeholder;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

}
