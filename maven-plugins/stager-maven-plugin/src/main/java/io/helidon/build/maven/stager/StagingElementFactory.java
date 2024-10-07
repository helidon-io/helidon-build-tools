/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.build.maven.stager.ConfigReader.Scope;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Staging element factory.
 */
class StagingElementFactory {

    private static final Set<String> ACTION_ELEMENTS = Set.of(
            StagingDirectory.ELEMENT_NAME,
            ArchiveTask.ELEMENT_NAME,
            DownloadTask.ELEMENT_NAME,
            CopyArtifactTask.ELEMENT_NAME,
            FileTask.ELEMENT_NAME,
            SymlinkTask.ELEMENT_NAME,
            TemplateTask.ELEMENT_NAME,
            UnpackTask.ELEMENT_NAME,
            UnpackArtifactTask.ELEMENT_NAME,
            ListFilesTask.ELEMENT_NAME);

    private static final Set<String> SYNTHETIC_ELEMENTS = Set.of(
            Include.ELEMENT_NAME,
            Exclude.ELEMENT_NAME,
            Substitution.ELEMENT_NAME);

    private static final Map<String, String> WRAPPER_ELEMENTS = Stream.of(ACTION_ELEMENTS, SYNTHETIC_ELEMENTS)
            .flatMap(Collection::stream)
            .filter(n -> !n.endsWith("s"))
            .collect(toMap(n -> n.endsWith("y") ? n.substring(0, n.length() - 1) + "ies" : n + "s", n -> n));

    /**
     * Create a staging element.
     *
     * @param name     element name, never {@code null}
     * @param attrs    attributes, never {@code null}
     * @param children nested children {@code null}
     * @param text     element text, may be {@code null}
     * @param scope    variables scope
     * @return StagingAction
     */
    StagingElement create(String name,
                          Map<String, String> attrs,
                          List<StagingElement> children,
                          String text,
                          Scope scope) {

        return switch (name) {
            case Variables.ELEMENT_NAME -> variables(attrs, children);
            case Variable.ELEMENT_NAME -> variable(attrs, children, scope);
            case VariableValue.ELEMENT_NAME -> variableValue(children, text);
            case ActionIterators.ELEMENT_NAME -> actionIterators(children, attrs);
            default -> createDefault(name, attrs, children, text);
        };
    }

    /**
     * Create a new action.
     *
     * @param name     element name, should not be {@code null}
     * @param attrs    element attributes, should not be {@code null}
     * @param children child elements to process, should not be {@code null}
     * @param text     element text, may be {@code null}
     * @return action
     */
    StagingAction createAction(String name,
                               Map<String, String> attrs,
                               List<StagingElement> children,
                               String text) {

        Supplier<ActionIterators> iterators = () -> firstChild(children, ActionIterators.class, () -> null);
        Supplier<Variables> variables = () -> firstChild(children, Variables.class, Variables::new);
        switch (name) {
            case StagingDirectory.ELEMENT_NAME:
                return new StagingDirectory(filterChildren(children, StagingAction.class), attrs);
            case UnpackTask.ELEMENT_NAME:
                return new UnpackTask(iterators.get(), attrs);
            case UnpackArtifactTask.ELEMENT_NAME:
                return new UnpackArtifactTask(iterators.get(), attrs);
            case CopyArtifactTask.ELEMENT_NAME:
                return new CopyArtifactTask(iterators.get(), attrs);
            case SymlinkTask.ELEMENT_NAME:
                return new SymlinkTask(iterators.get(), attrs);
            case DownloadTask.ELEMENT_NAME:
                return new DownloadTask(iterators.get(), attrs);
            case ArchiveTask.ELEMENT_NAME:
                return new ArchiveTask(iterators.get(), filterChildren(children, StagingAction.class), attrs);
            case TemplateTask.ELEMENT_NAME:
                return new TemplateTask(iterators.get(), attrs, variables.get());
            case FileTask.ELEMENT_NAME:
                return new FileTask(iterators.get(), filterChildren(children, TextAction.class), attrs, text);
            case ListFilesTask.ELEMENT_NAME:
                List<Include> includes = filterChildren(children, Include.class);
                List<Exclude> excludes = filterChildren(children, Exclude.class);
                List<Substitution> substitutions = filterChildren(children, Substitution.class);
                return new ListFilesTask(iterators.get(), includes, excludes, substitutions, attrs);
            default:
                if (WRAPPER_ELEMENTS.containsKey(name)) {
                    return new StagingTasks(name, filterChildren(children, StagingAction.class), attrs);
                }
                throw new IllegalStateException("Unknown action: " + name);
        }
    }

    private StagingElement createDefault(String name,
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

    private static Variable variable(Map<String, String> attrs, List<StagingElement> children, Scope scope) {
        if (attrs.containsKey("ref")) {
            return scope.resolve(attrs.get("ref"));
        }
        return variable(attrs.get("name"), children, attrs.get("value"));
    }

    private static Variable variable(String name, List<StagingElement> children, String text) {
        List<VariableValue> values = filterChildren(children, VariableValue.class);
        if (!values.isEmpty()) {
            return new Variable(name, new VariableValue.ListValue(values));
        }
        return new Variable(name, new VariableValue.SimpleValue(text));
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
            case Include.ELEMENT_NAME -> new Include(text);
            case Exclude.ELEMENT_NAME -> new Exclude(text);
            case Substitution.ELEMENT_NAME -> new Substitution(attrs);
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
}
