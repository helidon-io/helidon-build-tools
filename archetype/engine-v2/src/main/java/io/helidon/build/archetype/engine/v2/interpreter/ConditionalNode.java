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

import io.helidon.build.archetype.engine.v2.descriptor.Conditional;

/**
 * Base interface for conditional nodes.
 */
public interface ConditionalNode {

    /**
     * If initial node contains {@code if} statement, new {@code IfStatement} instance will be created, children will be added
     * to this instance and this {@code IfStatement} instance will be added as a child to the result {@code ASTNode}.
     *
     * @param initial  initial node from the archetype V2 descriptor
     * @param result   result ASTNode
     * @param children children
     * @param location location
     */
    static void addChildren(Conditional initial, ASTNode result, LinkedList<Visitable> children, ASTNode.Location location) {
        if (initial.ifProperties() != null) {
            IfStatement ifStatement = new IfStatement(initial.ifProperties(), result, location);
            children.forEach(child -> {
                if (child instanceof ASTNode) {
                    ((ASTNode) child).parent(ifStatement);
                }
            });
            ifStatement.children().addAll(children);
            result.children().add(ifStatement);
        } else {
            children.forEach(child -> {
                if (child instanceof ASTNode) {
                    ((ASTNode) child).parent(result);
                }
            });
            result.children().addAll(children);
        }
    }

    /**
     * If {@code initial} contains {@code if} statement wrap {@code visitable} into the new {@code IfStatement} instance and
     * return it or return unwrapped {@code visitable}.
     *
     * @param initial   initial
     * @param visitable visitable
     * @param parent    parent AST node for the visitable
     * @param location  location
     * @return visitable
     */
    static Visitable mapConditional(Conditional initial, Visitable visitable, ASTNode parent, ASTNode.Location location) {
        if (initial.ifProperties() != null) {
            IfStatement result = new IfStatement(initial.ifProperties(), parent, location);
            if (visitable instanceof ASTNode) {
                ((ASTNode) visitable).parent(result);
            }
            result.children().add(visitable);
            return result;
        }
        return visitable;
    }
}
