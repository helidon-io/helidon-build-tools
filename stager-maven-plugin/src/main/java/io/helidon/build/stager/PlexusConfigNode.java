/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.stager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * A config node that holds a node with its parent and a list of mapped children objects.
 */
public class PlexusConfigNode {

    private static AtomicInteger NEXT_ID = new AtomicInteger(0);
    private final PlexusConfiguration orig;
    private final PlexusConfigNode parent;
    private final int id;

    PlexusConfigNode(PlexusConfiguration orig, PlexusConfigNode parent) {
        this.orig = Objects.requireNonNull(orig, "orig is null");
        this.parent = parent;
        this.id = NEXT_ID.incrementAndGet();
    }

    /**
     * Visit this config node.
     *
     * @param visitor visitor
     */
    void visit(Consumer<PlexusConfigNode> visitor) {
        LinkedList<PlexusConfigNode> stack = new LinkedList<>(children());
        int parentId = id;
        while (!stack.isEmpty()) {
            PlexusConfigNode node = stack.peek();
            if (node.id == parentId) {
                // leaving node
                parentId = node.parent.id;
                stack.pop();
                visitor.accept(node);
            } else {
                List<PlexusConfigNode> children = node.children();
                if (!children.isEmpty()) {
                    // entering node
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                } else {
                    // leaf
                    parentId = node.parent.id;
                    stack.pop();
                    visitor.accept(node);
                }
            }
        }
    }

    /**
     * Return the node id.
     *
     * @return id
     */
    int id() {
        return id;
    }

    /**
     * Get the node name.
     *
     * @return name
     */
    String name() {
        return orig.getName();
    }

    /**
     * Get the node value.
     *
     * @return value, may be {@code null}
     */
    String value() {
        return orig.getValue();
    }

    /**
     * Get the node attributes.
     *
     * @return attributes
     */
    Map<String, String> attributes() {
        Map<String, String> attributes = new HashMap<>();
        for (String attrName : orig.getAttributeNames()) {
            attributes.put(attrName, orig.getAttribute(attrName));
        }
        return attributes;
    }

    /**
     * Get the parent node.
     *
     * @return parent, may be {@code null}
     */
    PlexusConfigNode parent() {
        return parent;
    }

    /**
     * Get the nested children.
     *
     * @return list of children config nodes
     */
    List<PlexusConfigNode> children() {
        List<PlexusConfigNode> children = new LinkedList<>();
        for (PlexusConfiguration child : orig.getChildren()) {
            children.add(new PlexusConfigNode(child, this));
        }
        return children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlexusConfigNode that = (PlexusConfigNode) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PlexusConfigNode{" +
                "id=" + id
                + ", name=" + orig.getName()
                + '}';
    }
}
