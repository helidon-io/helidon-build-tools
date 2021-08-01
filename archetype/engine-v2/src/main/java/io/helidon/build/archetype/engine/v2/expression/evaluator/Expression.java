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
public class Expression {

    private final AbstractSyntaxTree tree;
    private final Tokenizer tokenizer;
    private Token lastToken;
    private int countOpenParentheses = 0;

    /**
     * Create a new expression.
     *
     * @param rawExpr string that contains an expression.
     * @throws ParserException if a parsing error occurs.
     */
    public Expression(String rawExpr) throws ParserException {
        tokenizer = new Tokenizer();
        tokenizer.init(rawExpr);
        if (tokenizer.hasMoreTokens()) {
            lastToken = tokenizer.getNextToken();
        }
        this.tree = parse();
    }

    private Expression(Tokenizer tokenizer, Token lastToken, int countOpenParentheses) throws ParserException {
        this.tokenizer = tokenizer;
        this.lastToken = lastToken;
        this.countOpenParentheses = countOpenParentheses;
        this.tree = parse();
    }

    /**
     * Evaluate the current expression. If current expression contains variables {@link Expression#evaluate(java.util.Map)}
     * must be used.
     *
     * @return Object that represents the result of the evaluated expression (for example {@link Boolean} for the logical
     * expression).
     * @throws ParserException if a parsing error occurs.
     */
    public Object evaluate() throws ParserException {
        if (tree.isLiteral()) {
            return tree.asLiteral().getValue();
        }
        if (tree().isExpressionHandler()) {
            return tree.asExpressionHandler().evaluate().getValue();
        }
        return null;
    }

    /**
     * Initialize variables and evaluate the current expression.
     *
     * @param varInitializerMap Map that contains names of the variables as the keys and their values in the string
     *                          representation as the values.
     * @return Object that represents the result of the evaluated expression (for example {@link Boolean} for the logical
     * expression).
     * @throws ParserException if a parsing error occurs.
     */
    public Object evaluate(Map<String, String> varInitializerMap) throws ParserException {
        initializeVariables(varInitializerMap);
        return evaluate();
    }

    private void initializeVariables(Map<String, String> varInitializerMap) throws ParserException {
        List<AbstractSyntaxTree> literals = getLiterals(tree);
        for (AbstractSyntaxTree literal : literals) {
            if (literal.isVariable()) {
                String varValue = varInitializerMap.get(literal.asVariable().getName());
                if (varValue == null) {
                    throw new IllegalArgumentException(
                            String.format("Variable %s must be initialized", literal.asVariable().getName()));
                }
                Expression expression = new Expression(varValue);
                if (!expression.tree().isLiteral()) {
                    throw new IllegalArgumentException("varInitializerMap must not contain expressions");
                }
                literal.asVariable().setValue(expression.tree().asLiteral());
            }
        }
    }

