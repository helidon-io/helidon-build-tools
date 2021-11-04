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


/**
 * Node visitor.
 */
interface Visitor {

    default void visit(FencedCodeBlock fencedCodeBlock) {
        visitChildren(fencedCodeBlock);
    }

    default void visit(Paragraph paragraph) {
        visitChildren(paragraph);
    }

    default void visit(Document document) {
        visitChildren(document);
    }

    default void visit(StrongEmphasis strongEmphasis) {
        visitChildren(strongEmphasis);
    }

    default void visit(Emphasis emphasis) {
        visitChildren(emphasis);
    }

    default void visit(Code code) {
        visitChildren(code);
    }

    default void visit(Text text) {
        visitChildren(text);
    }

    default void visit(Link link) {
        visitChildren(link);
    }

    default void visit(CustomNode customNode) {
        visitChildren(customNode);
    }

    /**
     * Visit the child nodes.
     *
     * @param parent the parent node whose children should be visited
     */
    private void visitChildren(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null) {
            Node next = node.getNext();
            node.accept(this);
            node = next;
        }
    }
}
