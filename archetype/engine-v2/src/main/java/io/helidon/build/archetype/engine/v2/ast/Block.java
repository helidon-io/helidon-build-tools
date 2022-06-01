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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Block.
 */
public class Block extends Node {

    private final Kind kind;
    private final List<Node> children;

    /**
     * Create a new block.
     *
     * @param builder builder
     */
    protected Block(Builder builder) {
        super(builder);
        this.kind = Objects.requireNonNull(builder.kind, "kind is null");
        this.children = builder.children();
    }

    /**
     * Create a new block.
     *
     * @param info       builder info
     * @param attributes attributes map
     * @param kind       kind
     * @param children   children
     */
    protected Block(BuilderInfo info, Map<String, Value> attributes, Kind kind, List<Node> children) {
        super(info, attributes);
        this.kind = Objects.requireNonNull(kind, "kind is null");
        this.children = Objects.requireNonNull(children, "children is null");
    }

    /**
     * Get the nested nodes.
     *
     * @return nested nodes
     */
    public List<Node> children() {
        return children;
    }

    /**
     * Get the nested nodes stream.
     *
     * @param filter first filter applied
     * @return stream of nodes
     */
    public Stream<Node> children(Predicate<Node> filter) {
        return children.stream().filter(filter).map(Condition::unwrap);
    }

    /**
     * Get the nested nodes stream.
     *
     * @param filter first filter applied
     * @param <T>    node type
     * @return stream of nodes
     */
    public <T> Stream<T> children(Predicate<Node> filter, Class<T> clazz) {
        return children(filter).filter(clazz::isInstance).map(clazz::cast);
    }

    /**
     * Get the nested nodes stream.
     *
     * @param filter1 first filter applied
     * @param clazz   the type of nodes to include
     * @param filter2 final filter applied
     * @param <T>     node type
     * @return stream of nodes
     */
    public <T> Stream<T> children(Predicate<Node> filter1, Class<T> clazz, Predicate<T> filter2) {
        return children(filter1).filter(clazz::isInstance).map(clazz::cast).filter(filter2);
    }

    /**
     * Wrap this block with a new kind.
     *
     * @param kind kind
     * @return block
     */
    public Block wrap(Block.Kind kind) {
        return new Block(BuilderInfo.of(this), attributes(), kind, List.of(this));
    }

    @Override
    public String toString() {
        return "Block{"
                + "kind=" + kind
                + '}';
    }

    /**
     * Block visitor.
     *
     * @param <A> argument type
     */
    public interface Visitor<A> {

        /**
         * Visit a preset block.
         *
         * @param preset preset
         * @param arg    visitor argument
         * @return visit result
         */
        default VisitResult visitPreset(Preset preset, A arg) {
            return visitAny(preset, arg);
        }

        /**
         * Visit a variable block.
         *
         * @param variable variable
         * @param arg      visitor argument
         * @return visit result
         */
        default VisitResult visitVariable(Variable variable, A arg) {
            return visitAny(variable, arg);
        }

        /**
         * Visit an input block.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitInput(Input input, A arg) {
            return visitAny(input, arg);
        }

        /**
         * Visit an input block after traversing the nested nodes.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitInput(Input input, A arg) {
            return postVisitAny(input, arg);
        }

        /**
         * Visit a step block.
         *
         * @param step step
         * @param arg  visitor argument
         * @return result
         */
        default VisitResult visitStep(Step step, A arg) {
            return visitAny(step, arg);
        }

        /**
         * Visit a step block after traversing the nested nodes.
         *
         * @param step step
         * @param arg  visitor argument
         * @return result
         */
        default VisitResult postVisitStep(Step step, A arg) {
            return postVisitAny(step, arg);
        }

        /**
         * Visit an output block.
         *
         * @param output output
         * @param arg    visitor argument
         * @return result
         */
        default VisitResult visitOutput(Output output, A arg) {
            return visitAny(output, arg);
        }

