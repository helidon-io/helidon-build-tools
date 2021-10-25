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
 * Condition statement.
 */
public final class Condition extends Statement {

    private final Expression expression;
    private final Statement then;

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
     * Get the "then" statement.
     *
     * @return statement
     */
    public Statement then() {
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
     * Condition statement builder.
     */
    public static final class Builder extends Statement.Builder<Condition, Builder> {

        private Expression expression;
        private Statement.Builder<?, ?> then;

        private Builder(Path scriptPath, Position position) {
            super(scriptPath, position);
        }

        /**
         * Get the then statement.
         *
         * @return statement builder
         */
        Statement.Builder<?, ?> then() {
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
         * Set the {@code then} statement.
         *
         * @param then statement builder
         * @return this builder
         */
        public Builder then(Statement.Builder<?, ?> then) {
            this.then = then;
            return this;
        }

        @Override
        protected Condition doBuild() {
            return new Condition(this);
        }
    }
}
