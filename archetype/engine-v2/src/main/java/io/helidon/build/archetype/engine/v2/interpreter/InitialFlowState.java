package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v2.descriptor.Context;
import io.helidon.build.archetype.engine.v2.descriptor.Step;

public class InitialFlowState extends FlowState {

    private final Flow flow;

    InitialFlowState(Flow flow) {
        this.flow = flow;
    }

    @Override
    Optional<ASTNode> result() {
        return Optional.empty();
    }

    @Override
    void build(ContextAST context) {
        flow.interpreter().visit(context, null);
        getContexts(flow.entryPoint()).forEach(archContext -> flow.interpreter().visit(archContext, null));
        LinkedList<StepAST> steps = getSteps(flow.entryPoint());
        flow.interpreter().stack().addAll(steps);
        flow.tree().addAll(steps);
        try {
            while (!steps.isEmpty()) {
                flow.interpreter().visit(steps.pop(), flow);
            }
        } catch (WaitForUserInput waitForUserInput) {
            flow.state(new WaitingFlowState(flow));
            return;
        }
        //todo add check for additional steps
        flow.state(new ReadyFlowState(flow));
    }

    private LinkedList<ContextAST> getContexts(ArchetypeDescriptor entryPoint) {
        LinkedList<Context> contexts = entryPoint.contexts() != null ? entryPoint.contexts() : new LinkedList<>();
        return contexts.stream()
                .map(context -> ContextAST.create(context, null, ASTNode.Location.builder().build()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private LinkedList<StepAST> getSteps(ArchetypeDescriptor entryPoint) {
        LinkedList<Step> steps = entryPoint.steps() != null ? entryPoint.steps() : new LinkedList<>();
        if (steps.isEmpty()) {
            throw new InterpreterException("Archetype descriptor does not contain steps");
        }
        return steps.stream()
                .map(step -> StepAST.create(step, null, ASTNode.Location.builder().build()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    FlowStateEnum type() {
        return FlowStateEnum.INITIAL;
    }
}
