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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Config node diffs.
 */
final class ConfigDiffs implements Iterator<Diff> {

    private final ConfigNode orig;
    private final ConfigNode actual;
    private final LinkedList<Item> stack;
    private boolean used;
    private Diff next;

    /**
     * Create a new config diffs.
     *
     * @param orig   original node, must be non {@code null}
     * @param actual actual node, must be non {@code null}
     */
    ConfigDiffs(ConfigNode orig, ConfigNode actual) {
        if (orig == null || actual == null) {
            throw new IllegalArgumentException();
        }
        this.orig = orig;
        this.actual = actual;
        stack = new LinkedList<>();
        stack.add(new NodeItem(orig, actual));
    }

    /**
     * Rewind the diff to iterate again.
     */
    ConfigDiffs rewind() {
        if (used) {
            stack.clear();
            stack.add(new NodeItem(orig, actual));
            used = false;
        }
        return this;
    }

    /**
     * Get the diffs as a {@link Stream}.
     *
     * @return stream of Diff
     */
    Stream<Diff> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
    }

    @Override
    public Diff next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        used = true;
        Diff diff = next;
        next = null;
        return diff;
    }

    @Override
    public boolean hasNext() {
        while (next == null && !stack.isEmpty()) {
            Item item = stack.pop();
            if (item instanceof NodeItem) {
                NodeItem node = (NodeItem) item;
                next = item.toDiff();
                if (next == null) {
                    stack.addAll(0, node.attributes());
                    List<NodeItem> children = node.children();
                    if (!children.isEmpty()) {
                        for (int i = children.size() - 1; i >= 0; i--) {
                            stack.push(children.get(i));
                        }
                    }
                }
            } else {
                next = item.toDiff();
            }
        }
        return next != null;
    }

    /**
     * Diff-able item.
     */
    private interface Item {

        /**
         * Create a {@link Diff} instance for this item.
         *
         * @return Diff
         */
        Diff toDiff();
    }

    private final class NodeItem implements Item {

        private final ConfigNode orig;
        private final ConfigNode actual;

        NodeItem(ConfigNode orig, ConfigNode actual) {
            if (orig == null && actual == null) {
                throw new IllegalArgumentException();
            }
            this.orig = orig;
            this.actual = actual;
        }

        List<AttributeItem> attributes() {
            Map<String, String> origAttrs = orig.attributes();
            Map<String, String> actualAttrs = actual.attributes();
            LinkedList<AttributeItem> attributes = new LinkedList<>();
            for (Map.Entry<String, String> entry : origAttrs.entrySet()) {
                // exist in both
                // removed
                attributes.add(new AttributeItem(
                        this,
                        entry.getKey(),
                        entry.getValue(),
                        actualAttrs.getOrDefault(entry.getKey(), null)));
            }
            for (Map.Entry<String, String> entry : actualAttrs.entrySet()) {
                if (!origAttrs.containsKey(entry.getKey())) {
                    // added
                    attributes.add(new AttributeItem(
                            this,
                            entry.getKey(),
                            null,
                            entry.getValue()));
                }
            }
            return attributes;
        }

        List<NodeItem> children() {
            Iterator<ConfigNode> origIt = orig.children().iterator();
            Iterator<ConfigNode> actualIt = actual.children().iterator();
            List<NodeItem> children = new LinkedList<>();
            while (origIt.hasNext() || actualIt.hasNext()) {
                ConfigNode origNext;
                ConfigNode actualNext;
                if (origIt.hasNext()) {
                    origNext = origIt.next();
                } else {
                    origNext = null;
                }
                if (actualIt.hasNext()) {
                    actualNext = actualIt.next();
                } else {
                    actualNext = null;
                }
                children.add(new NodeItem(origNext, actualNext));
            }
            return children;
        }

        @Override
        public Diff toDiff() {
            if (orig != null && actual == null) {
                return new Diff(orig, null, orig.path());
            }
            if (orig == null && actual != null) {
                return new Diff(null, actual, actual.path());
            }
            if (orig != null) {
                if (!orig.name().equals(actual.name())){
                    return new Diff(orig, actual, orig.path());
                }
                String origValue = orig.value();
                String actualValue = actual.value();
                if ((origValue != null && actualValue == null && !actual.hasChildren())
                        || (origValue == null && actualValue != null && !orig.hasChildren())
                        || (origValue != null && actualValue != null && !origValue.equals(actualValue))) {
                    return new Diff(origValue, actualValue, orig.path() + "/" + orig.name());
                }
            }
            return null;
        }
    }

    private final class AttributeItem implements Item {

        private final NodeItem node;
        private final String key;
        private final String origValue;
        private final String actualValue;

        AttributeItem(NodeItem node, String key, String origValue, String actualValue) {
            if (node == null || key == null || key.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (origValue == null && actualValue == null) {
                throw new IllegalArgumentException();
            }
            this.node = node;
            this.key = key;
            this.origValue = origValue;
            this.actualValue = actualValue;
        }

        private String path() {
            ConfigNode n;
            if (node.orig != null) {
                n = node.orig;
            } else {
                n = node.actual;
            }
            String path = n.path();
            if (n.parent() != null) {
                path += "/";
            }
            return path + n.name() + "{" + n.index() + "}" + "#" + key;
        }

        @Override
        public Diff toDiff() {
            if (!(origValue != null && origValue.equals(actualValue))) {
                return new Diff(origValue, actualValue, path());
            }
            return null;
        }
    }
}
