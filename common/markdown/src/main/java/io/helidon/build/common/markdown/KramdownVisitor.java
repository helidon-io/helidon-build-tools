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
package io.helidon.build.common.markdown;

/**
 * Visitor for the different types of the {@code KramdownNode}.
 *
 * @param <T> type of the additional argument.
 */
interface KramdownVisitor<T> {

    /**
     * Process {@code SimpleTextKramdownNode} node.
     *
     * @param node SimpleTextKramdownNode
     * @param arg  argument
     */
    void visit(SimpleTextKramdownNode node, T arg);
}
