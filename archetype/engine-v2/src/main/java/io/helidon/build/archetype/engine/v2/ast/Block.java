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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Block statement.
 */
public class Block extends Statement {

    private final Kind kind;
    private final List<Statement> statements;

    /**
     * Create a new block.
     *
     * @param builder builder
     */
    protected Block(Builder builder) {
        super(builder);
        this.kind = Objects.requireNonNull(builder.kind, "kind is null");
        this.statements = builder.statements.stream()
                                            .filter(b -> !(b instanceof Noop.Builder))
                                            .map(Statement.Builder::build)
                                            .collect(toUnmodifiableList());
    }

    /**
     * Create a new block.
     *
     * @param scriptPath script path
     * @param position   position
     * @param kind       kind
     * @param statements statements
     */
    protected Block(Path scriptPath, Position position, Kind kind, List<Statement> statements) {
        super(scriptPath, position);
        this.kind = Objects.requireNonNull(kind, "kind is null");
        this.statements = Objects.requireNonNull(statements, "statements is null");
    }

    /**
     * Get the nested statements.
     *
     * @return statements
     */
    public List<Statement> statements() {
        return statements;
    }

    /**
     * Wrap this block with a new kind.
     *
     * @param kind kind
     * @return block
     */
    public Block wrap(Block.Kind kind) {
        return new Block(scriptPath(), position(), kind, List.of(this));
    }

    /**
     * Block visitor.
     *
     * @param <A> argument type
     */
    public interface Visitor<A> {

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
         * Visit an input block after traversing the nested statements.
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
         * Visit a step block after traversing the nested statements.
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
         * Visit an output block after traversing the nested statements.
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
         * Visit a model block after traversing the nested statements.
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
         * Visit any block after traversing the nested statements.
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
     * Visit this block after traversing the nested statements.
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
    public Kind blockKind() {
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
         * Includes.
         */
        INCLUDES,

        /**
         * Excludes.
         */
        EXCLUDES,

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
     * @param scriptPath script path
     * @param position   position
     * @param kind       kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Kind kind) {
        return new Builder(scriptPath, position, kind);
    }

    /**
     * Block builder.
     */
    public static class Builder extends Statement.Builder<Block, Builder> {

        private final List<Statement.Builder<? extends Statement, ?>> statements = new LinkedList<>();
        private final Kind kind;

        /**
         * Create a new builder.
         *
         * @param scriptPath script path
         * @param position   position
         * @param kind       kind
         */
        protected Builder(Path scriptPath, Position position, Kind kind) {
            super(scriptPath, position);
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

        /**
         * Get the nested statements.
         *
         * @return statement builders
         */
        List<Statement.Builder<? extends Statement, ?>> statements() {
            return statements;
        }

        @Override
        public Builder statement(Statement.Builder<? extends Statement, ?> builder) {
            statements.add(builder);
            return this;
        }

        @Override
        protected Block doBuild() {
            if (kind == Kind.MODEL) {
                statements.replaceAll(b -> {
                    if (b instanceof Noop.Builder) {
                        return Model.builder(b.scriptPath(), b.position(), Kind.VALUE)
                                    .value(((Noop.Builder) b).value())
                                    .attributes(b.attributes());
                    }
                    return b;
                });
            }
            return new Block(this);
        }
    }
}
