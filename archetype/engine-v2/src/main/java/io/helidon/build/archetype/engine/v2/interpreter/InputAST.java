package io.helidon.build.archetype.engine.v2.interpreter;

import io.helidon.build.archetype.engine.v2.descriptor.Input;

public class InputAST extends ASTNode {

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    public static InputAST from(Input input) {
        //todo implement
        return null;
    }
}
