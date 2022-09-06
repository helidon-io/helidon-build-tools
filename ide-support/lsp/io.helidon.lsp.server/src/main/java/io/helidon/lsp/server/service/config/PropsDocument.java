/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.service.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents content of the meta configuration file.
 */
//TODO change or remove it
public class PropsDocument extends LinkedHashMap<String, Object> {

    private LinkedHashMap<Map.Entry<String, Object>, FileBinding> binding = new LinkedHashMap<>();

    public LinkedHashMap<Map.Entry<String, Object>, FileBinding> getBinding() {
        return binding;
    }

    public void setBinding(LinkedHashMap<Map.Entry<String, Object>, FileBinding> binding) {
        this.binding = binding;
    }

    /**
     * Get parent node for the given node.
     *
     * @param childNode child node.
     * @return parent node for the given node.
     */
    public Map.Entry<String, Object> getParentNode(Object childNode) {
        if (!(childNode instanceof Map.Entry)) {
            return null;
        }
        Set<Map.Entry<String, Object>> entries = entrySet();
        if (entries.contains(childNode)) {
            //it is a root node - it does not have a parent
            return null;
        }
        return getParentNodeRecursive(entries, childNode);
    }

    /**
     * Get siblings for the given node.
     *
     * @param node node.
     * @return siblings for the given node.
     */
    @SuppressWarnings("unchecked")
    public Set<Map.Entry<String, Object>> getSiblings(Object node) {
        if (!(node instanceof Map.Entry)) {
            return null;
        }
        Map.Entry<String, Object> parentNode = getParentNode(node);
        Set<Map.Entry<String, Object>> result;
        if (parentNode == null) {
            return entrySet();
        }
        result = getChildren(parentNode);
        if (result != null) {
            result.removeAll(List.of((Map.Entry<String, Object>) node));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map.Entry<String, Object> getParentNodeRecursive(Set<Map.Entry<String, Object>> entries, Object childNode) {
        for (Map.Entry<String, Object> entry : entries) {
            Object node = entry.getValue();
            if (node instanceof Map) {
                if (((Map<?, ?>) node).entrySet().contains((Map.Entry<?, ?>) childNode)) {
                    return entry;
                } else {
                    return getParentNodeRecursive(((Map<String, Object>) node).entrySet(), childNode);
                }
            }
        }
        return null;
    }

    /**
     * Get children for the given node.
     *
     * @param parent parent node.
     * @return children for the given node.
     */
    @SuppressWarnings("unchecked")
    public Set<Map.Entry<String, Object>> getChildren(Object parent) {
        if (!(parent instanceof Map.Entry)) {
            return null;
        }
        if (!(((Map.Entry<?, ?>) parent).getValue() instanceof Map)) {
            return Collections.emptySet();
        }
        return ((Map<String, Object>) ((Map.Entry<String, Object>) parent).getValue()).entrySet();
    }

    /**
     * Get leaves for the current object.
     *
     * @return string representation of the leaves for the current object.
     */
    public Set<String> getLeaves() {
        return getLeavesInternal("", this);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getLeavesInternal(String parentKey, Map<String, Object> value) {
        Set<Map.Entry<String, Object>> entries = value.entrySet();
        Set<String> result = new LinkedHashSet<>();
        parentKey = parentKey.trim();
        for (Map.Entry<String, Object> entry : entries) {
            String key = parentKey.isEmpty() ? entry.getKey() : (parentKey + "." + entry.getKey());
            if (entry.getValue() instanceof Map) {
                Set<String> leafs = getLeavesInternal(key, (Map<String, Object>) entry.getValue());
                result.addAll(leafs);
            } else {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Get string representation of the given node.
     *
     * @param node node.
     * @return string representation of the given node.
     */
    public String toString(Map.Entry<String, Object> node) {
        List<Map.Entry<String, Object>> parentChain = new ArrayList<>();
        parentChain.add(node);
        StringBuilder result = new StringBuilder();
        Map.Entry<String, Object> parent = null;
        do {
            parent = getParentNode(node);
            if (parent != null) {
                parentChain.add(parent);
                node = parent;
            }
        } while (parent != null);
        Collections.reverse(parentChain);
        for (int i = 0; i < parentChain.size(); i++) {
            result.append(parentChain.get(i).getKey());
            if (i != (parentChain.size() - 1)) {
                result.append(".");
            }
        }
        return result.toString();
    }

    /**
     * Get parent node for the current position in the parsed document.
     *
     * @param currentColumn Current position column.
     * @param currentRow    Current position line.
     * @return Parent node for the current position.
     */
    public Map.Entry<String, Object> getLastProcessedParentNode(
            int currentColumn, int currentRow
    ) {
        Set<Map.Entry<Map.Entry<String, Object>, FileBinding>> bindingEntries =
                getBinding().entrySet();
        Map.Entry<String, Object> lastNode = null;
        for (Map.Entry<Map.Entry<String, Object>, FileBinding> entry : bindingEntries) {
            if (entry.getValue().getRow() >= currentRow) {
                return lastNode;
            }
            if (entry.getValue().getColumn() < currentColumn) {
                lastNode = entry.getKey();
            }
        }
        return lastNode;
    }

    /**
     * Class that uses to bind a place in the file with corresponding node in parsed content of the PropsDocument
     * instance.
     */
    public static class FileBinding {
        private Integer row;
        private Integer column;
        private Integer level;

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public Integer getColumn() {
            return column;
        }

        public void setColumn(Integer column) {
            this.column = column;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }
    }
}
