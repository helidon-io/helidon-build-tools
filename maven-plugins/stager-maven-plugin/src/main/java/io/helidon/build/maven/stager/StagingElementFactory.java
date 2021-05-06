/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Staging element factory.
 */
class StagingElementFactory {

    private static final List<String> WRAPPED_ELEMENTS = List.of(
            ArchiveTask.ELEMENT_NAME,
            DownloadTask.ELEMENT_NAME,
            CopyArtifactTask.ELEMENT_NAME,
            FileTask.ELEMENT_NAME,
            SymlinkTask.ELEMENT_NAME,
            TemplateTask.ELEMENT_NAME,
            UnpackArtifactTask.ELEMENT_NAME);

    private static final Map<String, String> WRAPPER_ELEMENTS = WRAPPED_ELEMENTS
            .stream()
            .collect(Collectors.toMap(n -> n + "s", n -> n));

    /**
     * Create a staging element.
     *
     * @param name     element name, never {@code null}
     * @param attrs    attributes, never {@code null}
     * @param children nested children {@code null}
     * @param text     element text, may be {@code null}
     * @return StagingAction
     */
    StagingElement create(String name,
                          Map<String, String> attrs,
                          Map<String, List<StagingElement>> children,
                          String text) {

        switch (name) {
            case StagingDirectory.ELEMENT_NAME:
            case UnpackArtifactTask.ELEMENT_NAME:
            case CopyArtifactTask.ELEMENT_NAME:
            case SymlinkTask.ELEMENT_NAME:
            case DownloadTask.ELEMENT_NAME:
            case ArchiveTask.ELEMENT_NAME:
            case TemplateTask.ELEMENT_NAME:
            case FileTask.ELEMENT_NAME:
                return createAction(name, attrs, children, text);
            case ActionIterators.ELEMENT_NAME:
                return actionIterators(children);
            case Variables.ELEMENT_NAME:
                return variables(children);
            case Variable.ELEMENT_NAME:
                return variable(attrs.get("name"), children, attrs.get("value"));
            case VariableValue.ELEMENT_NAME:
                return variableValue(children, text);
            default:
                throw new IllegalStateException("Unknown element: " + name);
        }
    }

    /**
     * Test if the given element name is a wrapper element.
     *
     * @param name element name
     * @return {@code true} if the name is a wrapper element, {@code false} otherwise
     */
    boolean isWrapperElement(String name) {
        return WRAPPER_ELEMENTS.keySet().contains(name);
    }

    /**
     * Get the name of the wrapped element for the given wrapper element name.
     *
     * @param name wrapper element name
     * @return wrapped element name
     */
    String wrappedElementName(String name) {
        return WRAPPER_ELEMENTS.get(name);
    }

    /**
     * Create a new action iterators using the variables found in the given children.
     *
     * @param children child elements to process, should not be {@code null}
     * @return ActionIterators, never {@code null}
     */
    ActionIterators actionIterators(Map<String, List<StagingElement>> children) {
        return new ActionIterators(filterChildren(children, Variables.ELEMENT_NAME, Variables.class)
                .stream()
                .map(ActionIterator::new)
                .collect(Collectors.toList()));
    }

    /**
     * Create a new variables instance from the variable found in the given children.
     *
     * @param children child elements to process, should not be {@code null}
     * @return Variables, never {@code null}
     */
    Variables variables(Map<String, List<StagingElement>> children) {
        return new Variables(filterChildren(children, Variable.ELEMENT_NAME, Variable.class));
    }

    /**
     * Create a new variable instance from the variable values found in the given children.
     *
     * @param children child elements to process, should not be {@code null}
     * @return Variable, never {@code null}
     */
    Variable variable(String name, Map<String, List<StagingElement>> children, String text) {
        List<VariableValue> values = filterChildren(children, VariableValue.ELEMENT_NAME, VariableValue.class);
        if (!values.isEmpty()) {
            return new Variable(name, new VariableValue.ListValue(values));
        }
        return new Variable(name, new VariableValue.SimpleValue(text));
    }

