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
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.descriptor.Output;

/**
 * Archetype output AST node.
 */
public class OutputAST extends ASTNode implements ConditionalNode {

    OutputAST(ASTNode parent, String currentDirectory) {
        super(parent, currentDirectory);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static OutputAST create(Output outputFrom, ASTNode parent, String currentDirectory) {
        OutputAST result = new OutputAST(parent, currentDirectory);

        LinkedList<Visitable> children = getChildren(outputFrom, result, currentDirectory);
        ConditionalNode.addChildren(outputFrom, result, children, currentDirectory);

        return result;
    }

    private static LinkedList<Visitable> getChildren(Output output, ASTNode parent, String currentDirectory) {
        LinkedList<Visitable> result = new LinkedList<>();
        if (output.model() != null) {
            result.add(ModelAST.create(output.model(), parent, currentDirectory));
        }
        result.addAll(output.transformations());
        result.addAll(output.filesList().stream()
                .map(fs -> ConditionalNode.mapConditional(fs, fs, parent, currentDirectory))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(output.fileList().stream()
                .map(fl -> ConditionalNode.mapConditional(fl, fl, parent, currentDirectory))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(output.template().stream()
                .map(t -> ConditionalNode.mapConditional(t, t, parent, currentDirectory))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(output.templates().stream()
                .map(t -> ConditionalNode.mapConditional(t, t, parent, currentDirectory))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }


}
