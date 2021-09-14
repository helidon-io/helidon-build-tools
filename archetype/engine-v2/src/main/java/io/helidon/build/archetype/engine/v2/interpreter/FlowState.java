package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public abstract class FlowState {

    abstract Optional<ASTNode> result();

    abstract void build(ContextAST context);

    abstract FlowStateEnum type();

    class Result extends ASTNode {

        Result() {
            super(null, Location.builder().build());
        }

        @Override
        public <A> void accept(Visitor<A> visitor, A arg) {
            visitor.visit(this, arg);
        }
    }
}
