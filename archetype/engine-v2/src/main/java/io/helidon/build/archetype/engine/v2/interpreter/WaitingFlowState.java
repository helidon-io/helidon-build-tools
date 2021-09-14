package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.Optional;

public class WaitingFlowState extends FlowState {

    private final Flow flow;

    WaitingFlowState(Flow flow) {
        this.flow = flow;
    }

    @Override
    Optional<ASTNode> result() {
        return Optional.empty();
    }

    @Override
    void build(ContextAST context) {
        ASTNode lastNode = flow.interpreter().stack().peek();
//        context.parent(lastNode);
//        context.children().forEach(ch->((ContextNodeAST)ch).parent(context));
        flow.interpreter().visit(context, lastNode);
        try {
            while (!flow.interpreter().stack().isEmpty()) {
                lastNode = flow.interpreter().stack().peek();
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
    FlowStateEnum type() {
        return FlowStateEnum.WAITING;
    }
}
