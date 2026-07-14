/*
 * Copyright (c) 2022, 2026 Oracle and/or its affiliates.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.build.common.xml.XMLElement;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Staging factory.
 */
final class StagingFactory implements XMLElement.Visitor {

    private static final Set<String> ACTION_ELEMENTS = Set.of(
            "directory",
            "archive",
            "download",
            "copy",
            "copy-artifact",
            "file",
            "symlink",
            "template",
            "unpack",
            "unpack-artifact",
            "list-files");

    private static final Set<String> SYNTHETIC_ELEMENTS = Set.of(
            "include",
            "exclude",
            "mapper",
            "substitution");

    private static final Map<String, String> WRAPPER_ELEMENTS = Stream.of(ACTION_ELEMENTS, SYNTHETIC_ELEMENTS)
            .flatMap(Collection::stream)
            .filter(n -> !n.endsWith("s"))
            .collect(toMap(n -> n.endsWith("y") ? n.substring(0, n.length() - 1) + "ies" : n + "s", n -> n));

    private final XMLElement config;
    private final Map<XMLElement, List<StagingElement>> mappings;
    private final Deque<Scope> scopes;

    private StagingFactory(XMLElement config) {
        this.config = config;
        this.mappings = new LinkedHashMap<>();
        this.scopes = new ArrayDeque<>();
    }

    /**
     * Create staging tasks from the given staging configuration.
     *
     * @param config config
     * @return tasks
     */
    static StagingTasks create(XMLElement config) {
        StagingFactory factory = new StagingFactory(config);
        return factory.create();
    }

    private StagingTasks create() {
        config.visit(this);
        StagingAction action = (StagingAction) mappings.get(config.parent()).iterator().next();
        if (action instanceof StagingTasks) {
            return (StagingTasks) action;
        }
        return new StagingTasks("actions", List.of(action), Map.of());
    }

    @Override
    public void visitElement(XMLElement elt) {
        scopes.push(new Scope(elt.name(), scopes.peek()));
    }

    @Override
    public void postVisitElement(XMLElement elt) {
        XMLElement parent = elt.parent();
        if (hasReferencedVariableAncestor(elt) || hasModelAncestor(elt)) {
            mappings.computeIfAbsent(elt, n -> new ArrayList<>());
            mappings.computeIfAbsent(parent, n -> new ArrayList<>());
            scopes.pop();
            return;
        }
        String name = elt.name();
        mappings.computeIfAbsent(elt, n -> new ArrayList<>());
        mappings.computeIfAbsent(parent, n -> new ArrayList<>());
        Scope scope = scopes.peek();
        if (scope == null) {
            throw new IllegalStateException("Scope is not available");
        }
        String value = elt.value();
        if (value.isEmpty() && !elt.children().isEmpty()) {
            value = null;
        }

        List<StagingElement> children = mappings.get(elt);
        StagingElement element = switch (name) {
            case "variables" -> variables(elt.attributes(), children);
            case "variable" -> variable(elt, children, scope);
            case "value" -> variableValue(children, value);
            case "model" -> TemplateModel.create(elt);
            case "iterators" -> actionIterators(children, elt.attributes());
            default -> createDefault(name, elt.attributes(), children, value);
        };
        if (element instanceof Variable variable) {
            scope.parent.variables.put(variable.name(), variable);
        }
        if (element instanceof Variables variables) {
            for (Variable variable : variables) {
                scope.parent.variables.putIfAbsent(variable.name(), variable);
            }
        }
        List<StagingElement> siblings = mappings.computeIfAbsent(parent, n -> new ArrayList<>());
        if (element instanceof StagingElements elements) {
            siblings.addAll(elements.nested());
        } else {
            siblings.add(element);
        }
        scopes.pop();
    }

    private static StagingAction createAction(String name,
                                              Map<String, String> attrs,
                                              List<StagingElement> children,
                                              String text) {

        Supplier<ActionIterators> iterators = () -> firstChild(children, ActionIterators.class, () -> null);
        Supplier<TemplateModel> model = () -> firstChild(children, TemplateModel.class, TemplateModel::empty);
        Supplier<List<Mapper>> mappers = () -> filterChildren(children, Mapper.class);
        return switch (name) {
            case "directory" -> new StagingDirectory(filterChildren(children, StagingAction.class), attrs);
            case "unpack" -> new UnpackTask(iterators.get(), mappers.get(), attrs);
            case "unpack-artifact" -> new UnpackArtifactTask(iterators.get(), mappers.get(), attrs);
            case "copy" -> new CopyTask(iterators.get(), filterChildren(children, Include.class),
                    filterChildren(children, Exclude.class), attrs);
            case "copy-artifact" -> new CopyArtifactTask(iterators.get(), attrs);
            case "symlink" -> new SymlinkTask(iterators.get(), attrs);
            case "download" -> new DownloadTask(iterators.get(), attrs);
            case "archive" -> new ArchiveTask(iterators.get(), filterChildren(children, StagingAction.class), attrs);
            case "template" -> new TemplateTask(iterators.get(), attrs, model.get());
            case "file" -> new FileTask(iterators.get(), filterChildren(children, TextAction.class), attrs, text);
            case "list-files" -> {
                List<Include> includes = filterChildren(children, Include.class);
                List<Exclude> excludes = filterChildren(children, Exclude.class);
                List<Substitution> substitutions = filterChildren(children, Substitution.class);
                yield new ListFilesTask(iterators.get(), includes, excludes, substitutions, attrs);
            }
            default -> {
                if (WRAPPER_ELEMENTS.containsKey(name)) {
                    yield new StagingTasks(name, filterChildren(children, StagingAction.class), attrs);
                }
                throw new IllegalStateException("Unknown action: " + name);
            }
        };
    }

