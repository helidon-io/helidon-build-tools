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

import io.helidon.build.archetype.engine.v2.descriptor.Model;

/**
 * Archetype model AST node.
 */
public class ModelAST extends ASTNode implements ConditionalNode {

    ModelAST(ASTNode parent, String currentDirectory) {
        super(parent, currentDirectory);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static ModelAST create(Model model, ASTNode parent, String currentDirectory) {
        ModelAST result = new ModelAST(parent, currentDirectory);

        LinkedList<Visitable> children = getChildren(model, result, currentDirectory);
        ConditionalNode.addChildren(model, result, children, currentDirectory);

        return result;
    }

    private static LinkedList<Visitable> getChildren(Model model, ASTNode parent, String currentDirectory) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(model.keyValues().stream()
                .map(v -> ConditionalNode.mapConditional(
                        v, ModelKeyValueAST.create(v, parent, currentDirectory), parent, currentDirectory))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(model.keyLists().stream()
                .map(l -> ConditionalNode.mapConditional(
                        l, ModelKeyListAST.create(l, parent, currentDirectory), parent, currentDirectory))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(model.keyMaps().stream()
                .map(m -> ConditionalNode.mapConditional(
                        m, ModelKeyMapAST.create(m, parent, currentDirectory), parent, currentDirectory))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }
}
