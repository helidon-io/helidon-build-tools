package io.helidon.build.archetype.engine.v2.interpreter;

public class InputEnumAST extends InputNodeAST {

    private final String name;
    private final OptionNodeAST option;

    InputEnumAST(String label, String name, OptionNodeAST option) {
        super(label);
        this.name = name;
        this.option = option;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

}