    private List<AbstractSyntaxTree> getLiterals(AbstractSyntaxTree tree) {
        List<AbstractSyntaxTree> result = new ArrayList<>();
        if (tree.isExpressionHandler()) {
            if (tree.isBinaryLogicalExpression()) {
                fillLiteralList(result, tree.asBinaryLogicalExpression().getLeft());
                fillLiteralList(result, tree.asBinaryLogicalExpression().getRight());
            }
            if (tree.isUnaryLogicalExpression()) {
                fillLiteralList(result, tree.asUnaryLogicalExpression().getLeft());
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

    private AbstractSyntaxTree parse() throws ParserException {

        AbstractSyntaxTree initialAST = getInitialAST();

        while (tokenizer.hasMoreTokens()) {
            AbstractSyntaxTree nested = null;
            var token = tokenizer.getNextToken();

            if (token == null) {
                continue;
            }

            if (token.getType().isBinaryOperator()) {
                if (lastToken.getType().isBinaryOperator()) {
                    throw new ParserException("Unexpected token - " + token.getValue());
                }
                lastToken = token;
                continue;
            }

            if (token.getType().equals(Token.Type.PARENTHESIS) && token.getValue().equals("(")) {
                token = getLastTokenSafely(token);
                nested = generateAST(tokenizer, token, 1);
                markExpressionAsIsolated(nested);
            }

            if (token.getType().equals(Token.Type.PARENTHESIS) && token.getValue().equals(")")) {
                countOpenParentheses--;
                checkOpenParentheses();
                return initialAST;
            }

            if (token.getType().equals(Token.Type.UNARY_LOGICAL_OPERATOR)) {
                nested = generateAST(tokenizer, token, 0);
            }

            if (lastToken.getType().isBinaryOperator()) {
                initialAST = getBinaryExpression(initialAST, nested != null ? nested : ASTFactory.create(token));
                lastToken = token;
            }
        }

        checkOpenParentheses();
        return initialAST;

    }

    private Token getLastTokenSafely(Token tokenByDefault) throws ParserException {
        if (tokenizer.hasMoreTokens()) {
            return tokenizer.getNextToken();
        }
        return tokenByDefault;
    }

    private AbstractSyntaxTree getInitialAST() throws ParserException {
        AbstractSyntaxTree initialAST;

        if (lastToken.getType().equals(Token.Type.PARENTHESIS)
                && lastToken.getValue().equals("(")) {
            lastToken = getLastTokenSafely(lastToken);
            initialAST = generateAST(tokenizer, lastToken, 1);
            markExpressionAsIsolated(initialAST);
        } else if (lastToken.getType().equals(Token.Type.UNARY_LOGICAL_OPERATOR)) {
            LogicalOperator logicalOperator = LogicalOperator.find(lastToken.getValue());
            lastToken = getLastTokenSafely(lastToken);
            initialAST = new UnaryLogicalExpression(
                    logicalOperator,
                    generateAST(tokenizer, lastToken, 0)
            );
        } else {
            initialAST = ASTFactory.create(lastToken);
        }

        return initialAST;
    }

    private AbstractSyntaxTree generateAST(Tokenizer tokenizer, Token token, int countOpenParentheses) throws ParserException {
        Expression expression = new Expression(tokenizer, token, countOpenParentheses);
        return expression.tree();
    }

    private void checkOpenParentheses() throws ParserException {
        if (countOpenParentheses != 0) {
            throw new ParserException("Unmatched parenthesis found");
        }
    }

    private void markExpressionAsIsolated(AbstractSyntaxTree ast) {
        if (ast.isBinaryLogicalExpression()) {
            ast.asBinaryLogicalExpression().setIsolated(true);
        }
    }

    private AbstractSyntaxTree getBinaryExpression(
            AbstractSyntaxTree initial, AbstractSyntaxTree current
    ) throws ParserException {
        var logicalOperator = LogicalOperator.find(lastToken.getValue());
        AbstractSyntaxTree result = null;

        if (initial.isBinaryLogicalExpression() && !initial.asBinaryLogicalExpression().isIsolated()) {
            int leftPrecedence = initial.asBinaryLogicalExpression().getOperator().getPrecedence();
            int currentPrecedence = logicalOperator.getPrecedence();

            if (currentPrecedence > leftPrecedence) {
                //change existing left AST (create new expression and set it as value for the right operand)
                result = new BinaryLogicalExpression(
                        initial.asBinaryLogicalExpression().getLeft(),
                        new BinaryLogicalExpression(
                                initial.asBinaryLogicalExpression().getRight(),
                                current,
                                logicalOperator
                        ),
                        initial.asBinaryLogicalExpression().getOperator()
                );
            } else {
                //create new AST that will be the new var left
                // (old var left will be as value for the left operand in this new AST)
                result = new BinaryLogicalExpression(
                        initial,
                        current,
                        logicalOperator
                );
            }
        } else {
            result = new BinaryLogicalExpression(
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
}
