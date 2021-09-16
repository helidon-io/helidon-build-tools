/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
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
    Optional<Flow.Result> result() {
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
                flow.interpreter().visit(steps.pop(), null);
            }
        } catch (WaitForUserInput waitForUserInput) {
            flow.state(new WaitingFlowState(flow));
            return;
        }
        if (flow.unresolvedInputs().isEmpty()) {
            flow.state(new ReadyFlowState(flow));
            return;
        }
        throw new InterpreterException("Script interpreter finished in unexpected state.");
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
