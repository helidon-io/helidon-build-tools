/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Helidon archetype XML descriptor.
 */
public final class ArchetypeDescriptor {

    private final LinkedList<Property> properties = new LinkedList<>();
    private final LinkedList<Transformation> transformations = new LinkedList<>();
    private TemplateSets templateSets;
    private FileSets fileSets;
    private final InputFlow inputFlow = new InputFlow();

    ArchetypeDescriptor() {
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
     * Get the archetype properties.
     *
     * @return list of {@link Property}, never {@code null}
     */
    public LinkedList<Property> properties() {
        return properties;
    }

    /**
     * Get the transformations.
     *
     * @return list of {@link Transformation}, never {@code null}
     */
    public LinkedList<Transformation> transformations() {
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
     * Set the template sets.
     * @param templateSets template sets
     */
    void templateSets(TemplateSets templateSets) {
        this.templateSets = Objects.requireNonNull(templateSets, "templateSets is null");
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
     * Set the file sets.
     * @param fileSets file sets
     */
    void fileSets(FileSets fileSets) {
        this.fileSets = Objects.requireNonNull(fileSets, "fileSets is null");
    }

    /**
     * Get the input flow.
     *
     * @return input flow, never {@code null}
     */
    public InputFlow inputFlow() {
        return inputFlow;
    }

    public static final class Property {

        private final String id;
        private final String description;
        private final Optional<String> defaultValue;

        Property(String id, String description, String defaultValue) {
            this.id = Objects.requireNonNull(id, "id is null");
            this.description = Objects.requireNonNull(description, "description is null");
            this.defaultValue = Optional.ofNullable(defaultValue);
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
         * get the property description.
         *
         * @return description, never {@code null}
         */
        public String description() {
            return description;
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
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + Objects.hashCode(this.id);
            hash = 29 * hash + Objects.hashCode(this.description);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Property other = (Property) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return Objects.equals(this.description, other.description);
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
         * @return linked list of replacement, never {@code null}
         */
        public LinkedList<Replacement> replacements() {
            return replacements;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + Objects.hashCode(this.id);
            hash = 41 * hash + Objects.hashCode(this.replacements);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Transformation other = (Transformation) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return Objects.equals(this.replacements, other.replacements);
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
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.regex);
            hash = 23 * hash + Objects.hashCode(this.replacement);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Replacement other = (Replacement) obj;
            if (!Objects.equals(this.regex, other.regex)) {
                return false;
            }
            return Objects.equals(this.replacement, other.replacement);
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
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.ifProperties);
            hash = 17 * hash + Objects.hashCode(this.unlessProperties);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Conditional other = (Conditional) obj;
            if (!Objects.equals(this.ifProperties, other.ifProperties)) {
                return false;
            }
            return Objects.equals(this.unlessProperties, other.unlessProperties);
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
        public int hashCode() {
            int hash = 5;
            hash = 31 * hash + Objects.hashCode(this.templateSets);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals((PathSets) obj)) {
                return false;
            }
            final TemplateSets other = (TemplateSets) obj;
            return Objects.equals(this.templateSets, other.templateSets);
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
        public int hashCode() {
            int hash = 5;
            hash = 31 * hash + Objects.hashCode(this.fileSets);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals((PathSets) obj)) {
                return false;
            }
            final FileSets other = (FileSets) obj;
            return Objects.equals(this.fileSets, other.fileSets);
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
        public int hashCode() {
            int hash = 3;
            hash = 11 * hash + Objects.hashCode(this.transformations);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PathSets other = (PathSets) obj;
            return Objects.equals(this.transformations, other.transformations);
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
        public int hashCode() {
            int hash = 7;
            hash += super.hashCode();
            hash = 67 * hash + Objects.hashCode(this.transformations);
            hash = 67 * hash + Objects.hashCode(this.directory);
            hash = 67 * hash + Objects.hashCode(this.includes);
            hash = 67 * hash + Objects.hashCode(this.excludes);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final FileSet other = (FileSet) obj;
            if (!Objects.equals(this.directory, other.directory)) {
                return false;
            }
            if (!Objects.equals(this.transformations, other.transformations)) {
                return false;
            }
            if (!Objects.equals(this.includes, other.includes)) {
                return false;
            }
            return Objects.equals(this.excludes, other.excludes);
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
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.nodes);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final InputFlow other = (InputFlow) obj;
            return Objects.equals(this.nodes, other.nodes);
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
        public int hashCode() {
            int hash = 7;
            hash += super.hashCode();
            hash = 11 * hash + Objects.hashCode(this.text);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final FlowNode other = (FlowNode) obj;
            return Objects.equals(this.text, other.text);
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
        public int hashCode() {
            int hash = 7;
            hash += super.hashCode();
            hash = 89 * hash + Objects.hashCode(this.choices);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final Select other = (Select) obj;
            return Objects.equals(this.choices, other.choices);
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
        public int hashCode() {
            int hash = 7;
            hash += super.hashCode();
            hash = 79 * hash + Objects.hashCode(this.property);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final Choice other = (Choice) obj;
            return Objects.equals(this.property, other.property);
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
        public int hashCode() {
            int hash = 5;
            hash += super.hashCode();
            hash = 17 * hash + Objects.hashCode(this.property);
            hash = 17 * hash + Objects.hashCode(this.defaultValue);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final Input other = (Input) obj;
            if (!Objects.equals(this.defaultValue, other.defaultValue)) {
                return false;
            }
            return Objects.equals(this.property, other.property);
        }
    }
}
