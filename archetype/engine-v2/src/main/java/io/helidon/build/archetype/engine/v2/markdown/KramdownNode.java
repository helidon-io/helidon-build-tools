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
package io.helidon.build.archetype.engine.v2.markdown;

import org.commonmark.node.CustomNode;

/**
 * Base class for different types of AST nodes that represent markdown text written using kramdown extension.
 * <p>
 * If other class extends this class, {@link KramdownNodeType} must be extended and new constant must be added to this enum
 * and {@link KramdownVisitor} must be extended too and a new method for the new inheritor of the {@code KramdownNode} must be
 * added.
 * </p>
 */
abstract class KramdownNode extends CustomNode {

    private final KramdownNodeType type;

    /**
     * Create a new instance.
     *
     * @param type type of the node
     */
    KramdownNode(KramdownNodeType type) {
        this.type = type;
    }

    public KramdownNodeType type() {
        return type;
    }

    /**
     * Add additional logic to the current {@code KramdownNode} instance.
     *
     * @param visitor Visitor
     * @param arg     additional argument
     * @param <T>     generic type of the argument
     */
    abstract <T> void accept(KramdownVisitor<T> visitor, T arg);
}
