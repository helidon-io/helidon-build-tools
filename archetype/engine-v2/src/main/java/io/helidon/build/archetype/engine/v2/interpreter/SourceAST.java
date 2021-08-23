package io.helidon.build.archetype.engine.v2.interpreter;

import io.helidon.build.archetype.engine.v2.descriptor.Source;

public class SourceAST extends ASTNode {

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    public static SourceAST from(Source source) {
        //todo implement
        return null;
    }
}
