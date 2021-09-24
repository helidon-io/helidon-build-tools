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

import java.util.Optional;

public class WaitingFlowState extends FlowState {

    private final Flow flow;

    WaitingFlowState(Flow flow) {
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
        if (flow.interpreter().unresolvedInputs().isEmpty()) {
            flow.state(new ReadyFlowState(flow));
            return;
        }
        throw new InterpreterException("Script interpreter finished in unexpected state.");
    }

    @Override
    FlowStateEnum type() {
        return FlowStateEnum.WAITING;
    }

    @Override
    boolean canBeGenerated() {
        return flow.interpreter().canBeGenerated();
    }
}
