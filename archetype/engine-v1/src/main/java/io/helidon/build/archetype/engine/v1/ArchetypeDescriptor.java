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
package io.helidon.build.archetype.engine.v1;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private final List<Property> properties;
    private final List<Transformation> transformations;
    private final TemplateSets templateSets;
    private final FileSets fileSets;
    private final InputFlow inputFlow;

    ArchetypeDescriptor(String modelVersion,
                        String name,
                        List<Property> properties,
                        List<Transformation> transformations,
                        TemplateSets templateSets,
                        FileSets fileSets,
                        InputFlow inputFlow) {

        this.modelVersion = Objects.requireNonNull(modelVersion, "modelVersion is null");
        this.name = Objects.requireNonNull(name, "name is null");
        this.properties = Objects.requireNonNull(properties, "properties is null");
        this.transformations = Objects.requireNonNull(transformations, "transformations is null");
        this.templateSets = templateSets;
        this.fileSets = fileSets;
        this.inputFlow = Objects.requireNonNull(inputFlow, "inputFlow is null");
    }

    /**
     * Create a archetype descriptor instance from an input stream.
     *
     * @param is input stream
     * @return ArchetypeDescriptor
     */
    public static ArchetypeDescriptor read(InputStream is) {
        return ArchetypeDescriptorReader.read(is);
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
     * @return list of {@link Property}, never {@code null}
     */
    public List<Property> properties() {
        return properties;
    }

    /**
     * Get the transformations.
     *
     * @return list of {@link Transformation}, never {@code null}
     */
    public List<Transformation> transformations() {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
            this(id, value, true, false);
        }

        Property(String id, String value, boolean exported, boolean readonly) {
            this.id = Objects.requireNonNull(id, "id is null");
            this.exported = exported;
            this.readonly = readonly;
            if (readonly && (value == null || value.isEmpty())) {
                throw new IllegalArgumentException("A readonly property requires a value");
            }
            this.value = value;
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
         * Properties marked as not exported are excluded converting to other descriptor formats
         * (e.g. {@code maven/archetype-metadata.xml}.
         *
         * @return {@code true} if exported, {@code false} otherwise
         */
        public boolean isExported() {
            return exported;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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
     *  transformation, a pipeline of string replacement operations.
     */
    public static final class Transformation {

        private final String id;
        private final LinkedList<Replacement> replacements;

        Transformation(String id) {
            this.id = Objects.requireNonNull(id, "id is null");
            this.replacements = new LinkedList<>();
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
        public LinkedList<Replacement> replacements() {
            return replacements;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Transformation that = (Transformation) o;
            return id.equals(that.id)
                    && replacements.equals(that.replacements);
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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

        Conditional(List<Property> ifProperties, List<Property> unlessProperties) {
            this.ifProperties = Objects.requireNonNull(ifProperties, "ifProperties is null");
            this.unlessProperties = Objects.requireNonNull(unlessProperties, "unlessProperties is null");
        }

        /**
         * Get the if properties.
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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

        private final LinkedList<FileSet> templateSets = new LinkedList<>();

        TemplateSets(List<Transformation> transformations) {
            super(transformations);
        }

        /**
         * Get the template sets.
         *
         * @return list of file set, never {@code null}
         */
        public LinkedList<FileSet> templateSets() {
            return templateSets;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            TemplateSets that = (TemplateSets) o;
            return templateSets.equals(that.templateSets);
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

        private final LinkedList<FileSet> fileSets = new LinkedList<>();

        FileSets(List<Transformation> transformations) {
            super(transformations);
        }

        /**
         * Get the file sets.
         *
         * @return list of file set, never {@code null}
         */
        public LinkedList<FileSet> fileSets() {
            return fileSets;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            FileSets fileSets1 = (FileSets) o;
            return fileSets.equals(fileSets1.fileSets);
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

        protected PathSets(List<Transformation> transformations) {
            this.transformations = Objects.requireNonNull(transformations, "transformations is null");
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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
        private final LinkedList<String> includes;
        private final LinkedList<String> excludes;
        private String directory;

        FileSet(List<Transformation> transformations, List<Property> ifProperties, List<Property> unlessProperties) {
            super(ifProperties, unlessProperties);
            this.transformations = Objects.requireNonNull(transformations, "transformations is null");
            this.includes = new LinkedList<>();
            this.excludes = new LinkedList<>();
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
         * Set the directory.
         * @param directory directory
         */
        void directory(String directory) {
            this.directory = Objects.requireNonNull(directory, "directory is null");
        }

        /**
         * Get the exclude filters.
         *
         * @return list of exclude filter, never {@code null}
         */
        public LinkedList<String> excludes() {
            return excludes;
        }

        /**
         * Get the include filters.
         *
         * @return list of include filter, never {@code null}
         */
        public LinkedList<String> includes() {
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
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

        private final LinkedList<FlowNode> nodes = new LinkedList<>();

        /**
         * Get the flow nodes.
         *
         * @return list of flow node, never {@code null}
         */
        public LinkedList<FlowNode> nodes() {
            return nodes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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

        protected FlowNode(String text, List<Property> ifProperties, List<Property> unlessProperties) {
            super(ifProperties, unlessProperties);
            this.text = Objects.requireNonNull(text, "text is null");
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
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

        private final LinkedList<Choice> choices;

        Select(String text, List<Property> ifProperties, List<Property> unlessProperties) {
            super(text, ifProperties, unlessProperties);
            this.choices = new LinkedList<>();
        }

        /**
         * Get the choices.
         *
         * @return list of {@code Choice}, never {@code null}
         */
        public LinkedList<Choice> choices() {
            return choices;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
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

        Choice(Property property, String text, List<Property> ifProperties, List<Property> unlessProperties) {
            super(text, ifProperties, unlessProperties);
            this.property = Objects.requireNonNull(property, "property is null");
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
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
        private final Optional<String> defaultValue;

        Input(Property property, String defaultValue, String text, List<Property> ifProperties,
                List<Property> unlessProperties) {

            super(text, ifProperties, unlessProperties);
            this.property = Objects.requireNonNull(property, "property is null");
            if (property.isReadonly()) {
                throw new IllegalArgumentException("Property: " + property.id() + " is readonly");
            }
            this.defaultValue = Optional.ofNullable(defaultValue);
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
            return defaultValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
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
