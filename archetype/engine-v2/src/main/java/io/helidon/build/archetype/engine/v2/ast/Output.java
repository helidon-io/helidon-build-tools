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
package io.helidon.build.archetype.engine.v2.ast;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Output block.
 */
public abstract class Output extends Block {

    /**
     * Create a new output.
     *
     * @param builder builder
     */
    Output(Output.Builder builder) {
        super(builder);
    }

    /**
     * Output visitor.
     *
     * @param <A> argument type
     */
    public interface Visitor<A> {

        /**
         * Visit a transformation block.
         *
         * @param transformation transformation
         * @param arg            visitor argument
         * @return result
         */
        default VisitResult visitTransformation(Transformation transformation, A arg) {
            return visitAny(transformation, arg);
        }

        /**
         * Visit a files block.
         *
         * @param files files
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitFiles(Files files, A arg) {
            return visitAny(files, arg);
        }

        /**
         * Visit a templates block.
         *
         * @param templates templates
         * @param arg       visitor argument
         * @return result
         */
        default VisitResult visitTemplates(Templates templates, A arg) {
            return visitAny(templates, arg);
        }

        /**
         * Visit a file block.
         *
         * @param file file
         * @param arg  visitor argument
         * @return result
         */
        default VisitResult visitFile(File file, A arg) {
            return visitAny(file, arg);
        }

        /**
         * Visit a template block.
         *
         * @param template template
         * @param arg      visitor argument
         * @return result
         */
        default VisitResult visitTemplate(Template template, A arg) {
            return visitAny(template, arg);
        }

        /**
         * Visit a template block after traversing the nested nodes.
         *
         * @param template template
         * @param arg      visitor argument
         * @return result
         */
        default VisitResult postVisitTemplate(Template template, A arg) {
            return postVisitAny(template, arg);
        }

        /**
         * Visit any output.
         *
         * @param output output
         * @param arg    visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult visitAny(Output output, A arg) {
            return VisitResult.CONTINUE;
        }

        /**
         * Visit any output after traversing the nested nodes.
         *
         * @param output output
         * @param arg    visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult postVisitAny(Output output, A arg) {
            return VisitResult.CONTINUE;
        }
    }

    /**
     * Visit this output.
     *
     * @param visitor visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return result
     */
    public abstract <A> VisitResult accept(Visitor<A> visitor, A arg);

    /**
     * Visit this output after traversing the nested nodes.
     *
     * @param visitor visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return result
     */
    public <A> VisitResult acceptAfter(Visitor<A> visitor, A arg) {
        return VisitResult.CONTINUE;
    }

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitOutput(this, arg);
    }

    @Override
    public <A> VisitResult acceptAfter(Block.Visitor<A> visitor, A arg) {
        return visitor.postVisitOutput(this, arg);
    }

    /**
     * Path rule.
     */
    public static final class Transformation extends Output {

        private final String id;
        private final List<Replace> operations;

        private Transformation(Output.Builder builder) {
            super(builder);
            this.id = builder.attribute("id");
            this.operations = builder.operations;
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitTransformation(this, arg);
        }

        /**
         * Get the id.
         *
         * @return id
         */
        public String id() {
            return id;
        }

        /**
         * Get the operations.
         *
         * @return operations
         */
        public List<Replace> operations() {
            return operations;
        }

        /**
         * Replace operation.
         */
        public static final class Replace {

            private final String replacement;
            private final String regex;

            private Replace(String replacement, String regex) {
                this.replacement = replacement;
                this.regex = regex;
            }

            /**
             * Get the replacement.
             *
             * @return replacement
             */
            public String replacement() {
                return replacement;
            }

            /**
             * Get the regex.
             *
             * @return regex
             */
            public String regex() {
                return regex;
            }
        }
    }

    /**
     * Files.
     */
    public static class Files extends Output {

        private final List<String> transformations;
        private final String directory;
        private final List<String> includes;
        private final List<String> excludes;

