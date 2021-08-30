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

import io.helidon.build.archetype.engine.v2.descriptor.Model;

/**
 * Archetype model AST node.
 */
public class ModelAST extends ASTNode implements ConditionalNode {

    ModelAST(String currentDirectory) {
        super(currentDirectory);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static ModelAST from(Model model, String currentDirectory) {
        ModelAST result = new ModelAST(currentDirectory);

        LinkedList<Visitable> children = getChildren(model, currentDirectory);
        ConditionalNode.addChildren(model, result, children, currentDirectory);

        return result;
    }

    private static LinkedList<Visitable> getChildren(Model model, String currentDirectory) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(model.keyValues());
        result.addAll(transformList(model.keyLists(), l -> ModelKeyListAST.from(l, currentDirectory)));
        result.addAll(transformList(model.keyMaps(), m -> ModelKeyMapAST.from(m, currentDirectory)));
        return result;
    }
}
