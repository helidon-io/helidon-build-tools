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

import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Config converters.
 */
final class ConfigConverters {

    private ConfigConverters() {
    }

    /**
     * Convert a config node to {@link Xpp3Dom}.
     *
     * @param node node to convert
     * @return Xpp3Dom
     */
    static Xpp3Dom toXpp3Dom(ConfigNode node) {
        return new Xpp3DomConverter(node).apply();
    }

    /**
     * Convert a config node to {@link String}.
     *
     * @param node node to convert
     * @return String
     */
    static String toString(ConfigNode node) {
        return new StringAdapter(node).apply();
    }

    private static final class Xpp3DomConverter extends ConfigConverter<Xpp3Dom> {

        private Xpp3Dom root = null;
        private Xpp3Dom parent = null;

        Xpp3DomConverter(ConfigNode node) {
            super(node);
        }

        @Override
        boolean enteringTreeNode(ConfigNode node) {
            processNode(node);
            return true;
        }

        @Override
        void leavingTreeNode(ConfigNode node) {
            if (parent != null) {
                parent = parent.getParent();
            }
        }

        @Override
        void leafNode(ConfigNode node) {
            processNode(node);
        }

        void processNode(ConfigNode node) {
            Xpp3Dom elt = new Xpp3Dom(node.name());
            node.attributes().forEach(elt::setAttribute);
            elt.setValue(node.value());
            if (parent != null) {
                parent.addChild(elt);
            }
            if (node.hasChildren()) {
                parent = elt;
            }
            if (root == null) {
                root = elt;
            }
        }

        @Override
        protected Xpp3Dom root() {
            return root;
        }
    }

    private static final class StringAdapter extends ConfigConverter<String> {

        private final StringBuilder sb = new StringBuilder();
        private final LinkedList<Integer> indexes = new LinkedList<>();

        StringAdapter(ConfigNode node) {
            super(node);
        }

        @Override
        void leafNode(ConfigNode node) {
            if (indexes.isEmpty()) {
                processNode(node, 0);
            } else {
                int index = indexes.pop();
                processNode(node, index);
                indexes.push(++index);
            }
        }

        @Override
        boolean enteringTreeNode(ConfigNode node) {
            int index = indexes.isEmpty() ? 0 : indexes.pop();
            processNode(node, index);
            indexes.push(++index);
            indexes.push(0);
            return true;
        }

        @Override
        void leavingTreeNode(ConfigNode node) {
            sb.append("]");
            indexes.pop();
        }

        void processNode(ConfigNode node, int index) {
            if (index > 0) {
                sb.append(",");
            }
            sb.append(node.name());
            Map<String, String> attributes = node.attributes();
            if (!attributes.isEmpty()) {
                sb.append("<")
                        .append(attributes
                                .entrySet()
                                .stream()
                                .map(e -> e.getKey() + ":" + e.getValue())
                                .collect(Collectors.joining(",")))
                        .append(">");
            }
            String value = node.value();
            if (value != null && !value.isEmpty()) {
                sb.append("=").append(value);
            } else if (node.hasChildren()) {
                sb.append("[");
            }
        }

        @Override
        protected String root() {
            return sb.toString();
        }
    }
}