    /**
     * Create a new variable value instance from the given children.
     *
     * @param children child elements to process, should not be {@code null}
     * @return VariableValue, never {@code null}
     */
    VariableValue variableValue(Map<String, List<StagingElement>> children, String text) {
        List<Variable> variables = filterChildren(children, Variable.ELEMENT_NAME, Variable.class);
        if (!variables.isEmpty()) {
            return new VariableValue.MapValue(variables);
        }
        return new VariableValue.SimpleValue(text);
    }

    /**
     * Create a new action.
     *
     * @param name     element name, should not be {@code null}
     * @param attrs    element attributes, should not be {@code null}
     * @param children child elements to process, should not be {@code null}
     * @param text     element text, may be {@code null}
     * @return
     */
    StagingAction createAction(String name,
                               Map<String, String> attrs,
                               Map<String, List<StagingElement>> children,
                               String text) {

        Supplier<ActionIterators> iterators = () -> firstChild(children, ActionIterators.class, () -> null);
        Supplier<Variables> variables = () -> firstChild(children, Variables.class, Variables::new);
        switch (name) {
            case StagingDirectory.ELEMENT_NAME:
                return new StagingDirectory(attrs.get("target"), filterChildren(children, StagingAction.class));
            case UnpackArtifactTask.ELEMENT_NAME:
                return new UnpackArtifactTask(iterators.get(),
                        new ArtifactGAV(attrs),
                        attrs.get("target"),
                        attrs.get("includes"),
                        attrs.get("excludes"));
            case CopyArtifactTask.ELEMENT_NAME:
                return new CopyArtifactTask(iterators.get(), new ArtifactGAV(attrs), attrs.get("target"));
            case SymlinkTask.ELEMENT_NAME:
                return new SymlinkTask(iterators.get(), attrs.get("source"), attrs.get("target"));
            case DownloadTask.ELEMENT_NAME:
                return new DownloadTask(iterators.get(), attrs.get("url"), attrs.get("target"));
            case ArchiveTask.ELEMENT_NAME:
                return new ArchiveTask(iterators.get(),
                        filterChildren(children, StagingAction.class),
                        attrs.get("target"),
                        attrs.get("includes"),
                        attrs.get("excludes"));
            case TemplateTask.ELEMENT_NAME:
                return new TemplateTask(iterators.get(), attrs.get("source"), attrs.get("target"), variables.get());
            case FileTask.ELEMENT_NAME:
                return new FileTask(iterators.get(), attrs.get("target"), text, attrs.get("source"));
            default:
                throw new IllegalStateException("Unknown action: " + name);
        }
    }

    /**
     * Filter the children with the given element name and type.
     *
     * @param mappings    children mappings
     * @param elementName map key to filter
     * @param type        type of the child to include
     * @param <T>         type parameter
     * @return list of children, never {@code null}
     */
    <T> List<T> filterChildren(Map<String, List<StagingElement>> mappings, String elementName, Class<T> type) {
        return Optional.ofNullable(mappings.get(elementName))
                .map(List::stream)
                .orElseGet(Stream::of)
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    /**
     * Filter the children with the given type.
     *
     * @param mappings children mappings
     * @param type     type of the child to include
     * @param <T>      type parameter
     * @return list of children, never {@code null}
     */
    <T> List<T> filterChildren(Map<String, List<StagingElement>> mappings, Class<T> type) {
        return Optional.ofNullable(mappings.values())
                .map(Collection::stream)
                .orElseGet(Stream::of)
                .flatMap(List::stream)
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    <T> T firstChild(Map<String, List<StagingElement>> mappings, Class<T> type, Supplier<T> defaultValue) {
        return filterChildren(mappings, type)
                .stream()
                .findFirst()
                .orElse(defaultValue.get());
    }
}
