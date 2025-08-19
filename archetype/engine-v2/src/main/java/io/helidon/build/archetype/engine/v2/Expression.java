/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.helidon.build.common.BitSets;
import io.helidon.build.common.LazyValue;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;

import static java.util.Collections.unmodifiableList;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Logical expression.
 */
public final class Expression implements Comparable<Expression> {

    /**
     * True.
     */
    public static final Expression TRUE = new Expression(List.of(Token.TRUE), true);

    /**
     * False.
     */
    public static final Expression FALSE = new Expression(List.of(Token.FALSE), true);

    private static final Map<String, Expression> CACHE1 = new HashMap<>();
    private static final Map<List<Token>, Expression> CACHE2 = new HashMap<>();
    private static final Map<String, Operator> OPS = Arrays.stream(Operator.values())
            .flatMap(op -> Arrays.stream(op.symbols).map(s -> Map.entry(s, op)))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    static {
        CACHE1.put("true", TRUE);
        CACHE1.put("false", FALSE);
    }

    private final List<Token> tokens;
    private final LazyValue<String> literal = new LazyValue<>(this::print);
    private final LazyValue<Expression> reduced0 = new LazyValue<>(this::reduce0);
    private final LazyValue<Set<String>> variables = new LazyValue<>(this::variables0);
    private final boolean reduced;

    Expression(String expression) {
        this(parse(expression), false);
    }

