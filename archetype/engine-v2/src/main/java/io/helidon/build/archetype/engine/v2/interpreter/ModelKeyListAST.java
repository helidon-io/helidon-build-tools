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
public class ModelKeyListAST extends ASTNode implements ConditionalNode {

    private int order = 100;
    private final String key;

    ModelKeyListAST(String key, int order, ASTNode parent, Location location) {
        super(parent, location);
        this.key = key;
        this.order = order;
    }

    /**
     * Get the key of the list.
     *
     * @return key
     */
    public String key() {
        return key;
    }

    /**
     * Get the list order.
     *
     * @return order
     */
    public int order() {
        return order;
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
//        ConditionalNode.addChildren(listFrom, result, children, location);
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
