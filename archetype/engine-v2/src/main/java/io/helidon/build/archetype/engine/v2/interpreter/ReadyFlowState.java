package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.Optional;

public class ReadyFlowState extends FlowState {

    private final Flow flow;

    ReadyFlowState(Flow flow) {
        this.flow = flow;
    }

    @Override
    Optional<ASTNode> result() {
        return Optional.empty();
    }

    @Override
    void build(ContextAST context) {
        ASTNode lastNode = flow.interpreter().stack().peek();
        flow.interpreter().visit(context, lastNode);
        Result result = new Result();

        ContextAST contextAST = new ContextAST();
        contextAST.children().addAll(flow.pathToContextNodeMap().values());
        result.children().add(contextAST);

        flow.tree().forEach(step -> traverseTree(step, result));

        flow.state(new DoneFlowState(result));
    }

    private void traverseTree(ASTNode node, Result result) {
        if (node instanceof OutputAST) {
            result.children().add(node);
        } else {
            node.children().forEach(child -> traverseTree((ASTNode) child, result));
        }
    }

    @Override
    FlowStateEnum type() {
        return FlowStateEnum.READY;
    }
}
