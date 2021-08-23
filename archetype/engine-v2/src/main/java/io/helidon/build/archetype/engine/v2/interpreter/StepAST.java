package io.helidon.build.archetype.engine.v2.interpreter;

import io.helidon.build.archetype.engine.v2.descriptor.Step;

class StepAST extends ASTNode {

    public static StepAST from(Step step) {
        //todo get content from the argument and transform it to the result
        return null;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

}