    private static StagingElement createDefault(String name,
                                                Map<String, String> attrs,
                                                List<StagingElement> children,
                                                String text) {

        if (SYNTHETIC_ELEMENTS.contains(name)) {
            return synthetic(name, attrs, text);
        } else if (ACTION_ELEMENTS.contains(name)) {
            return createAction(name, attrs, children, text);
        } else if (WRAPPER_ELEMENTS.containsKey(name)) {
            String wrapped = WRAPPER_ELEMENTS.get(name);
            if (SYNTHETIC_ELEMENTS.contains(wrapped)) {
                return new StagingElements(name, filterChildren(children, StagingElement.class));
            }
            return createAction(name, attrs, children, text);
        }
        throw new IllegalStateException("Unknown element: " + name);
    }

    private static Variables variables(Map<String, String> attrs, List<StagingElement> children) {
        return new Variables(filterChildren(children, Variable.class), attrs);
    }

    private static Variable variable(XMLElement elt, List<StagingElement> children, Scope scope) {
        Map<String, String> attrs = elt.attributes();
        if (attrs.containsKey("ref")) {
            Variable resolved = scope.resolve(attrs.get("ref"));
            String name = attrs.getOrDefault("name", resolved.name());
            return new Variable(name, resolved.value());
        }
        for (XMLElement child : elt.children()) {
            if ("variable".equals(child.name()) && !child.attributes().containsKey("ref")) {
                throw new IllegalArgumentException(
                        "Aggregate variable '" + attrs.get("name") + "' requires direct variable children to use ref");
            }
        }
        return variable(attrs.get("name"), children, attrs.get("value"));
    }

    private static Variable variable(String name, List<StagingElement> children, String text) {
        List<VariableValue> values = filterChildren(children, VariableValue.class);
        List<Variable> variables = filterChildren(children, Variable.class);
        if (!values.isEmpty() && !variables.isEmpty()) {
            throw new IllegalArgumentException(
                    "Variable '%s' cannot mix <value> and <variable> children"
                            .formatted(name));
        }
        if (!variables.isEmpty()) {
            return aggregateVariable(name, variables);
        }
        if (!values.isEmpty()) {
            return new Variable(name, new VariableValue.ListValue(values));
        }
        if (text == null) {
            return new Variable(name, new VariableValue.EmptyValue(name));
        }
        return new Variable(name, new VariableValue.SimpleValue(text));
    }

    private static Variable aggregateVariable(String name, List<Variable> variables) {
        List<VariableValue> aggregate = new ArrayList<>();
        for (Variable variable : variables) {
            if (!(variable.value() instanceof VariableValue.ListValue list)) {
                throw new IllegalArgumentException(
                        "Aggregate variable '" + name + "' requires list-valued reference '" + variable.name() + "'");
            }
            aggregate.addAll(list.values());
        }
        return new Variable(name, new VariableValue.ListValue(aggregate));
    }

    private static boolean hasReferencedVariableAncestor(XMLElement elt) {
        XMLElement p = elt.parent();
        while (p != null) {
            if ("variable".equals(p.name()) && p.attributes().containsKey("ref")) {
                return true;
            }
            p = p.parent();
        }
        return false;
    }

    private static boolean hasModelAncestor(XMLElement elt) {
        XMLElement parent = elt.parent();
        while (parent != null) {
            if ("model".equals(parent.name())) {
                return true;
            }
            parent = parent.parent();
        }
        return false;
    }

    private static VariableValue variableValue(List<StagingElement> children, String text) {
        List<Variable> variables = filterChildren(children, Variable.class);
        if (!variables.isEmpty()) {
            return new VariableValue.MapValue(variables);
        }
        return new VariableValue.SimpleValue(text);
    }

    private static ActionIterators actionIterators(List<StagingElement> children, Map<String, String> attrs) {
        List<ActionIterator> iterators = filterChildren(children, Variables.class)
                .stream()
                .map(ActionIterator::new)
                .collect(toList());
        return new ActionIterators(iterators, attrs);
    }

    private static StagingElement synthetic(String name, Map<String, String> attrs, String text) {
        return switch (name) {
            case "include" -> new Include(text);
            case "exclude" -> new Exclude(text);
            case "mapper" -> new Mapper(attrs);
            case "substitution" -> new Substitution(attrs);
            default -> throw new IllegalStateException("Unknown element: " + name);
        };
    }

    private static <T> List<T> filterChildren(List<StagingElement> children, Class<T> type) {
        return children.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(toList());
    }

    private static <T> T firstChild(List<StagingElement> children, Class<T> type, Supplier<T> defaultValue) {
        return filterChildren(children, type)
                .stream()
                .findFirst()
                .orElse(defaultValue.get());
    }

    private static final class Scope {

        private final String name;
        private final Scope parent;
        private final Map<String, Variable> variables;

        Scope(String name, Scope parent) {
            this.name = name;
            this.parent = parent;
            this.variables = new HashMap<>();
        }

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
                    + ", parent=" + (parent != null ? parent.name : null)
                    + ", variables=" + variables
                    + '}';
        }
    }
}
