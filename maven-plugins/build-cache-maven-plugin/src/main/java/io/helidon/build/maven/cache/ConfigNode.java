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
package io.helidon.build.maven.cache;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A config node that holds a node with its parent.
 */
public class ConfigNode {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    private final ConfigAdapter orig;
    private final ConfigNode parent;
    private final Map<String, String> attributes;
    private List<ConfigNode> children;
    private String value;
    private final String name;
    private final int id;

    /**
     * Create a new config node from a adapter node.
     *
     * @param orig   original node
     * @param parent parent, may be {@code null}
     */
    ConfigNode(ConfigAdapter orig, ConfigNode parent) {
        this.orig = Objects.requireNonNull(orig, "orig is null");
        this.parent = parent;
        this.attributes = orig.attributes();
        this.name = orig.name();
        this.value = orig.value();
        this.id = NEXT_ID.incrementAndGet();
    }

    /**
     * Visit this config node.
     *
     * @param function visitor function
     */
    void visit(Consumer<ConfigNode> function) {
        new ConfigVisitor() {
            @Override
            boolean enteringTreeNode(ConfigNode node) {
                function.accept(node);
                return true;
            }

            @Override
            void leafNode(ConfigNode node) {
                function.accept(node);
            }
        }.visit(this);
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
        return name;
    }

    /**
     * Get the node value.
     *
     * @return value, may be {@code null}
     */
    String value() {
        return value;
    }

    /**
     * Set the node value.
     *
     * @param newValue new value
     */
    void value(String newValue) {
        this.value = newValue;
    }

    /**
     * Get the node attributes.
     *
     * @return attributes
     */
    Map<String, String> attributes() {
        return attributes;
    }

    /**
     * Get the parent node.
     *
     * @return parent, may be {@code null}
     */
    ConfigNode parent() {
        return parent;
    }

    /**
     * Indicate if the node has children.
     *
     * @return {@code true} if this node has children, {@code false} otherwise
     */
    boolean hasChildren() {
        return !children().isEmpty();
    }

    /**
     * Get the nested children.
     *
     * @return list of children config nodes
     */
    List<ConfigNode> children() {
        if (children == null) {
            children = new LinkedList<>();
            for (ConfigAdapter child : orig.children()) {
                children.add(new ConfigNode(child, this));
            }
        }
        return children;
    }

    /**
     * Create a diff between this node and a given node.
     *
     * @param node node to diff
     * @return iterator of diff event
     */
    ConfigDiffs diff(ConfigNode node) {
        return new ConfigDiffs(this, node);
    }

    /**
     * Compute the absolute sibling index.
     *
     * @return index, or {@code -1} if there are no siblings
     */
    int index() {
        int index = 0;
        int count = 0;
        if (parent != null) {
            Iterator<ConfigNode> it = parent.children().iterator();
            while (it.hasNext()) {
                ConfigNode child = it.next();
                if (child.equals(this)) {
                    index = count;
                }
                count++;
            }
        }
        return index;
    }

    /**
     * Compute the node parent path.
     *
     * @return node parent path
     */
    String path() {
        if (parent == null) {
            return "/";
        }
        String path = "";
        ConfigNode n = this;
        while (n.parent != null) {
            path = "/" + n.parent.name + "{" + n.index() + "}" + path;
            n = n.parent;
        }
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigNode that = (ConfigNode) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Convert this config node to {@link Xpp3Dom}.
     *
     * @return Xpp3Dom
     */
    Xpp3Dom toXpp3Dom() {
        return ConfigConverters.toXpp3Dom(this);
    }

    @Override
    public String toString() {
        return ConfigConverters.toString(this);
    }
}
