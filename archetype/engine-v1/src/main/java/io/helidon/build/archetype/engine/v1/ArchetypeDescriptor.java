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
package io.helidon.build.archetype.engine.v1;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.xml.XMLElement;

/**
 * Helidon archetype V1 descriptor.
 */
public final class ArchetypeDescriptor {

    /**
     * The current version of the archetype descriptor model.
     */
    public static final String MODEL_VERSION = "1.0";

    private final String modelVersion;
    private final String name;
    private final Map<String, Property> properties;
    private final Map<String, Transformation> transformations;
    private final TemplateSets templateSets;
    private final FileSets fileSets;
    private final InputFlow inputFlow;

    ArchetypeDescriptor(XMLElement elt) {
        if (!"archetype-descriptor".equals(elt.name())) {
            throw new IllegalStateException("Invalid root element " + elt.name());
        }
        modelVersion = elt.attribute("modelVersion");
        name = elt.attribute("name");
        properties = elt.child("properties")
                .map(e -> Maps.from(e.children("property"), Property::id, Property::new))
                .orElse(Map.of());
        transformations = elt.child("transformations")
                .map(e -> Maps.from(e.children("transformation"), Transformation::id, Transformation::new))
                .orElse(Map.of());
        templateSets = elt.child("template-sets")
                .map(e -> new TemplateSets(e, transformations, properties))
                .orElse(null);
        fileSets = elt.child("file-sets")
                .map(e -> new FileSets(e, transformations, properties))
                .orElse(null);
        inputFlow = elt.child("input-flow")
                .map(e -> new InputFlow(e, properties))
                .orElseThrow(() -> new IllegalStateException("Missing input flow"));
    }

