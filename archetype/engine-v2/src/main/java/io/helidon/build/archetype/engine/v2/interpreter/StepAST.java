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
public class StepAST extends ASTNode implements ConditionalNode {

    private final String label;
    private String help;

    StepAST(String label, ASTNode parent, Location location) {
        super(parent, location);
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

    /**
     * Get the help.
     *
     * @return help
     */
    public String help() {
        return help;
    }

    /**
     * Set the help content.
     *
     * @param help help content
     */
    public void help(String help) {
        this.help = help;
    }

    static StepAST create(Step stepFrom, ASTNode parent, Location location) {
        StepAST result = new StepAST(stepFrom.label(), parent, location);
        LinkedList<Visitable> children = getChildren(stepFrom, result, location);
        ConditionalNode.addChildren(stepFrom, result, children, location);
        return result;
    }

    private static LinkedList<Visitable> getChildren(Step step, ASTNode parent, Location location) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(transformList(step.contexts(), c -> ContextAST.create(c, parent, location)));
        result.addAll(transformList(step.execs(), e -> ExecAST.create(e, parent, location)));
        result.addAll(transformList(step.sources(), s -> SourceAST.create(s, parent, location)));
        result.addAll(transformList(step.inputs(), i -> InputAST.create(i, parent, location)));
        return result;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

}