        /**
         * Visit an output block after traversing the nested nodes.
         *
         * @param output output
         * @param arg    visitor argument
         * @return result
         */
        default VisitResult postVisitOutput(Output output, A arg) {
            return postVisitAny(output, arg);
        }

        /**
         * Visit a model block.
         *
         * @param model model
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitModel(Model model, A arg) {
            return visitAny(model, arg);
        }

        /**
         * Visit a model block after traversing the nested nodes.
         *
         * @param model model
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitModel(Model model, A arg) {
            return postVisitAny(model, arg);
        }

        /**
         * Visit any block.
         *
         * @param block block
         * @param arg   visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult visitAny(Block block, A arg) {
            return VisitResult.CONTINUE;
        }

        /**
         * Visit any block after traversing the nested nodes.
         *
         * @param block block
         * @param arg   visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult postVisitAny(Block block, A arg) {
            return VisitResult.CONTINUE;
        }
    }

    /**
     * Visit this block.
     *
     * @param visitor block visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return visit result
     */
    public <A> VisitResult accept(Visitor<A> visitor, A arg) {
        return visitor.visitAny(this, arg);
    }

    /**
     * Visit this block after traversing the nested nodes.
     *
     * @param visitor block visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return visit result
     */
    public <A> VisitResult acceptAfter(Visitor<A> visitor, A arg) {
        return visitor.postVisitAny(this, arg);
    }

    @Override
    public final <A> VisitResult accept(Node.Visitor<A> visitor, A arg) {
        return visitor.visitBlock(this, arg);
    }

    @Override
    public <A> VisitResult acceptAfter(Node.Visitor<A> visitor, A arg) {
        return visitor.postVisitBlock(this, arg);
    }

    /**
     * Get the block kind.
     *
     * @return kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Block kind.
     */
    public enum Kind {

        /**
         * Script.
         */
        SCRIPT,

        /**
         * Methods.
         */
        METHODS,

        /**
         * Method.
         */
        METHOD,

        /**
         * Step.
         */
        STEP,

        /**
         * Option.
         */
        OPTION,

        /**
         * Inputs.
         */
        INPUTS,

        /**
         * Text.
         */
        TEXT,

        /**
         * Boolean.
         */
        BOOLEAN,

        /**
         * Enum.
         */
        ENUM,

        /**
         * List.
         */
        LIST,

        /**
         * Presets.
         */
        PRESETS,

        /**
         * Variables.
         */
        VARIABLES,

        /**
         * Output.
         */
        OUTPUT,

        /**
         * Templates.
         */
        TEMPLATES,

        /**
         * Template.
         */
        TEMPLATE,

        /**
         * Files.
         */
        FILES,

        /**
         * File.
         */
        FILE,

        /**
         * Model.
         */
        MODEL,

        /**
         * Map.
         */
        MAP,

        /**
         * Value.
         */
        VALUE,

        /**
         * Transformation.
         */
        TRANSFORMATION,

        /**
         * REPLACE.
         */
        REPLACE,

        /**
         * Includes.
         */
        INCLUDES,

        /**
         * Include.
         */
        INCLUDE,

        /**
         * Excludes.
         */
        EXCLUDES,

        /**
         * Exclude.
         */
        EXCLUDE,

        /**
         * Invoke with directory.
         */
        INVOKE_DIR,

        /**
         * Invoke.
         */
        INVOKE,
    }

    /**
     * Create a new builder.
     *
     * @param info builder info
     * @param kind kind
     * @return builder
     */
    public static Builder builder(BuilderInfo info, Kind kind) {
        return new Builder(info, kind);
    }

    /**
     * Block builder.
     */
    public static class Builder extends Node.Builder<Block, Builder> {

        private final Kind kind;

        /**
         * Create a new builder.
         *
         * @param info builder info
         * @param kind kind
         */
        protected Builder(BuilderInfo info, Kind kind) {
            super(info);
            this.kind = kind;
        }

        /**
         * Get the block kind.
         *
         * @return kind
         */
        Kind kind() {
            return kind;
        }

        @Override
        protected Block doBuild() {
            return new Block(this);
        }
    }
}
