/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

import java.util.function.Function;

/**
 * Condition.
 */
public final class Condition extends Node {

    private final String rawExpression;
    private final Expression expression;
    private final Node then;

    private Condition(Builder builder) {
        super(builder);
        this.rawExpression = builder.rawExpression;
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
     * Get the raw expression.
     *
     * @return raw expression
     */
    public String rawExpression() {
        return rawExpression;
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
    public String toString() {
        return "Condition{"
                + "expression=" + rawExpression
                + ", then=" + then
                + '}';
    }

    @Override
    public <A> VisitResult accept(Visitor<A> visitor, A arg) {
        return visitor.visitCondition(this, arg);
    }

    /**
     * If the given node is an instance of {@link Condition}, evaluate the expression.
     *
     * @param node     node
     * @param resolver variable resolver
     * @return {@code true} if the node is not an instance of {@link Condition} or the expression result
     */
    public static boolean filter(Node node, Function<String, Value> resolver) {
        if (node instanceof Condition) {
            return ((Condition) node).expression().eval(resolver).asBoolean();
        }
        return true;
    }

    /**
     * If the given node is an instance of {@link Condition}, unwrap the nested node or return the input node.
     *
     * @param node node
     * @return Node
     */
    public static Node unwrap(Node node) {
        if (node instanceof Condition) {
            return ((Condition) node).then();
        }
        return node;
    }

    /**
     * Create a new builder.
     *
     * @param info builder info
     * @return builder
     */
    public static Builder builder(BuilderInfo info) {
        return new Builder(info);
    }

    /**
     * Condition builder.
     */
    public static final class Builder extends Node.Builder<Condition, Builder> {

        private String rawExpression;
        private Expression expression;
        private Node.Builder<? extends Node, ?> then;

        private Builder(BuilderInfo info) {
            super(info);
        }

        /**
         * Set the expression.
         *
         * @param expression expression
         * @return this builder
         */
        public Builder expression(String expression) {
            this.rawExpression = expression;
            this.expression = Expression.create(expression);
            return this;
        }

        /**
         * Set the expression.
         *
         * @param expression expression
         * @return this builder
         */
        public Builder expression(Expression expression) {
            this.expression = expression;
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
