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
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.descriptor.Output;

/**
 * Archetype output AST node.
 */
public class OutputAST extends ASTNode implements ConditionalNode {

    OutputAST(ASTNode parent, Location location) {
        super(parent, location);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static OutputAST create(Output outputFrom, ASTNode parent, Location location) {
        OutputAST result = new OutputAST(parent, location);

        LinkedList<Visitable> children = getChildren(outputFrom, result, location);
//        ConditionalNode.addChildren(outputFrom, result, children, location);
        result.children().addAll(children);
        return result;
    }

    private static LinkedList<Visitable> getChildren(Output output, ASTNode parent, Location location) {
        LinkedList<Visitable> result = new LinkedList<>();
        if (output.model() != null) {
//            result.add(ModelAST.create(output.model(), parent, location));
            result.add(
                    ConditionalNode.mapConditional(
                            output.model(),
                            ModelAST.create(output.model(), parent, location),
                            parent,
                            location
                    ));
        }
        result.addAll(transformList(output.transformations(), t -> TransformationAST.create(t, parent, location)));
        result.addAll(output.filesList().stream()
                .map(fs -> ConditionalNode.mapConditional(fs, FileSetsAST.create(fs, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(output.fileList().stream()
                .map(fl -> ConditionalNode.mapConditional(fl, FileSetAST.create(fl, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(output.template().stream()
                .map(t -> ConditionalNode.mapConditional(t, TemplateAST.create(t, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(output.templates().stream()
                .map(t -> ConditionalNode.mapConditional(t, TemplatesAST.create(t, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }
}
