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

import io.helidon.build.archetype.engine.v2.descriptor.Step;

/**
 * Archetype step AST node.
 */
public class StepAST extends ASTNode implements ConditionalNode, HelpNode {

    private final String label;
    private final StringBuilder help = new StringBuilder();

    StepAST(String label, String currentDirectory) {
        super(currentDirectory);
        this.label = label;
    }

    /**
     * Get the label.
     *
     * @return label
     */
    public String label() {
        return label;
    }

    @Override
    public String help() {
        return help.toString();
    }

    @Override
    public void addHelp(String help) {
        this.help.append(help);
    }

    static StepAST from(Step step, String currentDirectory) {
        StepAST result = new StepAST(step.label(), currentDirectory);
        LinkedList<Visitable> children = getChildren(step, currentDirectory);
        ConditionalNode.addChildren(step, result, children, currentDirectory);
        return result;
    }

    private static LinkedList<Visitable> getChildren(Step step, String currentDirectory) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(transformList(step.contexts(), c -> ContextAST.from(c, currentDirectory)));
        result.addAll(transformList(step.execs(), ExecAST::from));
        result.addAll(transformList(step.sources(), s -> SourceAST.from(s, currentDirectory)));
        result.addAll(transformList(step.inputs(), i -> InputAST.from(i, currentDirectory)));
        return result;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

}
