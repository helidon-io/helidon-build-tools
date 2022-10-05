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
package io.helidon.build.maven.stager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config reader.
 */
final class ConfigReader implements PlexusConfigNode.Visitor {

    private final Map<PlexusConfigNode, Map<String, List<StagingElement>>> mappings;
    private final Deque<Scope> scopes;
    private final StagingElementFactory factory;

    ConfigReader(StagingElementFactory factory) {
        this.mappings = new LinkedHashMap<>();
        this.scopes = new ArrayDeque<>();
        this.factory = factory;
    }

    /**
     * Read the given staging configuration.
     *
     * @param node config node
     * @return actions
     */
    List<StagingAction> read(PlexusConfigNode node) {
        node.visit(this);
        return mappings.get(node)
                       .values()
                       .stream()
                       .flatMap(List::stream)
                       .filter(StagingAction.class::isInstance)
                       .map(StagingAction.class::cast)
                       .collect(Collectors.toList());
    }

    @Override
    public void visitNode(PlexusConfigNode node) {
        scopes.push(new Scope(node.name(), scopes.peek()));
    }

    @Override
    public void postVisitNode(PlexusConfigNode node) {
        PlexusConfigNode nodeParent = node.parent();
        String nodeName = node.name();
        mappings.computeIfAbsent(node, n -> new LinkedHashMap<>());
        mappings.computeIfAbsent(nodeParent, n -> new LinkedHashMap<>());
        if (factory.isWrapperElement(nodeName)) {
            String wrappedName = factory.wrappedElementName(nodeName);
            mappings.get(nodeParent).put(wrappedName, mappings.get(node).get(wrappedName));
        } else {
            Scope scope = scopes.peek();
            if (scope == null || scope.parent == null) {
                throw new IllegalStateException("Scope is not available");
            }
            StagingElement element = factory.create(
                    nodeName,
                    node.attributes(),
                    mappings.get(node),
                    node.value(),
                    scope);
            if (element instanceof Variables) {
                for (Variable variable : ((Variables) element)) {
                    scope.parent.variables.put(variable.name(), variable);
                }
            }
            mappings.get(nodeParent)
                    .computeIfAbsent(nodeName, n -> new LinkedList<>())
                    .add(element);
        }
        scopes.pop();
    }

    /**
     * Scope.
     */
    static final class Scope {

        private final String name;
        private final Scope parent;
        private final Map<String, Variable> variables;

        /**
         * Create a new scope.
         *
         * @param parent parent scope
         */
        Scope(String name, Scope parent) {
            this.name = name;
            this.parent = parent;
            this.variables = new HashMap<>();
        }

        /**
         * Resolve a variable in this scope.
         *
         * @param name variable name
         * @return variable
         * @throws IllegalArgumentException if the variable cannot be found
         */
        Variable resolve(String name) {
            Scope scope = this;
            while (scope != null) {
                if (scope.variables.containsKey(name)) {
                    return scope.variables.get(name);
                }
                scope = scope.parent;
            }
            throw new IllegalArgumentException("Unresolved variable: " + name);
        }

        @Override
        public String toString() {
            return "Scope{"
                    + "name='" + name + '\''
                    + ", parent=" + parent != null ? parent.name : null
                    + ", variables=" + variables
                    + '}';
        }
    }
}
