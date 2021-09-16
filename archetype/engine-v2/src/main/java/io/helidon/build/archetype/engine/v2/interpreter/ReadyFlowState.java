package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.List;
import java.util.Optional;

public class ReadyFlowState extends FlowState {

    private final Flow flow;
    private final OutputConverterVisitor outputConverterVisitor = new OutputConverterVisitor();

    ReadyFlowState(Flow flow) {
        this.flow = flow;
    }

    @Override
    Optional<Flow.Result> result() {
        return Optional.empty();
    }

    @Override
    void build(ContextAST context) {
        ASTNode lastNode = flow.interpreter().stack().peek();
        flow.interpreter().visit(context, lastNode);
        Flow.Result result = new Flow.Result();

        result.context().putAll(flow.interpreter().pathToContextNodeMap());

        flow.tree().forEach(step -> traverseTree(step, result));

        flow.state(new DoneFlowState(result));
    }

    private void traverseTree(ASTNode node, Flow.Result result) {
        if (node instanceof OutputAST) {
            result.outputs().add(node.accept(outputConverterVisitor, null));
//            if (node.children().size() > 0) {
//                if (node.children().get(0) instanceof IfStatement) {
//                    OutputAST outputAST = new OutputAST(node.parent(), node.location());
//                    List<Visitable> children = ((IfStatement) node.children().get(0)).children();
//                    if (children.size() > 0) {
//                        outputAST.children().addAll(
//                                children
//                        );
//                        result.children().add(outputAST);
//                    }
//                } else {
//                    result.children().add(node);
//                }
//            }
        } else {
            node.children().forEach(child -> traverseTree((ASTNode) child, result));
        }
    }

    @Override
    FlowStateEnum type() {
        return FlowStateEnum.READY;
    }
}
