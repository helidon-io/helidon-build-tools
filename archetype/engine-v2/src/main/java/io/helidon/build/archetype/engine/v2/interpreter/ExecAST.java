package io.helidon.build.archetype.engine.v2.interpreter;

import io.helidon.build.archetype.engine.v2.descriptor.Exec;

public class ExecAST extends ASTNode {

    private final String src;

    ExecAST(String src) {
        this.src = src;
    }

    public String src() {
        return src;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    public static ExecAST from(Exec exec) {
        //todo implement
        return null;
    }
}
