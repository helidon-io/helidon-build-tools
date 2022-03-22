/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
import java.util.List;

import io.helidon.build.archetype.engine.v2.ScriptLoader;

import static java.util.Collections.emptyList;

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
         * Visit a transformation block after traversing the nested nodes.
         *
         * @param transformation transformation
         * @param arg            visitor argument
         * @return result
         */
        default VisitResult postVisitTransformation(Transformation transformation, A arg) {
            return visitAny(transformation, arg);
        }

        /**
         * Visit a replacement block.
         *
         * @param replace replacement block
         * @param arg     visitor argument
         * @return result
         */
        default VisitResult visitReplace(Replace replace, A arg) {
            return visitAny(replace, arg);
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
         * Visit a files block after traversing the nested nodes.
         *
         * @param files files
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitFiles(Files files, A arg) {
            return visitAny(files, arg);
        }

        /**
         * Visit an include block.
         *
         * @param include include block
         * @param arg     visitor argument
         * @return result
         */
        default VisitResult visitInclude(Include include, A arg) {
            return visitAny(include, arg);
        }

        /**
         * Visit an exclude block.
         *
         * @param exclude exclude block
         * @param arg     visitor argument
         * @return result
         */
        default VisitResult visitExclude(Exclude exclude, A arg) {
            return visitAny(exclude, arg);
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
         * Visit a templates block after traversing the nested nodes.
         *
         * @param templates templates
         * @param arg       visitor argument
         * @return result
         */
        default VisitResult postVisitTemplates(Templates templates, A arg) {
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

        private Transformation(Output.Builder builder) {
            super(builder);
            this.id = builder.attribute("id", true).asString();
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitTransformation(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Output.Visitor<A> visitor, A arg) {
            return visitor.postVisitTransformation(this, arg);
        }

        /**
         * Get the id.
         *
         * @return id
         */
        public String id() {
            return id;
        }
    }

    /**
     * Replace operation.
     */
    public static final class Replace extends Output {

        private final String replacement;
        private final String regex;

        private Replace(Output.Builder builder) {
            super(builder);
            this.replacement = builder.attribute("replacement", true).asString();
            this.regex = builder.attribute("regex", true).asString();
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

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitReplace(this, arg);
        }
    }

    /**
     * Pattern.
     */
    public abstract static class Pattern extends Output {

        private final String value;

        private Pattern(Output.Builder builder) {
            super(builder);
            this.value = builder.value();
        }

        /**
         * Get the value.
         *
         * @return value
         */
        public String value() {
            return value;
        }
    }

    /**
     * Include block.
     */
    public static final class Include extends Pattern {

        private Include(Output.Builder builder) {
            super(builder);
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitInclude(this, arg);
        }
    }

    /**
     * Exclude block.
     */
    public static final class Exclude extends Pattern {

        private Exclude(Output.Builder builder) {
            super(builder);
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitExclude(this, arg);
        }
    }

    /**
     * Files.
     */
    public static class Files extends Output {

        private final List<String> transformations;
        private final String directory;

        /**
         * Create a new files block.
         *
         * @param builder builder
         */
        Files(Output.Builder builder) {
            super(builder);
            this.directory = builder.attribute("directory", true).asString();
            this.transformations = builder.attribute("transformations", ValueTypes.STRING_LIST, emptyList());
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitFiles(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Output.Visitor<A> visitor, A arg) {
            return visitor.postVisitFiles(this, arg);
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
    }

    /**
     * Templates.
     */
    public static final class Templates extends Files {

        private final String engine;

        private Templates(Output.Builder builder) {
            super(builder);
            this.engine = builder.attribute("engine", true).asString();
        }

        @Override
        public <A> VisitResult accept(Output.Visitor<A> visitor, A arg) {
            return visitor.visitTemplates(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Output.Visitor<A> visitor, A arg) {
            return visitor.postVisitTemplates(this, arg);
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
            this.source = builder.attribute("source", true).asString();
            this.target = builder.attribute("target", true).asString();
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
            this.engine = builder.attribute("engine", true).asString();
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
     * @param loader     script loader
     * @param scriptPath script path
     * @param position   position
     * @param kind       block kind
     * @return builder
     */
    public static Builder builder(ScriptLoader loader, Path scriptPath, Position position, Kind kind) {
        return new Builder(loader, scriptPath, position, kind);
    }

    /**
     * Output block builder.
     */
    public static class Builder extends Block.Builder {

        /**
         * Create a new output builder.
         *
         * @param loader     script loader
         * @param scriptPath script path
         * @param position   position
         * @param kind       kind
         */
        Builder(ScriptLoader loader, Path scriptPath, Position position, Kind kind) {
            super(loader, scriptPath, position, kind);
        }

        @Override
        protected Block doBuild() {
            Kind kind = kind();
            switch (kind) {
                case REPLACE:
                    return new Transformation.Replace(this);
                case TRANSFORMATION:
                    return new Transformation(this);
                case INCLUDE:
                    return new Include(this);
                case EXCLUDE:
                    return new Exclude(this);
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
