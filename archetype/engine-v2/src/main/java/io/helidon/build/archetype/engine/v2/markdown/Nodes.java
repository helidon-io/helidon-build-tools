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

import java.util.Iterator;

/**
 * Utility class for working with multiple {@link Node}s.
 */
class Nodes {

    private Nodes() {
    }

    public static Iterable<Node> between(Node start, Node end) {
        return new Nodes.NodeIterable(start.getNext(), end);
    }

    private static class NodeIterable implements Iterable<Node> {

        private final Node first;
        private final Node end;

        private NodeIterable(Node first, Node end) {
            this.first = first;
            this.end = end;
        }

        @Override
        public Iterator<Node> iterator() {
            return new Nodes.NodeIterator(first, end);
        }
    }

    private static class NodeIterator implements Iterator<Node> {

        private Node node;
        private final Node end;

        private NodeIterator(Node first, Node end) {
            node = first;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            return node != null && node != end;
        }

        @Override
        public Node next() {
            Node result = node;
            node = node.getNext();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}

