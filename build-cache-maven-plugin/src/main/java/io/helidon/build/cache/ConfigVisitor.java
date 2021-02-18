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
package io.helidon.build.cache;

import java.util.LinkedList;
import java.util.List;

/**
 * Config visitor.
 */
abstract class ConfigVisitor {

    /**
     * Entering a tree node.
     *
     * @param node config node
     * @return {@code true} to visit the children of the node, {@code false} otherwise
     */
    boolean enteringTreeNode(ConfigNode node) {
        return true;
    }

    /**
     * Leaving a tree node.
     *
     * @param node config node
     */
    void leavingTreeNode(ConfigNode node) {
        // do-nothing
    }

    /**
     * Process a leaf node.
     *
     * @param node config node
     */
    abstract void leafNode(ConfigNode node);

    /**
     * Visit the given node.
     * @param node config node to visit
     */
    final void visit(ConfigNode node) {
        LinkedList<ConfigNode> stack = new LinkedList<>();
        stack.push(node);
        ConfigNode parent = node.parent();
        int parentId = parent != null ? parent.id() : 0;
        while (!stack.isEmpty()) {
            node = stack.peek();
            parent = node.parent();
            if (node.id() == parentId) {
                // leaving node
                parentId = parent != null ? parent.id() : 0;
                stack.pop();
                leavingTreeNode(node);
            } else {
                List<ConfigNode> children = node.children();
                if (!children.isEmpty()) {
                    if (enteringTreeNode(node)) {
                        // entering node
                        for (int i = children.size() - 1; i >= 0; i--) {
                            stack.push(children.get(i));
                        }
                    }
                } else {
                    // leaf
                    parentId = parent != null ? parent.id() : 0;
                    stack.pop();
                    leafNode(node);
                }
            }
        }
    }
}
