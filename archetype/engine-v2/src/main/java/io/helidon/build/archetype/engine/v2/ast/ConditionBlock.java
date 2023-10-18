/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

/**
 * ConditionBlock.
 */
public final class ConditionBlock extends Block {
    private final Expression expression;

    private ConditionBlock(Builder builder) {
        super(builder);
        this.expression = builder.expression;
    }

    /**
     * Create a new Builder instance.
     *
     * @param info builder info
     * @param kind kind
     * @return  builder instance
     */
    public static Builder builder(BuilderInfo info, Kind kind) {
        return new Builder(info, kind);
    }

    /**
     * Get the expression.
     *
     * @return expression
     */
    public Expression expression() {
        return expression;
    }

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitConditionBlock(this, arg);
    }

    @Override
    public <A> VisitResult acceptAfter(Visitor<A> visitor, A arg) {
        return visitor.postVisitConditionBlock(this, arg);
    }

    /**
     * ConditionBlock builder.
     */
    public static final class Builder extends Block.Builder {
        private Expression expression;

        private Builder(BuilderInfo info, Kind kind) {
            super(info, kind);
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

        @Override
        protected ConditionBlock doBuild() {
            return new ConditionBlock(this);
        }
    }
}