    /**
     * Create an archetype descriptor instance from an input stream.
     *
     * @param is input stream
     * @return ArchetypeDescriptor
     */
    public static ArchetypeDescriptor read(InputStream is) {
        try {
            XMLElement elt = XMLElement.parse(is);
            return new ArchetypeDescriptor(elt);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the archetype descriptor model version.
     *
     * @return model version, never {@code null}
     */
    public String modelVersion() {
        return modelVersion;
    }

    /**
     * Get the archetype descriptor name.
     * The name is informative only, the source of truth is the catalog.
     *
     * @return name, never {@code null}
     * @see ArchetypeCatalog.ArchetypeEntry#name()
     */
    public String name() {
        return name;
    }

    /**
     * Get the archetype properties.
     *
     * @return map of {@link Property}, never {@code null}
     */
    public Map<String, Property> properties() {
        return properties;
    }

    /**
     * Get the transformations.
     *
     * @return map of {@link Transformation}, never {@code null}
     */
    public Map<String, Transformation> transformations() {
        return transformations;
    }

    /**
     * Get the template sets.
     *
     * @return optional of template sets, never {@code null}
     */
    public Optional<TemplateSets> templateSets() {
        return Optional.ofNullable(templateSets);
    }

    /**
     * Get the file sets.
     *
     * @return optional of file sets, never {@code null}
     */
    public Optional<FileSets> fileSets() {
        return Optional.ofNullable(fileSets);
    }

    /**
     * Get the input flow.
     *
     * @return input flow, never {@code null}
     */
    public InputFlow inputFlow() {
        return inputFlow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArchetypeDescriptor that = (ArchetypeDescriptor) o;
        return modelVersion.equals(that.modelVersion)
               && properties.equals(that.properties)
               && transformations.equals(that.transformations)
               && Objects.equals(templateSets, that.templateSets)
               && Objects.equals(fileSets, that.fileSets)
               && inputFlow.equals(that.inputFlow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelVersion, properties, transformations, templateSets, fileSets, inputFlow);
    }

    @Override
    public String toString() {
        return "ArchetypeDescriptor{"
               + "modelVersion='" + modelVersion + '\''
               + ", name='" + name + '\''
               + '}';
    }

    /**
     * Archetype property.
     */
    public static final class Property {

        private final String id;
        private final String value;
        private final boolean exported;
        private final boolean readonly;

        Property(String id, String value) {
            this.id = Objects.requireNonNull(id, "id is null");
            this.value = value;
            this.exported = true;
            this.readonly = false;
        }

        Property(XMLElement elt) {
            id = elt.attribute("id");
            value = elt.attribute("value", null);
            exported = elt.attributeBoolean("exported", true);
            readonly = elt.attributeBoolean("readonly", false);
        }

        /**
         * Get the property id.
         *
         * @return property id, never {@code null}
         */
        public String id() {
            return id;
        }

        /**
         * Get the default value.
         *
         * @return default value
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Indicate if this property is read only.
         * A read only property always a value ; the value cannot be overridden.
         *
         * @return {@code true} if read only, {@code false} otherwise
         */
        public boolean isReadonly() {
            return readonly;
        }

        /**
         * Indicate if this property is exported.
         * Properties marked as not exported are excluded converting to other descriptor formats.
         * E.g. {@code maven/archetype-metadata.xml}.
         *
         * @return {@code true} if exported, {@code false} otherwise
         */
        public boolean isExported() {
            return exported;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Property property = (Property) o;
            return exported == property.exported
                   && readonly == property.readonly
                   && id.equals(property.id)
                   && Objects.equals(value, property.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value, exported, readonly);
        }

        @Override
        public String toString() {
            return "Property{"
                   + "id='" + id + '\''
                   + ", value='" + value + '\''
                   + ", exported=" + exported
                   + ", readonly=" + readonly
                   + '}';
        }
    }

    /**
     * transformation, a pipeline of string replacement operations.
     */
    public static final class Transformation {

        private final String id;
        private final List<Replacement> replacements;

        Transformation(String id, List<Replacement> replacements) {
            this.id = id;
            this.replacements = replacements;
        }

        private Transformation(XMLElement elt) {
            this.id = elt.attribute("id");
            this.replacements = elt.children("replace").stream()
                    .map(r -> new Replacement(
                            r.attribute("regex"),
                            r.attribute("replacement")))
                    .collect(Collectors.toList());
        }

        /**
         * Get the transformation id.
         *
         * @return transformation id, never {@code null}
         */
        public String id() {
            return id;
        }

        /**
         * Get the replacements.
         *
         * @return list of replacement, never {@code null}
         */
        public List<Replacement> replacements() {
            return replacements;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Transformation that = (Transformation) o;
            return id.equals(that.id) && replacements.equals(that.replacements);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, replacements);
        }

        @Override
        public String toString() {
            return "Transformation{"
                   + "id='" + id + '\''
                   + ", replacements=" + replacements
                   + '}';
        }
    }

    /**
     * Replace operation for a transformation.
     */
    public static final class Replacement {

        private final String regex;
        private final String replacement;

        Replacement(String regex, String replacement) {
            this.regex = Objects.requireNonNull(regex, "regex is null");
            this.replacement = Objects.requireNonNull(replacement, "replacement is null");
        }

        /**
         * Get the source regular expression to match the section to be replaced.
         *
         * @return regular expression, never {@code null}
         */
        public String regex() {
            return regex;
        }

        /**
         * Get the replacement for the matches of the source regular expression.
         *
         * @return replacement, never {@code null}
         */
        public String replacement() {
            return replacement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Replacement that = (Replacement) o;
            return regex.equals(that.regex)
                   && replacement.equals(that.replacement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regex, replacement);
        }

        @Override
        public String toString() {
            return "Replacement{"
                   + "regex='" + regex + '\''
                   + ", replacement='" + replacement + '\''
                   + '}';
        }
    }

    /**
     * Base class for conditional nodes.
     */
    public abstract static class Conditional {

        private final List<Property> ifProperties;
        private final List<Property> unlessProperties;

        private Conditional(List<Property> ifProperties, List<Property> unlessProperties) {
            this.ifProperties = ifProperties;
            this.unlessProperties = unlessProperties;
        }

        private Conditional(XMLElement elt, Map<String, Property> properties) {
            this.ifProperties = Lists.map(elt.attributeList("if", ","), it ->
                    Maps.getOrThrow(properties, it, k -> new IllegalStateException(String.format(
                            "Unknown property %s in %s", it, elt.name()))));
            this.unlessProperties = Lists.map(elt.attributeList("unless", ","), it ->
                    Maps.getOrThrow(properties, it, k -> new IllegalStateException(String.format(
                            "Unknown property %s in %s", it, elt.name()))));
        }

        /**
         * Get the {@code if} properties.
         *
         * @return list of properties, never {@code null}
         */
        public List<Property> ifProperties() {
            return ifProperties;
        }

        /**
         * Get the {@code unless} properties.
         *
         * @return list of properties, never {@code null}
         */
        public List<Property> unlessProperties() {
            return unlessProperties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Conditional that = (Conditional) o;
            return ifProperties.equals(that.ifProperties)
                   && unlessProperties.equals(that.unlessProperties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ifProperties, unlessProperties);
        }
    }

    /**
     * Set of included template files.
     */
    public static final class TemplateSets extends PathSets {

        private final List<FileSet> templateSets;

        TemplateSets(XMLElement elt, Map<String, Transformation> transformations, Map<String, Property> properties) {
            super(elt, transformations);
            templateSets = Lists.map(elt.children("template-set"), it -> new FileSet(it, transformations, properties));
        }

        /**
         * Get the template sets.
         *
         * @return list of file set, never {@code null}
         */
        public List<FileSet> templateSets() {
            return templateSets;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            return templateSets.equals(((TemplateSets) o).templateSets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), templateSets);
        }

        @Override
        public String toString() {
            return "TemplateSets{"
                   + "templateSets=" + templateSets
                   + ", transformations=" + transformations()
                   + '}';
        }
    }

