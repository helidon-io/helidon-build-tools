package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.Optional;

public class DoneFlowState extends FlowState {

    private final ASTNode result;

    DoneFlowState(ASTNode result) {
        this.result = result;
    }

    @Override
    Optional<ASTNode> result() {
        return Optional.ofNullable(result);
    }

    @Override
    void build(ContextAST context) {

    }

    @Override
    FlowStateEnum type() {
        return FlowStateEnum.DONE;
    }
}
