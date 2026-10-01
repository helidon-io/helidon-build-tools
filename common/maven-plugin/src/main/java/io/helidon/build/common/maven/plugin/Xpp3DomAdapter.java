/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.xml.XMLElement;

import org.apache.maven.model.InputLocation;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * {@link io.helidon.build.common.xml.XMLElement} adapter for {@link org.codehaus.plexus.util.xml.Xpp3Dom}.
 */
public final class Xpp3DomAdapter implements XMLElement {

    private final Xpp3Dom orig;
    private final String name;
    private final Map<String, String> attributes;
    private final List<XMLElement> children = new ArrayList<>();
    private final Location location;
    private Xpp3DomAdapter parent;
    private String value;

    private Xpp3DomAdapter(Xpp3Dom orig, Xpp3DomAdapter parent) {
        this.orig = orig;
        this.parent = parent;
        this.name = orig.getName();
        this.attributes = new LinkedHashMap<>();
        for (String attrName : orig.getAttributeNames()) {
            attributes.put(attrName, orig.getAttribute(attrName));
        }
        this.value = orig.getValue();
        if (orig.getInputLocation() instanceof InputLocation loc) {
            this.location = new Location("pom.xml", loc.getLineNumber(), loc.getColumnNumber());
        } else {
            this.location = Location.UNKNOWN;
        }
    }

    /**
     * Create a new instance.
     *
     * @param orig original {@link Xpp3Dom} instance to be adapted
     * @return XMLElement
     */
    public static XMLElement create(Xpp3Dom orig) {
        Xpp3DomAdapter node = new Xpp3DomAdapter(orig, null);
        Deque<Xpp3DomAdapter> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            Xpp3DomAdapter p = stack.pop();
            if (p.orig != null) {
                Xpp3Dom[] children = p.orig.getChildren();
                for (int i = children.length - 1; i >= 0; i--) {
                    Xpp3DomAdapter n = new Xpp3DomAdapter(children[i], p);
                    p.children.add(0, n);
                    stack.push(n);
                }
            }
        }
        return node;
    }

    @Override
    public Xpp3DomAdapter parent() {
        return parent;
    }

    @Override
    public XMLElement parent(XMLElement parent) {
        this.parent = (Xpp3DomAdapter) parent;
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value != null ? value : "";
    }

    @Override
    public XMLElement value(String value) {
        this.value = value;
        return this;
    }

    @Override
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public List<XMLElement> children() {
        return children;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public XMLElement detach() {
        if (parent == null) {
            return this;
        }
        return visit(new Visitor() {
            private Xpp3DomAdapter node;
            private Xpp3DomAdapter last;

            @Override
            public boolean visitElement(XMLElement elt) {
                if (elt instanceof Xpp3DomAdapter pcn) {
                    node = new Xpp3DomAdapter(pcn.orig, node);
                    if (node.parent != null) {
                        node.parent.children.add(node);
                    }
                }
                return true;
            }

            @Override
            public void postVisitElement(XMLElement elt) {
                if (node != null) {
                    Xpp3DomAdapter parent = node.parent;
                    if (parent == null) {
                        last = node;
                    }
                    node = parent;
                }
            }
        }).last;
    }

    @Override
    public String toString() {
        return "ConfigNode{"
                + "name=" + name
                + '}';
    }
}
