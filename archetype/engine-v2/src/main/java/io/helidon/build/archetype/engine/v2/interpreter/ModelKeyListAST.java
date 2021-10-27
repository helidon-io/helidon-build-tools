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

import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;

/**
 * Archetype list AST node with key attribute used in {@link ModelAST} and {@link MapTypeAST}.
 */
public class ModelKeyListAST extends ListTypeAST implements ConditionalNode {

    private final String key;

    ModelKeyListAST(String key, int order, ASTNode parent, Location location) {
        super(order, parent, location);
        this.key = key;
    }

    /**
     * Get the key of the list.
     *
     * @return key
     */
    public String key() {
        return key;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static ModelKeyListAST create(ModelKeyList listFrom, ASTNode parent, Location location) {
        ModelKeyListAST result = new ModelKeyListAST(listFrom.key(), listFrom.order(), parent, location);

        LinkedList<Visitable> children = getChildren(listFrom, result, location);
        result.children().addAll(children);

        return result;
    }

    private static LinkedList<Visitable> getChildren(ModelKeyList list, ASTNode parent, Location location) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(list.values().stream()
                .map(v -> ConditionalNode.mapConditional(
                        v, ValueTypeAST.create(v, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(list.maps().stream()
                .map(m -> ConditionalNode.mapConditional(
                        m, MapTypeAST.create(m, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        result.addAll(list.lists().stream()
                .map(l -> ConditionalNode.mapConditional(
                        l, ListTypeAST.create(l, parent, location), parent, location))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }
}
