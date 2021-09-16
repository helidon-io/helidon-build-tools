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

import io.helidon.build.archetype.engine.v2.descriptor.Model;

/**
 * Archetype model AST node.
 */
public class ModelAST extends ASTNode implements ConditionalNode {

    ModelAST(ASTNode parent, Location location) {
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

    static ModelAST create(Model model, ASTNode parent, Location location) {
        ModelAST result = new ModelAST(parent, location);

        LinkedList<Visitable> children = getChildren(model, result, location);
//        ConditionalNode.addChildren(model, result, children, location);
        result.children().addAll(children);

        return result;
    }

    private static LinkedList<Visitable> getChildren(Model model, ASTNode parent, Location location) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(model.keyValues().stream()
                .map(v -> ConditionalNode.mapConditional(
                        v, ModelKeyValueAST.create(v, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(model.keyLists().stream()
                .map(l -> ConditionalNode.mapConditional(
                        l, ModelKeyListAST.create(l, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(model.keyMaps().stream()
                .map(m -> ConditionalNode.mapConditional(
                        m, ModelKeyMapAST.create(m, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }
}