    private Expression(List<Token> tokens, boolean reduced) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }
        this.tokens = unmodifiableList(tokens);
        this.reduced = reduced;
    }

    /**
     * Get or create an expression.
     *
     * @param expression expression
     * @return Expression
     */
    public static Expression create(String expression) {
        return CACHE1.computeIfAbsent(expression, Expression::new);
    }

    /**
     * Negate this expression.
     *
     * @return Expression
     */
    public Expression negate() {
        if (this == TRUE) {
            return FALSE;
        } else if (this == FALSE) {
            return TRUE;
        } else {
            return new Expression(Lists.addAll(tokens, Token.NOT), false);
        }
    }

    /**
     * Combine this expression and the given expression with the logical 'and' operator.
     *
     * @param expr expression
     * @return Expression
     */
    public Expression and(Expression expr) {
        if (expr == null || expr == TRUE || this == FALSE) {
            return this;
        } else if (this == TRUE) {
            return expr;
        } else {
            return new Expression(Lists.addAll(tokens, expr.tokens, Token.AND), false);
        }
    }

    /**
     * Combine this expression and the given expression with the logical 'or' operator.
     *
     * @param expr expression
     * @return Expression
     */
    public Expression or(Expression expr) {
        if (expr == null || expr == FALSE || this == TRUE) {
            return this;
        } else if (this == FALSE) {
            return expr;
        } else {
            return new Expression(Lists.addAll(tokens, expr.tokens, Token.OR), false);
        }
    }

    /**
     * Get the expression tokens.
     *
     * @return list of tokens
     */
    public List<Token> tokens() {
        return tokens;
    }

    /**
     * Get the variable names in this expression.
     *
     * @return variable names
     */
    public Set<String> variables() {
        return variables.get();
    }

    /**
     * Evaluate this expression.
     *
     * @return result
     */
    public boolean eval() {
        return eval(s -> null);
    }

    /**
     * Evaluate this expression.
     *
     * @param resolver variable resolver
     * @return result
     * @throws UnresolvedVariableException if {@code resolver} returns {@code null}
     */
    public boolean eval(Function<String, Value<?>> resolver) {
        Deque<Value<?>> stack = new ArrayDeque<>();
        for (Token token : tokens) {
            Value<?> value;
            if (token.operator != null) {
                Value<?> op1 = stack.pop();
                switch (token.operator) {
                    case NOT:
                        value = Value.of(!op1.getBoolean());
                        break;
                    case SIZEOF:
                        if (op1.type() == Value.Type.LIST) {
                            value = Value.of(op1.getList().size());
                        } else {
                            value = Value.of(op1.getString().length());
                        }
                        break;
                    case AS_INT:
                        value = Value.of(op1.getInt());
                        break;
                    case AS_LIST:
                        value = Value.of(op1.getList());
                        break;
                    case AS_STRING:
                        value = Value.of(op1.getString());
                        break;
                    default:
                        Value<?> op2 = stack.pop();
                        switch (token.operator) {
                            case OR:
                                value = Value.of(op2.asBoolean().orElse(false) || op1.asBoolean().orElse(false));
                                break;
                            case AND:
                                value = Value.of(op2.asBoolean().orElse(false) && op1.asBoolean().orElse(false));
                                break;
                            case EQUAL:
                                value = Value.of(Value.isEqual(op2, op1));
                                break;
                            case NOT_EQUAL:
                                value = Value.of(!Value.isEqual(op2, op1));
                                break;
                            case GREATER_THAN:
                                value = Value.of(op2.getInt() > op1.getInt());
                                break;
                            case GREATER_OR_EQUAL:
                                value = Value.of(op2.getInt() >= op1.getInt());
                                break;
                            case LOWER_THAN:
                                value = Value.of(op2.getInt() < op1.getInt());
                                break;
                            case LOWER_OR_EQUAL:
                                value = Value.of(op2.getInt() <= op1.getInt());
                                break;
                            case CONTAINS:
                                if (op1.type() == Value.Type.LIST) {
                                    value = Value.of(new HashSet<>(op2.getList()).containsAll(op1.getList()));
                                } else if (op2.type() == Value.Type.LIST) {
                                    value = Value.of(op2.getList().contains(op1.asString().orElse(null)));
                                } else {
                                    value = Value.of(op1.isPresent()
                                                     && op2.asString().orElse("").contains(op1.getString()));
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unsupported operator: " + token.operator);
                        }
                }
            } else if (token.operand != null) {
                value = token.operand;
            } else if (token.variable != null) {
                value = resolver.apply(token.variable);
                if (value == null) {
                    throw new UnresolvedVariableException(token.variable);
                }
            } else {
                throw new IllegalStateException("Invalid token");
            }
            stack.push(value);
        }
        return stack.pop().asBoolean().get();
    }

    /**
     * Reduce the expression.
     *
     * @return Expression
     */
    public Expression reduce() {
        return reduced ? this : reduced0.get();
    }

    /**
     * Inline variables.
     *
     * @param resolver resolver
     * @return Expression
     */
    public Expression inline(Function<String, Value<?>> resolver) {
        List<Token> inlined = new ArrayList<>();
        for (Token token : tokens) {
            if (token.variable != null) {
                Value<?> value = resolver.apply(token.variable);
                if (value != null && value.isPresent()) {
                    inlined.add(Token.of(value));
                    continue;
                }
            }
            inlined.add(token);
        }
        return new Expression(inlined, false).reduce();
    }

    /**
     * Relativize the given expression against this expression.
     *
     * @param expr expression
     * @return Expression
     */
    public Expression relativize(Expression expr) {
        return and(expr).reduce().sub(this);
    }

    /**
     * Substitute an expression from this expression.
     *
     * @param expr expression
     * @return sub expression
     */
    public Expression sub(Expression expr) {
        Map<String, List<Token>> v1 = new TreeMap<>();
        Expression e1 = synthetic(v1);
        if (v1.isEmpty()) {
            return e1.eval() ? TRUE : FALSE;
        }

        Map<String, List<Token>> v2 = new TreeMap<>();
        Expression e2 = expr.synthetic(v2);
        if (v2.isEmpty()) {
            return this;
        }

        // shared variables
        Map<String, List<Token>> sharedVars = new LinkedHashMap<>();
        sharedVars.putAll(Maps.filterKey(v1, v2::containsKey));
        sharedVars.putAll(Maps.filterKey(v2, v1::containsKey));

        if (sharedVars.isEmpty()) {
            // not intersecting
            return this;
        }

        // shared variables first
        Map<String, List<Token>> vars = new LinkedHashMap<>(sharedVars);
        vars.putAll(v1);
        vars.putAll(v2);

        // evaluate the truth tables
        BitSet m2 = e2.minterms(vars.keySet());
        BitSet m1 = e1.minterms(vars.keySet());

        if (!m1.intersects(m2)) {
            // not intersecting
            return this;
        }

        if (m1.equals(m2)) {
            // always true
            return TRUE;
        }

        int tableSize = 1 << vars.size();
        boolean flip = false;
        if (m1.cardinality() > tableSize >> 1) {
            m1.flip(0, tableSize);
            m2.flip(0, tableSize);
            flip = true;
        }

        BitSet minterms = BitSets.copyOf(m1);
        BitSet shared = BitSets.and(BitSets.copyOf(m1), m2);

        int offset = vars.size() - sharedVars.size();
        int mask = -1 << offset;
        int suffixes = 1 << offset; // number of suffix variations
        int prefixes = 1 << sharedVars.size(); // number of prefix variations
        for (int i = shared.nextSetBit(0); i >= 0 && i < tableSize; i = shared.nextSetBit(i + 1)) {
            // test all suffixes of the prefix
            int prefix = i & mask;
            boolean stable = true;
            for (int y = 0; y < suffixes; y++) {
                if (!m2.get(prefix | y)) {
                    stable = false;
                    break;
                }
            }
            if (stable) {
                // set all prefixes of the suffix
                int suffix = i & ~mask;
                for (int y = 0; y < prefixes; y++) {
                    minterms.set((y << offset) | suffix);
                }
            }
        }

        if (flip) {
            minterms.flip(0, tableSize);
        }

        if (minterms.isEmpty() || minterms.cardinality() == tableSize) {
            // always true
            return TRUE;
        }
        return reduce(vars, minterms);
    }

    /**
     * Get the expression literal.
     *
     * @return expression literal
     */
    public String literal() {
        return literal.get();
    }

    @Override
    public String toString() {
        return "Expression{"
               + "tokens=" + tokens
               + '}';
    }

    @Override
    public int compareTo(Expression o) {
        return Lists.compare(tokens, o.tokens);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Expression)) {
            return false;
        }
        Expression other = (Expression) o;
        return tokens.equals(other.tokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokens);
    }

    // QMC algorithm
    static int[][] reduce(int... minterms) {
        TermTable table = new TermTable();

        // sort by bit count
        BitSet positions = new BitSet(); // bitmap of sorted minterms
        int numVars = 32 - Integer.numberOfLeadingZeros(Math.max(minterms[minterms.length - 1], 1));
        for (int x = 0, y = 0; x <= numVars && y < minterms.length; x++) {
            int z = y;
            for (int i = positions.nextClearBit(0); i < minterms.length; i = positions.nextClearBit(i + 1)) {
                int term = minterms[i];
                if (x == Integer.bitCount(term)) {
                    table.terms.add(new Term(term, 0, BitSets.of(i)));
                    positions.set(i);
                    y++;
                    if (x == 0) {
                        break;
                    }
                }
            }
            if (y > z) {
                table.groups.add(y);
            }
        }

        // find implicants
        TermTable tmp = new TermTable(); // write-table
        while (true) {
            tmp.clear();
            BitSet bitmap = new BitSet();
            for (int g = 0, i1 = 0; g < table.groups.size(); g++) { // for-each group
                boolean addGroup = false;
                for (int l1 = table.groups.get(g); i1 < l1; i1++) {
                    Term term1 = table.terms.get(i1);
                    boolean merged = false;
                    if (g + 1 < table.groups.size()) { // compare nth and nth+1
                        for (int i2 = l1, l2 = table.groups.get(g + 1); i2 < l2; i2++) {
                            Term term2 = table.terms.get(i2);
                            if (term1.mark == term2.mark) {
                                int delta = term1.bits ^ term2.bits;
                                if (Integer.bitCount(delta) == 1) {
                                    BitSet ids = BitSets.or(BitSets.copyOf(term1.ids), term2.ids); // merge the ids
                                    if (!tmp.contains(ids)) { // add once
                                        int mark = delta | term1.mark | term2.mark; // merge the marks
                                        int bits = (term1.bits | term2.bits) & ~mark; // merge the bits (exclude the marks)
                                        tmp.terms.add(new Term(bits, mark, ids));
                                        bitmap.or(ids);
                                        merged = true;
                                    }
                                }
                            }
                        }
                    }
                    if (!merged && !BitSets.containsAll(term1.ids, bitmap)) {
                        // not mergeable and not covered by any other term
                        tmp.terms.add(term1);
                        bitmap.or(term1.ids);
                        if (!tmp.groups.isEmpty()) {
                            tmp.groups.set(tmp.groups.size() - 1, tmp.terms.size());
                        }
                    }
                    addGroup |= merged;
                }
                if (addGroup) {
                    tmp.groups.add(tmp.terms.size());
                }
            }
            if (!tmp.terms.isEmpty()) {
                // swap tables
                TermTable next = tmp;
                tmp = table;
                table = next;
            } else {
                break;
            }
        }

        // prime chart
        BitSet essentials = new BitSet();
        BitSet duplicates = new BitSet();
        for (Term term : table.terms) {
            if (term.ids.intersects(essentials)) {
                duplicates.or(BitSets.and(BitSets.copyOf(term.ids), essentials)); // record used terms
                essentials.andNot(duplicates); // remove duplicates from essentials
                essentials.or(BitSets.andNot(BitSets.copyOf(term.ids), duplicates)); // record unused terms
            } else {
                essentials.or(term.ids);
            }
        }

        // compute the result as an array of [bits][marks]
        List<int[]> result = new ArrayList<>(table.terms.size());
        for (Term term : table.terms) {
            if (term.ids.intersects(essentials)) {
                result.add(new int[] {
                        term.bits,
                        term.mark
                });
            }
        }
        return result.toArray(new int[0][]);
    }

    private static Expression reduce(Map<String, List<Token>> vars, BitSet minterms) {
        List<Token> tokens = new ArrayList<>();
        int[][] terms = reduce(minterms.stream().toArray());
        for (int i = 0; i < terms.length; i++) {
            // associate bits with variables from high to low
            int bit = 1 << vars.size() - 1;
            int y = 0;
            for (List<Token> v : vars.values()) {
                if ((bit & terms[i][1]) == 0) {
                    // bit is not marked
                    tokens.addAll(v);
                    if ((bit & terms[i][0]) == 0) {
                        int index = tokens.size() - 1;
                        Token last = tokens.get(index);
                        if (last == Token.EQUAL) {
                            // replace NOT + EQUAL with NOT_EQUAL
                            tokens.set(index, Token.NOT_EQUAL);
                        } else if (last == Token.NOT_EQUAL) {
                            // replace NOT + NOT_EQUAL with EQUAL
                            tokens.set(index, Token.EQUAL);
                        } else {
                            tokens.add(Token.NOT);
                        }
                    }
                    if (y++ > 0) {
                        tokens.add(Token.AND);
                    }
                }
                bit >>>= 1;
            }
            if (i > 0) {
                tokens.add(Token.OR);
            }
        }
        return new Expression(tokens, true);
    }

    /**
     * Unresolved variable error.
     */
    public static final class UnresolvedVariableException extends RuntimeException {
        private final String variable;

        private UnresolvedVariableException(String variable) {
            super("Unresolved variable: " + variable);
            this.variable = variable;
        }

        /**
         * Get the unresolved variable name.
         *
         * @return variable name
         */
        public String variable() {
            return variable;
        }
    }

    /**
     * Expression formatting error.
     */
    public static final class FormatException extends RuntimeException {

        private FormatException(String message) {
            super(message);
        }
    }

    private String print() {
        Deque<String> stack = new ArrayDeque<>();
        Deque<Integer> ops = new ArrayDeque<>();
        for (Token token : tokens) {
            if (token.operator != null) {
                String op1 = stack.pop();
                int pr1 = ops.pop();
                if (token.operator.precedence >= pr1) {
                    op1 = "(" + op1 + ")";
                }
                if (token.operator.valence == 1) {
                    stack.push(token + op1);
                } else {
                    String op2 = stack.pop();
                    int pr2 = ops.pop();
                    if (token.operator.precedence > pr2) {
                        op2 = "(" + op2 + ")";
                    }
                    stack.push(op2 + " " + token + " " + op1);
                }
                ops.push(token.operator.precedence);
            } else {
                stack.push(token.toString());
                ops.push(Integer.MAX_VALUE);
            }
        }
        return stack.peek();
    }

    private Set<String> variables0() {
        Set<String> variables = new HashSet<>();
        for (Token token : tokens) {
            if (token.isVariable()) {
                variables.add(token.variable);
            }
        }
        return variables;
    }

    private Expression reduce0() {
        if (this == TRUE) {
            return TRUE;
        } else if (this == FALSE) {
            return FALSE;
        } else {
            return CACHE2.computeIfAbsent(tokens, l -> reduce1());
        }
    }

    private Expression reduce1() {
        Map<String, List<Token>> vars = new TreeMap<>();

        // ensure a boolean-only expression and record variables
        Expression expr = synthetic(vars);

        // constant
        int numVars = vars.size();
        if (numVars == 0) {
            return expr.eval() ? TRUE : FALSE;
        }

        // evaluate the truth table
        BitSet minterms = expr.minterms(vars.keySet());

        // always false
        if (minterms.isEmpty()) {
            return FALSE;
        }

        // always true
        if (minterms.cardinality() == (1 << numVars)) {
            return TRUE;
        }

        // QMC resolution
        return reduce(vars, minterms);
    }

    private List<Token> expandVar(String varName, Map<String, List<Token>> vars) {
        List<Token> value = vars.get(varName);
        if (value == null) {
            return List.of(Token.of(varName));
        } else {
            List<Token> tokens = new ArrayList<>();
            for (Token token : value) {
                if (token.variable != null) {
                    tokens.addAll(expandVar(token.variable, vars));
                } else {
                    tokens.add(token);
                }
            }
            return tokens;
        }
    }

    private Expression synthetic(Map<String, List<Token>> vars) {
        // substitute non-logical operations that use variables with synthetic variables
        // to produce an expression containing only logical operations
        Map<String, List<Token>> tempVars = new TreeMap<>();
        Deque<List<Token>> stack = new ArrayDeque<>();
        for (Token token : tokens) {
            if (token.operator != null) {
                List<Token> op1 = stack.pop();
                Token t1 = op1.get(0);
                String s1 = t1.variable != null ? t1.variable : t1.toString();
                String varName;
                switch (token.operator) {
                    case NOT:
                        stack.push(Lists.addAll(op1, token));
                        break;
                    case SIZEOF:
                    case AS_INT:
                    case AS_LIST:
                    case AS_STRING:
                        // t1 is always a variable (enforced in parse)
                        varName = token.toString() + ' ' + s1;
                        tempVars.putIfAbsent(varName, Lists.addAll(vars.getOrDefault(s1, op1), token));
                        stack.push(List.of(Token.of(varName)));
                        break;
                    case CONTAINS:
                    case EQUAL:
                    case NOT_EQUAL:
                    case GREATER_THAN:
                    case GREATER_OR_EQUAL:
                    case LOWER_THAN:
                    case LOWER_OR_EQUAL:
                        List<Token> op2 = stack.pop();
                        Token t2 = op2.get(0);
                        if (t1 == Token.TRUE && t2.variable != null) {
                            stack.push(List.of(t2));
                        } else if (t1 == Token.FALSE && t2.variable != null) {
                            stack.push(List.of(t2, Token.NOT));
                        } else if (t2 == Token.TRUE && t1.variable != null) {
                            stack.push(List.of(t1));
                        } else if (t2 == Token.FALSE && t1.variable != null) {
                            stack.push(List.of(t1, Token.NOT));
                        } else if (t1.variable != null || t2.variable != null) {
                            String s2 = t2.variable != null ? t2.variable : t2.toString();
                            op2 = tempVars.getOrDefault(s2, op2);
                            op1 = tempVars.getOrDefault(s1, op1);
                            List<Token> next = new ArrayList<>();
                            if (token == Token.NOT_EQUAL) {
                                // normalize NOT_EQUAL into NOT + EQUAL
                                // to avoid creating different variables
                                token = Token.EQUAL;
                                next.add(Token.NOT);
                            }
                            varName = s2 + ' ' + token + ' ' + s1;
                            tempVars.putIfAbsent(varName, Lists.addAll(op2, op1, token));
                            stack.push(Lists.addAll(next, 0, Token.of(varName)));
                        } else {
                            stack.push(Lists.addAll(op2, op1, token));
                        }
                        break;
                    default:
                        stack.push(Lists.addAll(stack.pop(), op1, token));
                }
            } else {
                stack.push(List.of(token));
            }
        }

        List<Token> expr = new ArrayList<>();
        while (!stack.isEmpty()) {
            for (Token token : stack.pop()) {
                if (token.variable != null) {
                    vars.putIfAbsent(token.variable, expandVar(token.variable, tempVars));
                }
                expr.add(token);
            }
        }
        return new Expression(expr, false);
    }

    private BitSet minterms(Set<String> names) {
        BitSet minterms = new BitSet();

        // evaluate the truth table
        int tableSize = 1 << names.size(); // 2 ^ length
        for (int y = 0; y < tableSize; y++) {

            // associate bits with variables from high to low
            int x = 1 << names.size() - 1;
            Map<String, Value<Boolean>> vars = new HashMap<>();
            for (String varName : names) {
                vars.put(varName, Value.of((y & x) != 0));
                x >>>= 1;
            }

            // record only when successful
            if (eval(vars::get)) {
                minterms.set(y);
            }
        }
        return minterms;
    }

    private static List<Token> parse(String expression) {
        // raw infix tokens
        Spliterator<Symbol> spliterator = spliteratorUnknownSize(new Tokenizer(expression), ORDERED);
        List<Symbol> symbols = StreamSupport.stream(spliterator, false).collect(toList());

        // used for validation
        int stackSize = 0;

        List<Token> tokens = new ArrayList<>();
        Stack<Symbol> stack = new Stack<>();

        // shunting yard, convert infix to rpn
        ListIterator<Symbol> it = symbols.listIterator();
        while (it.hasNext()) {
            int previous = it.previousIndex();
            Symbol symbol = it.next();
            switch (symbol.type) {
                case BINARY_OPERATOR:
                case UNARY_OPERATOR:
                    if (symbol.type != Symbol.Type.UNARY_OPERATOR
                        && previous >= 0 && symbols.get(previous).value.equals("(")) {
                        throw new FormatException("Invalid parenthesis");
                    }
                    while (!stack.isEmpty() && OPS.containsKey(stack.peek().value)) {
                        Operator currentOp = OPS.get(symbol.value);
                        Operator leftOp = OPS.get(stack.peek().value);
                        if ((leftOp.precedence >= currentOp.precedence)) {
                            stackSize += 1 - addToken(stack.pop(), tokens);
                            continue;
                        }
                        break;
                    }
                    stack.push(symbol);
                    break;
                case PARENTHESIS:
                    if ("(".equals(symbol.value)) {
                        stack.push(symbol);
                    } else if (")".equals(symbol.value)) {
                        while (!stack.isEmpty() && !stack.peek().value.equals("(")) {
                            stackSize += 1 - addToken(stack.pop(), tokens);
                        }
                        if (stack.isEmpty()) {
                            throw new FormatException("Unmatched parenthesis");
                        }
                        stack.pop();
                    } else {
                        throw new IllegalStateException("Unexpected symbol: " + symbol.value);
                    }
                    break;
                case BOOLEAN:
                case STRING:
                case ARRAY:
                case INT:
                case VARIABLE:
                    stackSize += 1 - addToken(symbol, tokens);
                    break;
                case SKIP:
                case COMMENT:
                    break;
                default:
                    throw new IllegalStateException("Unexpected symbol: " + symbol.value);
            }
        }
        while (!stack.isEmpty()) {
            stackSize += 1 - addToken(stack.pop(), tokens);
        }
        if (stackSize != 1) {
            throw new FormatException(String.format("Invalid expression: '%s'", expression));
        }
        return tokens;
    }

    private static int addToken(Symbol symbol, List<Token> tokens) {
        Token token = Token.of(symbol);
        int valence = 0;
        if (token.operator != null) {
            if (tokens.size() < token.operator.valence) {
                throw new FormatException("Missing operand(s)");
            }
            valence = token.operator.valence;
            Token op1 = tokens.get(tokens.size() - 1);
            switch (token.operator) {
                case NOT:
                    if (op1.operand != null && !(op1.operand.type() == Value.Type.BOOLEAN)) {
                        throw new FormatException("Invalid operand");
                    }
                    break;
                case AS_LIST:
                case AS_INT:
                case AS_STRING:
                    if (op1.variable == null) {
                        throw new FormatException("Invalid operand");
                    }
                    break;
                default:
            }
        }
        tokens.add(token);
        return valence;
    }

    /**
     * Expression operator.
     */
    public enum Operator {
        /**
         * Equal operator.
         */
        EQUAL(8, 2, "=="),

        /**
         * Not equal operator.
         */
        NOT_EQUAL(8, 2, "!="),

        /**
         * And operator.
         */
        AND(4, 2, "&&", "AND"),

        /**
         * Or operator.
         */
        OR(3, 2, "||", "OR"),

        /**
         * Greater than operator.
         */
        GREATER_THAN(10, 2, ">"),

        /**
         * Greater or equal operator.
         */
        GREATER_OR_EQUAL(10, 2, ">="),

        /**
         * Lower than operator.
         */
        LOWER_THAN(10, 2, "<"),

        /**
         * Lower or equal operator.
         */
        LOWER_OR_EQUAL(10, 2, "<="),

        /**
         * Contains operator.
         */
        CONTAINS(9, 2, "contains"),

        /**
         * As-list operator.
         */
        AS_LIST(14, 1, "(list)"),

        /**
         * As-string operator.
         */
        AS_STRING(14, 1, "(string)"),

        /**
         * As-int operator.
         */
        AS_INT(14, 1, "(int)"),

        /**
         * Sizeof operator.
         */
        SIZEOF(14, 1, "sizeof"),

        /**
         * Not operator.
         */
        NOT(13, 1, "!", "NOT");

        private final int precedence;
        private final int valence;
        private final String[] symbols;

        Operator(int precedence, int valence, String... symbols) {
            this.precedence = precedence;
            this.valence = valence;
            this.symbols = symbols;
        }

        /**
         * Get the operator symbol.
         *
         * @return symbol
         */
        public String symbol() {
            return symbols[0];
        }
    }

    /**
     * Expression token.
     */
    public static final class Token implements Comparable<Token> {

        private static final Pattern ARRAY_PATTERN = Pattern.compile("(?<element>'[^']*')((\\s*,\\s*)|(\\s*]))");
        private static final Pattern VAR_PATTERN = Pattern.compile("^\\$\\{(?<varName>~?[\\w.-]+)}");
        private static final Token TRUE = new Token(null, null, Value.TRUE);
        private static final Token FALSE = new Token(null, null, Value.FALSE);
        private static final Token AND = new Token(Operator.AND, null, null);
        private static final Token OR = new Token(Operator.OR, null, null);
        private static final Token NOT = new Token(Operator.NOT, null, null);
        private static final Token GREATER_THAN = new Token(Operator.GREATER_THAN, null, null);
        private static final Token GREATER_OR_EQUAL = new Token(Operator.GREATER_OR_EQUAL, null, null);
        private static final Token LOWER_THAN = new Token(Operator.LOWER_THAN, null, null);
        private static final Token LOWER_OR_EQUAL = new Token(Operator.LOWER_OR_EQUAL, null, null);
        private static final Token EQUAL = new Token(Operator.EQUAL, null, null);
        private static final Token NOT_EQUAL = new Token(Operator.NOT_EQUAL, null, null);
        private static final Token CONTAINS = new Token(Operator.CONTAINS, null, null);
        private static final Token SIZEOF = new Token(Operator.SIZEOF, null, null);
        private static final Token AS_STRING = new Token(Operator.AS_STRING, null, null);
        private static final Token AS_INT = new Token(Operator.AS_INT, null, null);
        private static final Token AS_LIST = new Token(Operator.AS_LIST, null, null);

        private final Operator operator;
        private final String variable;
        private final Value<?> operand;

        private Token(Operator operator, String variable, Value<?> operand) {
            this.operator = operator;
            this.variable = variable;
            this.operand = operand;
        }

        /**
         * Indicate if this token represents an operator.
         *
         * @return {@code true} if an operator, {@code false} otherwise
         */
        public boolean isOperator() {
            return operator != null;
        }

        /**
         * Indicate if this token represents an operand.
         *
         * @return {@code true} if an operand, {@code false} otherwise
         */
        public boolean isOperand() {
            return operand != null;
        }

        /**
         * Indicate if this token represents a variable.
         *
         * @return {@code true} if a variable, {@code false} otherwise
         */
        public boolean isVariable() {
            return variable != null;
        }

        /**
         * Get the operator.
         *
         * @return Operator
         */
        public Operator operator() {
            if (operator == null) {
                throw new IllegalStateException("Token is not an operator: " + this);
            }
            return operator;
        }

        /**
         * Get the operand.
         *
         * @return Value
         */
        public Value<?> operand() {
            if (operand == null) {
                throw new IllegalStateException("Token is not an operand: " + this);
            }
            return operand;
        }

        /**
         * Get the variable.
         *
         * @return String
         */
        public String variable() {
            if (variable == null) {
                throw new IllegalStateException("Token is not a variable: " + this);
            }
            return variable;
        }

        /**
         * Create a new operator token.
         *
         * @param operator operator
         * @return Token
         */
        public static Token of(Operator operator) {
            switch (operator) {
                case EQUAL:
                    return EQUAL;
                case NOT_EQUAL:
                    return NOT_EQUAL;
                case AND:
                    return AND;
                case OR:
                    return OR;
                case NOT:
                    return NOT;
                case GREATER_THAN:
                    return GREATER_THAN;
                case GREATER_OR_EQUAL:
                    return GREATER_OR_EQUAL;
                case LOWER_THAN:
                    return LOWER_THAN;
                case LOWER_OR_EQUAL:
                    return LOWER_OR_EQUAL;
                case CONTAINS:
                    return CONTAINS;
                case SIZEOF:
                    return SIZEOF;
                case AS_STRING:
                    return AS_STRING;
                case AS_INT:
                    return AS_INT;
                case AS_LIST:
                    return AS_LIST;
                default:
                    throw new IllegalStateException("Unexpected operator: " + operator);
            }
        }

        /**
         * Create a new variable token.
         *
         * @param variable variable
         * @return Token
         */
        public static Token of(String variable) {
            return new Token(null, variable, null);
        }

        /**
         * Create a new operand token.
         *
         * @param value value
         * @return Token
         */
        public static Token of(Value<?> value) {
            return new Token(null, null, value);
        }

        /**
         * Create a new boolean operand token.
         *
         * @param value value
         * @return Token
         */
        public static Token of(boolean value) {
            return value ? TRUE : FALSE;
        }

        @Override
        public int compareTo(Token o) {
            if (operator != null) {
                return o.operator != null ? operator.compareTo(o.operator) : 1;
            }
            if (operand != null) {
                return o.operand != null ? Value.compare(operand, o.operand) : -1;
            }
            if (variable != null) {
                return o.variable != null ? variable.compareTo(o.variable) : -1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Token)) {
                return false;
            }
            Token token = (Token) o;
            return operator == token.operator
                   && Objects.equals(variable, token.variable)
                   && Value.isEqual(operand, token.operand);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operator, variable, operand);
        }

        @Override
        public String toString() {
            if (operand != null) {
                switch (operand.type()) {
                    case STRING:
                        return "'" + operand.getString() + "'";
                    case BOOLEAN:
                        return String.valueOf(operand.getBoolean());
                    case INTEGER:
                        return String.valueOf(operand.getInt());
                    case LIST:
                        return "["
                               + operand.getList().stream()
                                       .map(s -> "'" + s + "'")
                                       .collect(Collectors.joining(","))
                               + "]";
                    default:
                        throw new IllegalStateException("Unexpected operand type: " + operand.type());
                }
            } else if (variable != null) {
                return "${" + variable + "}";
            } else if (operator != null) {
                return operator.symbol();
            } else {
                return "?";
            }
        }

        private static List<String> parseArray(String symbol) {
            return ARRAY_PATTERN.matcher(symbol)
                    .results()
                    .map(r -> r.group(1))
                    .map(s -> s.substring(1, s.length() - 1))
                    .collect(toList());
        }

        private static String parseVariable(String symbol) {
            return VAR_PATTERN.matcher(symbol)
                    .results()
                    .map(r -> r.group(1))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Incorrect variable name: " + symbol));
        }

        private static Token of(Symbol symbol) {
            switch (symbol.type) {
                case BINARY_OPERATOR:
                case UNARY_OPERATOR:
                    return Token.of(OPS.get(symbol.value));
                case BOOLEAN:
                    return Token.of(Boolean.parseBoolean(symbol.value));
                case STRING:
                    return Token.of(Value.of(symbol.value.substring(1, symbol.value.length() - 1)));
                case INT:
                    return Token.of(Value.of(Integer.parseInt(symbol.value)));
                case ARRAY:
                    return Token.of(Value.of(parseArray(symbol.value)));
                case VARIABLE:
                    return Token.of(parseVariable(symbol.value));
                case PARENTHESIS:
                    throw new FormatException("Unmatched parenthesis");
                default:
                    throw new IllegalStateException("Unexpected symbol" + symbol.value);
            }
        }
    }

    private static final class Symbol {

        private final Type type;
        private final String value;

        Symbol(Type type, String value) {
            this.type = type;
            this.value = value;
        }

        enum Type {
            SKIP("^\\s+"),
            ARRAY("^\\[[^]\\[]*]"),
            BOOLEAN("^(true|false)"),
            STRING("^['\"][^'\"]*['\"]"),
            INT("^\\-?[0-9]+"),
            VARIABLE("^\\$\\{(?<varName>~?[\\w.-]+)}"),
            BINARY_OPERATOR("^([<>=!]=|[<>]|\\|\\||OR|&&|AND|contains)"),
            UNARY_OPERATOR("^(!|NOT|\\(list\\)|\\(string\\)|\\(int\\)|sizeof)"),
            PARENTHESIS("^[()]"),
            COMMENT("#.*\\R");

            private final Pattern pattern;

            Type(String regex) {
                this.pattern = Pattern.compile(regex);
            }
        }

        @Override
        public String toString() {
            return "Symbol{ " + value + " }";
        }
    }

    private static final class Tokenizer implements Iterator<Symbol> {

        private final String line;
        private int cursor;

        Tokenizer(String line) {
            this.line = line;
            this.cursor = 0;
        }

        @Override
        public boolean hasNext() {
            return cursor < line.length();
        }

        @Override
        public Symbol next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String current = line.substring(cursor);
            for (Symbol.Type type : Symbol.Type.values()) {
                Matcher matcher = type.pattern.matcher(current);
                if (matcher.find()) {
                    String value = matcher.group();
                    cursor += value.length();
                    return new Symbol(type, value);
                }
            }
            throw new FormatException("Unexpected token: " + current);
        }
    }

    private static final class Term {
        private final int mark;
        private final int bits;
        private final BitSet ids;

        Term(int bits, int mark, BitSet ids) {
            this.bits = bits;
            this.mark = mark;
            this.ids = ids;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Term)) {
                return false;
            }
            return ids.equals(((Term) o).ids);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(ids);
        }
    }

    private static final class TermTable {
        private final List<Term> terms = new ArrayList<>();
        private final List<Integer> groups = new ArrayList<>();

        void clear() {
            terms.clear();
            groups.clear();
        }

        boolean contains(BitSet ids) {
            int numGroups = groups.size();
            int startIndex = numGroups == 0 ? 0 : groups.get(numGroups - 1);
            int endIndex = numGroups > 1 ? groups.get(numGroups - 2) : terms.size();
            for (int i = startIndex; i < endIndex; i++) {
                if (terms.get(i).ids.equals(ids)) {
                    return true;
                }
            }
            return false;
        }
    }
}
