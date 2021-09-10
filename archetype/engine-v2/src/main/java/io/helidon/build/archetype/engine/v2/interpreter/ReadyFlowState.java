package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
import java.util.List;

public class ReadyFlowState extends FlowState {

    private final Flow flow;

    public ReadyFlowState(Flow flow) {
        this.flow = flow;
    }

    @Override
    LinkedList<StepAST> tree() {
        return null;
    }

    @Override
    List<StepAST> results() {
        return null;
    }

    @Override
    List<UserInputAST> unresolvedInputs() {
        return null;
    }

    @Override
    LinkedList<StepAST> optionalSteps() {
        return null;
    }

    @Override
    void build(ContextAST context) {

    }

    @Override
    FlowStateEnum state() {
        return FlowStateEnum.READY;
    }
}
