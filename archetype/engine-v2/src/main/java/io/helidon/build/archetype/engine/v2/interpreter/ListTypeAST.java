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

import io.helidon.build.archetype.engine.v2.descriptor.ListType;

/**
 * Archetype list AST node without key used in {@link ModelAST}.
 */
public class ListTypeAST extends ASTNode implements ConditionalNode {

    private int order = 100;

    ListTypeAST(int order, String currentDirectory) {
        super(currentDirectory);
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

    static ListTypeAST from(ListType list, String currentDirectory) {
        ListTypeAST result = new ListTypeAST(list.order(), currentDirectory);

        LinkedList<Visitable> children = getChildren(list, currentDirectory);
        ConditionalNode.addChildren(list, result, children, currentDirectory);

        return result;
    }

    private static LinkedList<Visitable> getChildren(ListType list, String currentDirectory) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(list.values());
        result.addAll(transformList(list.maps(), m -> MapTypeAST.from(m, currentDirectory)));
        result.addAll(transformList(list.lists(), l -> ListTypeAST.from(l, currentDirectory)));
        return result;
    }

}
