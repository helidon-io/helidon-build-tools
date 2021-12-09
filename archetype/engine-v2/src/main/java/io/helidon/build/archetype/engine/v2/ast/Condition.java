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

/**
 * Condition.
 */
public final class Condition extends Node {

    private final Expression expression;
    private final Node then;

    private Condition(Builder builder) {
        super(builder);
        this.expression = builder.expression;
        this.then = builder.then.doBuild();
    }

    /**
     * Get the expression.
     *
     * @return expression
     */
    public Expression expression() {
        return expression;
    }

    /**
     * Get the "then" node.
     *
     * @return node
     */
    public Node then() {
        return then;
    }

    @Override
    public <A> VisitResult accept(Visitor<A> visitor, A arg) {
        return visitor.visitCondition(this, arg);
    }

    /**
     * Create a new builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position) {
        return new Builder(scriptPath, position);
    }

    /**
     * Condition builder.
     */
    public static final class Builder extends Node.Builder<Condition, Builder> {

        private Expression expression;
        private Node.Builder<? extends Node, ?> then;

        private Builder(Path scriptPath, Position position) {
            super(scriptPath, position);
        }

        /**
         * Get the then node.
         *
         * @return node builder
         */
        Node.Builder<?, ?> then() {
            return then;
        }

        /**
         * Set the expression.
         *
         * @param expression expression
         * @return this builder
         */
        public Builder expression(String expression) {
            this.expression = Expression.create(expression);
            return this;
        }

        /**
         * Set the {@code then} node.
         *
         * @param then node builder
         * @return this builder
         */
        public Builder then(Node.Builder<? extends Node, ?> then) {
            this.then = then;
            return this;
        }

        @Override
        protected Condition doBuild() {
            return new Condition(this);
        }
    }
}