        /**
         * Create a new files block.
         *
         * @param builder builder
         */
        Files(Output.Builder builder) {
            super(builder);
            String attr = builder.attributes().get("transformations");
            this.transformations = attr != null ? Arrays.stream(attr.split(","))
                                                        .map(String::trim)
                                                        .collect(toList()) : emptyList();
            this.directory = Objects.requireNonNull(builder.directory, "directory is null");
            this.includes = builder.includes;
            this.excludes = builder.excludes;
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitFiles(this, arg);
        }

        /**
         * Get the transformations.
         *
         * @return transformations
         */
        public List<String> transformations() {
            return transformations;
        }

        /**
         * Get the directory.
         *
         * @return directory
         */
        public String directory() {
            return directory;
        }

        /**
         * Get the includes.
         *
         * @return includes
         */
        public List<String> includes() {
            return includes;
        }

        /**
         * Get the excludes.
         *
         * @return excludes
         */
        public List<String> excludes() {
            return excludes;
        }
    }

    /**
     * Templates.
     */
    public static final class Templates extends Files {

        private final String engine;

        private Templates(Output.Builder builder) {
            super(builder);
            this.engine = builder.attribute("engine");
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitTemplates(this, arg);
        }

        /**
         * Get the template engine.
         *
         * @return engine
         */
        public String engine() {
            return engine;
        }
    }

    /**
     * File.
     */
    public static class File extends Output {

        private final String source;
        private final String target;

        /**
         * Create a new file block.
         *
         * @param builder builder
         */
        File(Output.Builder builder) {
            super(builder);
            this.source = builder.attribute("source");
            this.target = builder.attribute("target");
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitFile(this, arg);
        }

        /**
         * Get the source.
         *
         * @return source
         */
        public String source() {
            return source;
        }

        /**
         * Get the target.
         *
         * @return target
         */
        public String target() {
            return target;
        }
    }

    /**
     * Template.
     */
    public static final class Template extends File {

        private final String engine;

        /**
         * Create a new file block.
         *
         * @param builder builder
         */
        Template(Output.Builder builder) {
            super(builder);
            this.engine = builder.attribute("engine");
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitTemplate(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Output.Visitor<A> visitor, A arg) {
            return visitor.postVisitTemplate(this, arg);
        }

        /**
         * Get the engine.
         *
         * @return engine
         */
        public String engine() {
            return engine;
        }
    }

    /**
     * Create a new Output block builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @param kind       block kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Kind kind) {
        return new Builder(scriptPath, position, kind);
    }

    /**
     * Output block builder.
     */
    public static class Builder extends Block.Builder {

        private String directory;
        private final List<String> includes = new LinkedList<>();
        private final List<String> excludes = new LinkedList<>();
        private final List<Transformation.Replace> operations = new LinkedList<>();

        /**
         * Create a new output builder.
         *
         * @param scriptPath script path
         * @param position   position
         * @param kind       kind
         */
        Builder(Path scriptPath, Position position, Kind kind) {
            super(scriptPath, position, kind);
        }

        private boolean doRemove(Noop.Builder b) {
            switch (b.kind()) {
                case REPLACE:
                    operations.add(new Transformation.Replace(
                            b.attribute("replacement"),
                            b.attribute("regex")));
                    return true;
                case DIRECTORY:
                    directory = b.value();
                    return true;
                case EXCLUDE:
                    excludes.add(b.value());
                    return true;
                case INCLUDE:
                    includes.add(b.value());
                    return true;
                default:
                    return true;
            }
        }

        private boolean doRemove(Block.Builder b) {
            switch (b.kind()) {
                case EXCLUDES:
                case INCLUDES:
                    remove(b.children(), Noop.Builder.class, this::doRemove);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        protected Block doBuild() {
            remove(children(), Noop.Builder.class, this::doRemove);
            remove(children(), Block.Builder.class, this::doRemove);
            Kind kind = kind();
            switch (kind) {
                case TRANSFORMATION:
                    return new Transformation(this);
                case FILES:
                    return new Files(this);
                case TEMPLATES:
                    return new Templates(this);
                case FILE:
                    return new File(this);
                case TEMPLATE:
                    return new Template(this);
                default:
                    throw new IllegalArgumentException("Unknown output block: " + kind);
            }
        }
    }
}
