/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.maven.stager;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.build.common.xml.XMLElement;

/**
 * Ordered free-form data supplied to a template.
 */
final class TemplateModel implements StagingElement {

    private final Node root;

    private TemplateModel(Node root) {
        this.root = root;
    }

    static TemplateModel empty() {
        return new TemplateModel(new Node(Map.of()));
    }

    static TemplateModel create(XMLElement element) {
        return new TemplateModel(node(element, "model"));
    }

    Node root() {
        return root;
    }

    TemplateModel resolve(Map<String, String> vars) {
        return new TemplateModel(root.resolve(vars));
    }

    @Override
    public String elementName() {
        return "model";
    }

    private static Node node(XMLElement element, String path) {
        if (!element.attributes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Model element '%s' must not have attributes"
                            .formatted(path));
        }
        if (!element.children().isEmpty() && !element.value().isBlank()) {
            throw new IllegalArgumentException(
                    "Model element '%s' cannot mix text and child elements"
                            .formatted(path));
        }
        Map<String, List<Object>> fields = new LinkedHashMap<>();
        for (XMLElement child : element.children()) {
            String childPath = path + "." + child.name();
            Object value;
            if (child.children().isEmpty()) {
                if (!child.attributes().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Model element '%s' must not have attributes"
                                    .formatted(childPath));
                }
                value = child.value();
            } else {
                value = node(child, childPath);
            }
            fields.computeIfAbsent(child.name(), ignored -> new ArrayList<>()).add(value);
        }
        return new Node(fields);
    }

    static final class Node extends AbstractMap<String, Object> implements Iterable<Object> {

        private final Map<String, List<Object>> fields;

        Node(Map<String, List<Object>> fields) {
            this.fields = fields;
        }

        Node resolve(Map<String, String> vars) {
            Map<String, List<Object>> resolved = new LinkedHashMap<>();
            fields.forEach((name, values) -> resolved.put(name, values.stream()
                    .map(value -> resolve(value, vars))
                    .toList()));
            return new Node(resolved);
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            Map<String, Object> entries = new LinkedHashMap<>();
            fields.forEach((name, values) -> entries.put(name, value(name, values)));
            return entries.entrySet();
        }

        @Override
        public Object get(Object key) {
            if (!(key instanceof String name)) {
                return null;
            }
            List<Object> values = fields.get(key);
            return values == null ? null : value(name, values);
        }

        @Override
        public Iterator<Object> iterator() {
            List<Map.Entry<String, Object>> children = new ArrayList<>();
            fields.forEach((name, values) -> values.forEach(value -> children.add(Map.entry(name, value))));
            List<Object> entries = new ArrayList<>(children.size());
            int index = 0;
            for (Map.Entry<String, Object> child : children) {
                entries.add(new TemplateModel.Entry(child.getKey(), child.getValue(), index++, children.size(), this));
            }
            return entries.iterator();
        }

        private Object value(String name, List<Object> values) {
            return values.size() == 1 ? values.get(0) : new Elements(name, values, this);
        }

        private static Object resolve(Object value, Map<String, String> vars) {
            if (value instanceof Node node) {
                return node.resolve(vars);
            }
            String result = (String) value;
            for (Map.Entry<String, String> variable : vars.entrySet()) {
                result = result.replace("{" + variable.getKey() + "}", variable.getValue());
            }
            return result;
        }
    }

    private static final class Elements extends AbstractList<Object> {

        private final String name;
        private final List<Object> values;
        private final Node parent;

        Elements(String name, List<Object> values, Node parent) {
            this.name = name;
            this.values = values;
            this.parent = parent;
        }

        @Override
        public Object get(int index) {
            return new Entry(name, values.get(index), index, values.size(), parent);
        }

        @Override
        public int size() {
            return values.size();
        }
    }

    private static final class Entry extends AbstractMap<String, Object> {

        private static final Set<String> SYNTHETIC = Set.of("name", "value", "index", "first", "last");

        private final Map<String, Object> fields;

        Entry(String name, Object value, int index, int size, Map<String, Object> parent) {
            fields = new LinkedHashMap<>();
            fields.put("name", name);
            fields.put("value", value);
            fields.put("index", index);
            fields.put("first", index == 0);
            fields.put("last", index == size - 1);
            if (value instanceof Node node) {
                node.fields.forEach((fieldName, values) -> putIfModelField(fieldName, node.value(fieldName, values)));
            }
            parent.forEach(this::putIfModelField);
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return fields.entrySet();
        }

        void putIfModelField(String name, Object value) {
            if (!SYNTHETIC.contains(name)) {
                fields.putIfAbsent(name, value);
            }
        }
    }
}
