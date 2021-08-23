package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;

public class InputListAST extends InputNodeAST {

    private final String name;
    private final LinkedList<OptionNodeAST> options;

    InputListAST(
            String label, String name, LinkedList<OptionNodeAST> options
    ) {
        super(label);
        this.name = name;
        this.options = options;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

}