    /**
     * Set of included template files.
     */
    public static final class FileSets extends PathSets {

        private final List<FileSet> fileSets;

        private FileSets(XMLElement elt, Map<String, Transformation> transformations, Map<String, Property> properties) {
            super(elt, transformations);
            fileSets = Lists.map(elt.children("file-set"), it -> new FileSet(it, transformations, properties));
        }

        /**
         * Get the file sets.
         *
         * @return list of file set, never {@code null}
         */
        public List<FileSet> fileSets() {
            return fileSets;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            return fileSets.equals(((FileSets) o).fileSets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), fileSets);
        }

        @Override
        public String toString() {
            return "FileSets{"
                   + "fileSets=" + fileSets
                   + ", transformations=" + transformations()
                   + '}';
        }
    }

    /**
     * Base class for {@link TemplateSets} and {@link FileSets}.
     */
    public abstract static class PathSets {

        private final List<Transformation> transformations;

        private PathSets(XMLElement elt, Map<String, Transformation> transformations) {
            this.transformations = Lists.map(elt.attributeList("transformations", ","), it ->
                    Maps.getOrThrow(transformations, it, k -> new IllegalStateException(String.format(
                            "Unknown transformation %s in %s", it, elt.name()))));
        }

        /**
         * Get the transformations.
         *
         * @return list of transformation, never {@code null}
         */
        public List<Transformation> transformations() {
            return transformations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PathSets pathSets = (PathSets) o;
            return transformations.equals(pathSets.transformations);
        }

        @Override
        public int hashCode() {
            return Objects.hash(transformations);
        }
    }

    /**
     * A list of included files.
     */
    public static final class FileSet extends Conditional {

        private final List<Transformation> transformations;
        private final List<String> includes;
        private final List<String> excludes;
        private final String directory;

        FileSet(String directory,
                List<String> includes,
                List<String> excludes,
                List<Transformation> transformations,
                List<Property> ifProperties,
                List<Property> unlessProperties) {

            super(ifProperties, unlessProperties);
            this.transformations = transformations;
            this.includes = includes;
            this.excludes = excludes;
            this.directory = directory;
        }

        private FileSet(XMLElement elt, Map<String, Transformation> transformations, Map<String, Property> properties) {
            super(elt, properties);
            this.directory = elt.child("directory").map(XMLElement::value).orElse(null);
            this.transformations = Lists.map(elt.attributeList("transformations", ","), it ->
                    Maps.getOrThrow(transformations, it, k -> new IllegalStateException(String.format(
                            "Unknown transformation %s in %s", it, elt.name()))));
            this.includes = Lists.map(elt.childrenAt("includes", "include"), XMLElement::value);
            this.excludes = Lists.map(elt.childrenAt("excludes", "exclude"), XMLElement::value);
        }

        /**
         * Get the directory of this file set.
         *
         * @return directory optional, never {@code null}
         */
        public Optional<String> directory() {
            return Optional.ofNullable(directory);
        }

        /**
         * Get the exclude filters.
         *
         * @return list of exclude filter, never {@code null}
         */
        public List<String> excludes() {
            return excludes;
        }

        /**
         * Get the include filters.
         *
         * @return list of include filter, never {@code null}
         */
        public List<String> includes() {
            return includes;
        }

        /**
         * Get the applied transformations.
         *
         * @return list of transformation, never {@code null}
         */
        public List<Transformation> transformations() {
            return transformations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            FileSet fileSet = (FileSet) o;
            return transformations.equals(fileSet.transformations)
                   && includes.equals(fileSet.includes)
                   && excludes.equals(fileSet.excludes)
                   && directory.equals(fileSet.directory);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), transformations, includes, excludes, directory);
        }

        @Override
        public String toString() {
            return "FileSet{"
                   + "ifProperties=" + ifProperties()
                   + ", unlessProperties=" + unlessProperties()
                   + ", transformations=" + transformations
                   + ", includes=" + includes
                   + ", excludes=" + excludes
                   + ", directory='" + directory + '\''
                   + '}';
        }
    }

    /**
     * User input flow.
     */
    public static final class InputFlow {

        private final List<FlowNode> nodes;

        private InputFlow(XMLElement elt, Map<String, Property> properties) {
            this.nodes = Lists.map(elt.children(), it -> {
                if ("select".equals(it.name())) {
                    return new Select(it, properties);
                }
                if ("input".equals(it.name())) {
                    return new Input(it, properties);
                }
                throw new IllegalStateException("Invalid input flow node: " + it.name());
            });
        }

        /**
         * Get the flow nodes.
         *
         * @return list of flow node, never {@code null}
         */
        public List<FlowNode> nodes() {
            return nodes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InputFlow inputFlow = (InputFlow) o;
            return Objects.equals(nodes, inputFlow.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodes);
        }

        @Override
        public String toString() {
            return "InputFlow{"
                   + "nodes=" + nodes
                   + '}';
        }
    }

    /**
     * Base class for flow nodes.
     */
    public abstract static class FlowNode extends Conditional {

        private final String text;

        private FlowNode(XMLElement elt, Map<String, Property> properties) {
            super(elt, properties);
            this.text = elt.attribute("text");
        }

        /**
         * Get the input text for this select.
         *
         * @return input text, never {@code null}
         */
        public String text() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            FlowNode flowNode = (FlowNode) o;
            return text.equals(flowNode.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), text);
        }
    }

    /**
     * Select input, one of N choices.
     */
    public static final class Select extends FlowNode {

        private final List<Choice> choices;

        private Select(XMLElement elt, Map<String, Property> properties) {
            super(elt, properties);
            this.choices = Lists.map(elt.children("choice"), it -> new Choice(it, properties));
        }

        /**
         * Get the choices.
         *
         * @return list of {@code Choice}, never {@code null}
         */
        public List<Choice> choices() {
            return choices;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            Select select = (Select) o;
            return choices.equals(select.choices);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), choices);
        }

        @Override
        public String toString() {
            return "Select{"
                   + "ifProperties=" + ifProperties()
                   + ", unlessProperties=" + unlessProperties()
                   + ", text='" + text() + '\''
                   + ", choices=" + choices
                   + '}';
        }
    }

    /**
     * A selectable choice.
     */
    public static final class Choice extends FlowNode {

        private final Property property;

        private Choice(XMLElement elt, Map<String, Property> properties) {
            super(elt, properties);
            this.property = Maps.getOrThrow(properties, elt.attribute("property"),
                    k -> new IllegalStateException(String.format(
                            "Unknown property %s in %s", k, elt.name())));
            if (property.isReadonly()) {
                throw new IllegalArgumentException("Property: " + property.id() + " is readonly");
            }
        }

        /**
         * Get the property mapping for this choice.
         *
         * @return property, never {@code null}
         */
        public Property property() {
            return property;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            Choice choice = (Choice) o;
            return Objects.equals(property, choice.property);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), property);
        }

        @Override
        public String toString() {
            return "Choice{"
                   + "ifProperties=" + ifProperties()
                   + ", unlessProperties=" + unlessProperties()
                   + ", text='" + text() + '\''
                   + ", property=" + property
                   + '}';
        }
    }

    /**
     * A user input.
     */
    public static final class Input extends FlowNode {

        private final Property property;
        private final String defaultValue;

        private Input(XMLElement elt, Map<String, Property> properties) {
            super(elt, properties);
            this.defaultValue = elt.attribute("default", null);
            this.property = Maps.getOrThrow(properties, elt.attribute("property"),
                    k -> new IllegalStateException(String.format(
                            "Unknown property %s in %s", k, elt.name())));
            if (property.isReadonly()) {
                throw new IllegalArgumentException("Property: " + property.id() + " is readonly");
            }
        }

        /**
         * Get the mapped property.
         *
         * @return Property, never {@code null}
         */
        public Property property() {
            return property;
        }

        /**
         * Get the default value.
         *
         * @return default value
         */
        public Optional<String> defaultValue() {
            return Optional.ofNullable(defaultValue);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            Input input = (Input) o;
            return property.equals(input.property)
                   && defaultValue.equals(input.defaultValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), property, defaultValue);
        }

        @Override
        public String toString() {
            return "Input{"
                   + "ifProperties=" + ifProperties()
                   + ", unlessProperties=" + unlessProperties()
                   + ", text='" + text() + '\''
                   + ", property=" + property
                   + ", defaultValue=" + defaultValue
                   + '}';
        }
    }
}
