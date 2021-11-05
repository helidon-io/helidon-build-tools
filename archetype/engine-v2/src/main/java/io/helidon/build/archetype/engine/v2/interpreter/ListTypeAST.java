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

import io.helidon.build.archetype.engine.v2.descriptor.ListType;

/**
 * Archetype list AST node without key used in {@link ModelAST}.
 */
public class ListTypeAST extends ASTNode implements ConditionalNode {

    private int order = 100;

    ListTypeAST(int order, ASTNode parent, Location location) {
        super(parent, location);
        this.order = order;
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

    static ASTNode create(ListType listFrom, ASTNode parent, Location location) {
        ListTypeAST result = new ListTypeAST(listFrom.order(), parent, location);

        LinkedList<Visitable> children = getChildren(listFrom, result, location);
        result.children().addAll(children);
        return result;
    }

    private static LinkedList<Visitable> getChildren(ListType list, ASTNode parent, Location location) {
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