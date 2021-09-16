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

package io.helidon.build.archetype.engine.v2.expression.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An expression supporting logical operators with text, boolean and array literals and variables. The logical operators have
 * the precedence. Parenthesis must be used to express a different order.
 * The expression is parsed into a syntax tree that can be accessed using {@link #tree()}.
 */
public final class Expression {

    private final String rawExpression;
    private final AbstractSyntaxTree tree;
    private final Tokenizer tokenizer;
    private Token lastToken;
    private int countOpenParentheses = 0;

    /**
     * Get string representation of the current {@code Expression} instance.
     *
     * @return expression
     */
    public String expression() {
        return rawExpression;
    }

    /**
     * Create a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new expression.
     *
     * @param rawExpr string that contains an expression.
     */
    private Expression(String rawExpr) {
        rawExpression = rawExpr;
        tokenizer = new Tokenizer();
        tokenizer.init(rawExpr);
        if (tokenizer.hasMoreTokens()) {
            lastToken = tokenizer.getNextToken();
        }
        this.tree = parse();
    }

    private Expression(Tokenizer tokenizer, Token lastToken, int countOpenParentheses, String rawExpression) {
        this.tokenizer = tokenizer;
        this.lastToken = lastToken;
        this.countOpenParentheses = countOpenParentheses;
        this.tree = parse();
        this.rawExpression = rawExpression;
    }

    /**
     * Evaluate the current expression. If current expression contains variables {@link Expression#evaluate(java.util.Map)}
     * must be used.
     *
     * @return the result of the evaluated expression.
     */
    public boolean evaluate() {
        if (tree.isLiteral()) {
            return (boolean) tree.asLiteral().value();
        }
        if (tree.isExpressionHandler()) {
            return (boolean) tree.asExpressionHandler().evaluate().value();
        }
        if (tree.isVariable()) {
            if (tree.asVariable().value() instanceof BooleanLiteral) {
                return (boolean) tree.asVariable().value().value();
            }
        }
        throw new UnexpectedResultException(String.format("Expression %s cannot be evaluated", rawExpression));
    }

    /**
     * Initialize variables and evaluate the current expression.
     *
     * @param varInitializerMap Map that contains names of the variables as the keys and their values in the string
     *                          representation as the values.
     * @return the result of the evaluated expression.
     */
    public boolean evaluate(Map<String, String> varInitializerMap) {
        initializeVariables(varInitializerMap);
        return evaluate();
    }

    private void initializeVariables(Map<String, String> varInitializerMap) {
        List<AbstractSyntaxTree> literals = getLiterals(tree);
        for (AbstractSyntaxTree literal : literals) {
            if (literal.isVariable()) {
                String varValue = varInitializerMap.get(literal.asVariable().name());
                if (varValue == null) {
                    throw new IllegalArgumentException(
                            String.format("Variable %s must be initialized", literal.asVariable().name()));
                }
                Expression expression = new Expression(varValue);
                if (!expression.tree().isLiteral()) {
                    throw new IllegalArgumentException("varInitializerMap must not contain expressions");
                }
                literal.asVariable().value(expression.tree().asLiteral());
            }
        }
    }

    private List<AbstractSyntaxTree> getLiterals(AbstractSyntaxTree tree) {
        List<AbstractSyntaxTree> result = new ArrayList<>();
        if (tree.isExpressionHandler()) {
            if (tree.isBinaryExpression()) {
                fillLiteralList(result, tree.asBinaryExpression().left());
                fillLiteralList(result, tree.asBinaryExpression().right());
            }
            if (tree.isUnaryExpression()) {
                fillLiteralList(result, tree.asUnaryExpression().left());
            }
        } else {
            result.add(tree);
        }
        return result;
    }

    private void fillLiteralList(List<AbstractSyntaxTree> literalList, AbstractSyntaxTree tree) {
        if (tree.isLiteral()) {
            literalList.add(tree);
        } else {
            literalList.addAll(getLiterals(tree));
        }
    }

    private AbstractSyntaxTree parse() {

        AbstractSyntaxTree initialAST = getInitialAST();

        while (tokenizer.hasMoreTokens()) {
            AbstractSyntaxTree nested = null;
            Token token = tokenizer.getNextToken();

            if (token == null) {
                continue;
            }

            if (token.type().binaryOperator()) {
                if (lastToken.type().binaryOperator()) {
                    throw new ParserException("Unexpected token - " + token.value());
                }
                lastToken = token;
                continue;
            }

            if (token.type().equals(Token.Type.PARENTHESIS) && token.value().equals("(")) {
                token = getLastTokenSafely(token);
                nested = generateAST(tokenizer, token, 1);
                markExpressionAsIsolated(nested);
            }

            if (token.type().equals(Token.Type.PARENTHESIS) && token.value().equals(")")) {
                countOpenParentheses--;
                checkOpenParentheses();
                return initialAST;
            }

            if (token.type().equals(Token.Type.UNARY_LOGICAL_OPERATOR)) {
                nested = generateAST(tokenizer, token, 0);
            }

            if (lastToken.type().binaryOperator()) {
                initialAST = getBinaryExpression(initialAST, nested != null ? nested : ASTFactory.create(token));
                lastToken = token;
            }
        }

        checkOpenParentheses();
        return initialAST;

    }

    private Token getLastTokenSafely(Token tokenByDefault) {
        if (tokenizer.hasMoreTokens()) {
            return tokenizer.getNextToken();
        }
        return tokenByDefault;
    }

    private AbstractSyntaxTree getInitialAST() {
        AbstractSyntaxTree initialAST;

        if (lastToken.type().equals(Token.Type.PARENTHESIS)
                && lastToken.value().equals("(")) {
            lastToken = getLastTokenSafely(lastToken);
            initialAST = generateAST(tokenizer, lastToken, 1);
            markExpressionAsIsolated(initialAST);
        } else if (lastToken.type().equals(Token.Type.UNARY_LOGICAL_OPERATOR)) {
            Operator operator = Operator.find(lastToken.value());
            lastToken = getLastTokenSafely(lastToken);
            initialAST = new UnaryExpression(
                    operator,
                    generateAST(tokenizer, lastToken, 0)
            );
        } else {
            initialAST = ASTFactory.create(lastToken);
        }

        return initialAST;
    }

    private AbstractSyntaxTree generateAST(Tokenizer tokenizer, Token token, int countOpenParentheses) {
        Expression expression = new Expression(tokenizer, token, countOpenParentheses, rawExpression);
        return expression.tree();
    }

    private void checkOpenParentheses() {
        if (countOpenParentheses != 0) {
            throw new ParserException("Unmatched parenthesis found");
        }
    }

    private void markExpressionAsIsolated(AbstractSyntaxTree ast) {
        if (ast.isBinaryExpression()) {
            ast.asBinaryExpression().isolated(true);
        }
    }

    private AbstractSyntaxTree getBinaryExpression(
            AbstractSyntaxTree initial, AbstractSyntaxTree current
    ) {
        Operator logicalOperator = Operator.find(lastToken.value());
        AbstractSyntaxTree result = null;

        if (initial.isBinaryExpression() && !initial.asBinaryExpression().isolated()) {
            int leftPrecedence = initial.asBinaryExpression().operator().priority();
            int currentPrecedence = logicalOperator.priority();

            if (currentPrecedence > leftPrecedence) {
                //change existing left AST (create new expression and set it as value for the right operand)
                result = new BinaryExpression(
                        initial.asBinaryExpression().left(),
                        new BinaryExpression(
                                initial.asBinaryExpression().right(),
                                current,
                                logicalOperator
                        ),
                        initial.asBinaryExpression().operator()
                );
            } else {
                //create new AST that will be the new var left
                // (old var left will be as value for the left operand in this new AST)
                result = new BinaryExpression(
                        initial,
                        current,
                        logicalOperator
                );
            }
        } else {
            result = new BinaryExpression(
                    initial,
                    current,
                    logicalOperator
            );
        }

        return result;
    }

    /**
     * Get the syntax tree.
     *
     * @return AbstractSyntaxTree
     */
    public AbstractSyntaxTree tree() {
        return tree;
    }

    /**
     * {@code Expression} builder static inner class.
     */
    public static final class Builder {
        private String expression;

        private Builder() {
        }

        /**
         * Sets the {@code expression} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param expression the {@code expression} to set
         * @return a reference to this Builder
         */
        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        /**
         * Returns a {@code Expression} built from the parameters previously set.
         *
         * @return a {@code Expression} built with parameters of this {@code Expression.Builder}
         */
        public Expression build() {
            return new Expression(expression);
        }
    }
}
