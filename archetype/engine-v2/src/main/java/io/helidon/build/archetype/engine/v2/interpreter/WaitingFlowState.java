package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
import java.util.List;

public class WaitingFlowState extends FlowState {

    private final Flow flow;

    public WaitingFlowState(Flow flow) {
        this.flow = flow;
    }

    @Override
    LinkedList<StepAST> tree() {
        return flow.tree();
    }

    @Override
    List<StepAST> results() {
        return List.of();
    }

    @Override
    List<UserInputAST> unresolvedInputs() {
        return flow.interpreter().unresolvedInputs();
    }

    @Override
    LinkedList<StepAST> optionalSteps() {
        //todo implement
        return null;
    }

    @Override
    void build(ContextAST context) {
        flow.interpreter().visit(context, null);
        try {
            while (!flow.interpreter().stack().isEmpty()) {
                ASTNode lastNode = flow.interpreter().stack().peek();
                if (lastNode != null) {
                    lastNode.accept(flow.interpreter(), lastNode.parent());
                } else {
                    throw new InterpreterException("Interpreter does not have nodes to process.");
                }
            }
        } catch (WaitForUserInput waitForUserInput) {
            flow.state(new WaitingFlowState(flow));
            return;
        }
        //todo add check for additional steps
        if (flow.interpreter().unresolvedInputs().isEmpty()) {
            flow.state(new ReadyFlowState(flow));
        }
    }

    @Override
    FlowStateEnum state() {
        return FlowStateEnum.WAITING;
    }
}
