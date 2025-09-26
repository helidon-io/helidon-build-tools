/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.helidon.build.archetype.engine.v2.Context.Scope;
import io.helidon.build.archetype.engine.v2.Context.ScopeValue;
import io.helidon.build.archetype.engine.v2.Context.ValueKind;
import io.helidon.build.archetype.engine.v2.Expression.Token;
import io.helidon.build.archetype.engine.v2.InputResolver.InvalidInputException;
import io.helidon.build.archetype.engine.v2.InputResolver.ResolvedKind;
import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.archetype.engine.v2.ScriptInvoker.InvocationException;
import io.helidon.build.archetype.engine.v2.Value.Type;
import io.helidon.build.common.BitSets;
import io.helidon.build.common.InputStreams;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.Variations;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.archetype.engine.v2.Nodes.optionIndex;
import static io.helidon.build.archetype.engine.v2.Nodes.options;
import static io.helidon.build.common.Checksum.md5;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.readAllBytes;
import static java.util.Objects.requireNonNull;

/**
 * Script compiler.
 */
public class ScriptCompiler {

    /**
     * Compiler validation exception.
     */
    public static class ValidationException extends RuntimeException {
        private final List<String> errors;

        ValidationException(List<String> errors) {
            this.errors = errors;
        }

        /**
         * Get the errors.
         *
         * @return errors
         */
        public List<String> errors() {
            return errors;
        }

        @Override
        public String getMessage() {
            return System.lineSeparator() + String.join(System.lineSeparator(), errors);
        }
    }

    /**
     * Compiler image.
     */
    public static final class Image {
        private final Node node = Nodes.script();
        private final Map<String, byte[]> blobs = new HashMap<>();

        private Image() {
        }

        /**
         * Get the node.
         *
         * @return Node
         */
        public Node node() {
            return node;
        }

        /**
         * Write the image.
         *
         * @param outputDir output directory
         */
        public void write(Path outputDir) {
            try {
                // write entrypoint
                ensureDirectory(outputDir);
                Path entrypoint = outputDir.resolve("main.xml");
                try (XMLScriptWriter writer = new XMLScriptWriter(Files.newBufferedWriter(entrypoint), true)) {
                    writer.writeScript(node);
                }

                // write blobs
                if (!blobs.isEmpty()) {
                    Path blobsDir = ensureDirectory(outputDir.resolve("blobs"));
                    for (Entry<String, byte[]> entry : blobs.entrySet()) {
                        Files.write(blobsDir.resolve(entry.getKey()), entry.getValue());
                    }
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    /**
     * Compiler option.
     */
    public interface Option {
        /**
         * Run the option.
         *
         * @param compiler compiler
         */
        default void run(ScriptCompiler compiler) {
        }
    }

    /**
     * Compiler options.
     */
    public enum Options implements Option {
        /**
         * Only validate.
         */
        VALIDATE_ONLY,
        /**
         * Skip the validation.
         */
        SKIP_VALIDATION,
        /**
         * Ignore errors.
         */
        IGNORE_ERRORS,
        /**
         * Remove outputs.
         */
        NO_OUTPUT,
        /**
         * Remove transient variables.
         */
        NO_TRANSIENT
    }

    static final String PRESET_UNRESOLVED = "Preset input cannot be resolved";
    static final String PRESET_TYPE_MISMATCH = "Preset type mismatch";
    static final String EXPR_INCOMPATIBLE_OPERATOR = "Expression contains a non compatible operator";
    static final String EXPR_UNRESOLVED_VARIABLE = "Expression contains an unresolved variable";
    static final String EXPR_EVAL_ERROR = "Expression evaluated with an error";
    static final String STEP_NO_INPUT = "Step does not contain any input";
    static final String STEP_DECLARED_OPTIONAL = "Step is declared as optional but includes non optional input";
    static final String STEP_NOT_DECLARED_OPTIONAL = "Step is not declared as optional but includes only optional input";
    static final String INPUT_ALREADY_DECLARED = "Input already declared in current scope";
    static final String INPUT_TYPE_MISMATCH = "Input is declared in another scope with a different type";
    static final String INPUT_OPTIONAL_NO_DEFAULT = "Input is optional but does not have a default value";
    static final String INPUT_NOT_IN_STEP = "Input is not nested within a step";
    static final String OPTION_VALUE_ALREADY_DECLARED = "Option value is already declared";

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Map<Node, String> scopes = new HashMap<>();
    private final Map<Node, Map<String, Value<?>>> paths = new HashMap<>();
    private final Map<String, Set<Node>> declaredValues = new HashMap<>();
    private final Map<Node, Expression> expressions = new HashMap<>();
    private final Map<Node, Map<String, Expression>> refs = new HashMap<>();
    private final Map<String, Type> refTypes = new HashMap<>();
    private final Map<Node, Path> workDirs = new HashMap<>();
    private final Set<String> errors = new LinkedHashSet<>();
    private final Node sourceNode;
    private final Context ctx = new Context();
    private volatile List<Option> options;

    /**
     * Create a new instance.
     *
     * @param source source
     * @param cwd    cwd
     */
    public ScriptCompiler(Script.Source source, Path cwd) {
        this.ctx.pushCwd(requireNonNull(cwd, "cwd is null"));
        this.sourceNode = Script.load(requireNonNull(source, "source is null"), false);
    }

    /**
     * Compile the given script.
     *
     * @param options options
     * @return {@code true} if successful, {@code false} otherwise
     */
    public Image compile(List<Option> options) {
        this.options = options;
        init();

        // validate
        if (!options.contains(Options.SKIP_VALIDATION)) {
            validate();
        }

        // run options
        for (Option option : options) {
            option.run(this);
        }

        // exit on error
        if (!errors.isEmpty() && !options.contains(Options.IGNORE_ERRORS)) {
            throw new ValidationException(Lists.drain(errors));
        }

        Image image = new Image();
        if (!options.contains(Options.VALIDATE_ONLY)) {
            compile(image);
        }
        return image;
    }

    /**
     * Get the source node.
     *
     * @return Node
     */
    public Node sourceNode() {
        init();
        return sourceNode;
    }

    /**
     * Get the validation errors.
     *
     * @return errors
     */
    public Set<String> errors() {
        return errors;
    }

    /**
     * Compute variations.
     *
     * @param filters filters
     * @return variations
     */
    public Set<Map<String, String>> variations(List<Expression> filters) {
        init();
        Set<Map<String, String>> variations = new TreeSet<>(Maps::compare);
        sourceNode.visit(new VariationVisitor(variations, filters));
        return variations;
    }

    private void init() {
        if (initialized.compareAndSet(false, true)) {
            new InlineInvoker().invoke(sourceNode); // inline call sites
            new RefsInvoker().invoke(sourceNode); // gather refs and prune tree
        }
    }

    private void compile(Image image) {
        Map<Node, Node> mirrors = new HashMap<>();
        mirrors.put(sourceNode, image.node);
        mirrors.put(image.node, sourceNode);
        sourceNode.visit(new InputVisitor(image, mirrors)); // render inputs
        if (!options.contains(Options.NO_OUTPUT)) {
            sourceNode.visit(new OutputVisitor(image)); // render outputs
        }
        image.node.visit(new StubsVisitor(mirrors)); // render stubs
        image.node.visit(new DedupVisitor()); // de-dup steps
    }

    private void validate() {
        validatePresets();
        validateInputTypes();
        validateExpressions();
        validateOptions();
        validateInputs();
        validateSteps();
    }

    private void validatePresets() {
        Map<String, List<Node>> inputs = new HashMap<>();
        for (Node node : sourceNode.traverse(Kind::isInput)) {
            inputs.computeIfAbsent(scopeId(node), k -> new ArrayList<>()).add(node);
        }
        for (Node node : sourceNode.traverse(Kind::isPreset)) {
            String path = node.attribute("path").getString();
            List<Node> resolved = inputs.getOrDefault(path, List.of());
            if (resolved.isEmpty()) {
                errors.add(String.format(
                        "%s %s: '%s'",
                        node.location(),
                        PRESET_UNRESOLVED,
                        path));
            } else {
                Type valueType = node.kind().valueType();
                for (Node n : resolved) {
                    if (valueType != n.kind().valueType()) {
                        errors.add(String.format(
                                "%s %s: '%s', expected: %s, actual: %s",
                                n.location(),
                                PRESET_TYPE_MISMATCH,
                                path,
                                node.kind(),
                                n.kind()));
                    }
                }
            }
        }
    }

    private void validateInputTypes() {
        Map<String, Kind> kinds = new HashMap<>();
        for (Node node : sourceNode.traverse(Kind::isInput)) {
            String scopeId = scopeId(node);
            Kind expected = kinds.computeIfAbsent(scopeId, k -> node.kind());
            if (expected != node.kind()) {
                errors.add(String.format("%s %s: '%s'",
                        node.location(),
                        INPUT_TYPE_MISMATCH,
                        scopeId));
            }
        }
    }

    private void validateExpressions() {
        for (Node node : sourceNode.traverse(Kind.CONDITION::equals)) {
            Expression expr = node.expression();

            // check operators compatibility
            for (Token token : expr.tokens()) {
                if (token.isOperator()) {
                    switch (token.operator()) {
                        case AS_INT:
                        case AS_LIST:
                        case AS_STRING:
                        case SIZEOF:
                        case GREATER_THAN:
                        case GREATER_OR_EQUAL:
                        case LOWER_THAN:
                        case LOWER_OR_EQUAL:
                            errors.add(String.format("%s %s: '%s'",
                                    node.location(),
                                    EXPR_INCOMPATIBLE_OPERATOR,
                                    token.operator().symbol()));
                            break;
                        default:
                    }
                }
            }

            boolean resolved = true;
            Scope scope = scope(node);
            Expression blockExpr = expression(node.parent());
            Map<String, Expression> refMap = refs.getOrDefault(node, Map.of());
            for (String variable : expr.variables()) {

                // normalized variable
                String ref = scope.key(variable);

                // expression that represents where the reference is defined
                Expression refExpr0 = refMap.getOrDefault(ref, Expression.FALSE);

                // "relativize" the expression within the block
                Expression refExpr1 = blockExpr.relativize(refExpr0);

                // inline values
                Expression refExpr2 = inline(node, refExpr1);

                if (refExpr2 != Expression.TRUE) {
                    resolved = false;
                    errors.add(String.format("%s %s: '%s'",
                            node.location(),
                            EXPR_UNRESOLVED_VARIABLE,
                            variable));
                }
            }

            // check evaluation
            if (resolved) {
                try {
                    Map<String, Value<?>> variables = new HashMap<>();
                    for (String variable : expr.variables()) {
                        Type type = refTypes.getOrDefault(scope.key(variable), Type.EMPTY);
                        variables.put(variable, Value.typed(type));
                    }
                    expr.eval(variables::get);
                } catch (RuntimeException ex) {
                    errors.add(String.format(
                            "%s %s: '%s'",
                            node.location(),
                            EXPR_EVAL_ERROR,
                            ex.getMessage()));
                }
            }
        }
    }

    private void validateOptions() {
        Map<Node, Set<Node>> options = new HashMap<>();
        for (Node option : sourceNode.traverse(Kind.INPUT_OPTION::equals)) {
            for (Node input : option.ancestors(Kind::isInput)) {
                options.computeIfAbsent(input, k -> new HashSet<>()).add(option);
                break;
            }
        }
        options.forEach((input, nodes) -> {
            Set<String> values = new HashSet<>();
            for (Node option : nodes) {
                String value = option.value().getString();
                if (!values.add(value)) {
                    errors.add(String.format(
                            "%s %s: '%s'",
                            input.location(),
                            OPTION_VALUE_ALREADY_DECLARED,
                            value));
                }
            }
        });
    }

    private void validateInputs() {
        Map<String, List<Node>> inputs = new HashMap<>();
        for (Node input : sourceNode.traverse(Kind::isInput)) {
            Scope scope = scope(input);

            // optional no default
            if (input.attribute("optional").asBoolean().orElse(false)
                && input.attribute("default").isEmpty()) {
                switch (input.kind()) {
                    case INPUT_ENUM:
                    case INPUT_TEXT:
                        errors.add(String.format("%s %s: '%s'",
                                input.location(),
                                INPUT_OPTIONAL_NO_DEFAULT,
                                scope.key()));
                        break;
                    default:
                }
            }

            if (input.ancestor(Kind.STEP::equals).isEmpty()) {
                // input not in a step
                errors.add(String.format("%s %s: '%s'",
                        input.location(),
                        INPUT_NOT_IN_STEP,
                        scope.key()));
            } else {
                // input already declared
                Iterable<Node> actual = input.ancestors(Kind::isInput);
                for (Node n : inputs.getOrDefault(scope.key(), List.of())) {
                    // only allow duplicates if the common ancestor is an enum
                    Node firstMatch = Lists.firstMatch(actual, n.ancestors(Kind::isInput));
                    if (firstMatch == null || firstMatch.kind() != Kind.INPUT_ENUM) {
                        errors.add(String.format("%s %s: '%s'",
                                input.location(),
                                INPUT_ALREADY_DECLARED,
                                scope.key()));
                        break;
                    }
                }
                inputs.computeIfAbsent(scope.key(), k -> new ArrayList<>()).add(input);
            }
        }
    }

    private void validateSteps() {
        for (Node step : sourceNode.traverse(Kind.STEP::equals)) {
            List<Node> inputs = step.collect(Kind::isInput);
            if (inputs.isEmpty()) {
                errors.add(String.format("%s %s",
                        step.location(),
                        STEP_NO_INPUT));
            } else {
                boolean optionalStep = step.attribute("optional").asBoolean().orElse(false);
                boolean optionalInputs = true;
                for (Node input : inputs) {
                    if (!input.attribute("optional").asBoolean().orElse(false)) {
                        optionalInputs = false;
                        break;
                    }
                }
                if (optionalStep && !optionalInputs) {
                    errors.add(String.format("%s %s",
                            step.location(),
                            STEP_DECLARED_OPTIONAL));
                } else if (!optionalStep && optionalInputs) {
                    errors.add(String.format("%s %s",
                            step.location(),
                            STEP_NOT_DECLARED_OPTIONAL));
                }
            }
        }
    }

    private String scopeId(Node node) {
        for (Node n = node; n != null; n = n.parent()) {
            String scopeId = scopes.get(n);
            if (scopeId != null) {
                return scopeId;
            }
        }
        return "";
    }

    private Scope scope(Node node) {
        return ctx.scope().get("~" + scopeId(node));
    }

    private Value<?> declaredValue(Node node, String key) {
        Node node0 = null;
        Expression blockExpr = expression(node.parent());
        for (Node n : declaredValues.getOrDefault(key, Set.of())) {
            if (node.id() > n.id()) {
                // "relativize" the expression within the block
                Expression refExpr = blockExpr.relativize(expression(n));
                if (refExpr == Expression.TRUE) {
                    if (node0 == null || n.id() > node0.id()) {
                        node0 = n;
                    }
                } else if (refExpr != Expression.FALSE) {
                    if (node0 != null && n.id() > node0.id()) {
                        node0 = null;
                    }
                }
            }
        }
        if (node0 != null) {
            return Value.typed(node0.value(), node0.kind().valueType());
        }
        return Value.empty();
    }

    private Expression inline(Node node, Expression expr) {
        try {
            Scope scope = scope(node);
            Map<String, Value<?>> values = path(node);
            return expr.inline(s -> {
                String key = scope.get(s).key();
                Value<?> value = values.get(key);
                if (value == null) {
                    value = declaredValue(node, key);
                }
                return value;
            });
        } catch (RuntimeException ex) {
            errors.add(String.format(
                    "%s %s: '%s'",
                    node.location(),
                    EXPR_EVAL_ERROR,
                    ex.getMessage()));
            return expr;
        }
    }

    private Map<String, Value<?>> path(Node node) {
        return paths.computeIfAbsent(node.parent(), this::path0);
    }

    private Map<String, Value<?>> path0(Node node) {
        Map<String, Value<?>> values = new LinkedHashMap<>();
        for (Node n = node; n != null; n = n.parent()) {
            switch (n.kind()) {
                case INPUT_BOOLEAN:
                    values.put(scopeId(n), Value.TRUE);
                    break;
                case INPUT_OPTION:
                    Node input = n.ancestor(Kind::isInput).orElseThrow();
                    if (input.kind() == Kind.INPUT_ENUM) {
                        values.put(scopeId(input), Value.of(n.value().getString()));
                    }
                    break;
                default:
            }
        }
        return values;
    }

    private Expression normalize(Expression expr, Scope scope) {
        return expr.map(t -> {
            if (t.isVariable()) {
                return Token.of(scope.key(t.variable()));
            }
            return t;
        }).reduce();
    }

    private Expression expression(Node node) {
        return expressions.computeIfAbsent(node, k -> expression(k, this::scopeId));
    }

    private Expression expression(Node node, Function<Node, String> key) {
        Expression expr = Expression.TRUE;
        for (Node n = node; n != null; n = n.parent()) {
            switch (n.kind()) {
                case CONDITION:
                    expr = expr.and(n.expression());
                    break;
                case INPUT_BOOLEAN:
                    expr = expr.and(Expression.create(String.format("${%s}", key.apply(n))));
                    break;
                case INPUT_TEXT:
                    expr = expr.and(Expression.create(String.format("${%s} != ''", key.apply(n))));
                    break;
                case INPUT_OPTION:
                    Node input = n.ancestor(Kind::isInput).orElseThrow();
                    switch (input.kind()) {
                        case INPUT_ENUM:
                            expr = expr.and(Expression.create(String.format(
                                    "${%s} == '%s'",
                                    key.apply(input),
                                    n.value().getString())));
                            break;
                        case INPUT_LIST:
                            expr = expr.and(Expression.create(String.format(
                                    "${%s} contains '%s'",
                                    key.apply(input),
                                    n.value().getString())));
                            break;
                        default:
                    }
                    break;
                default:
            }
        }
        return expr.reduce();
    }

    private void logDuration(long startTime, String msg) {
        long endTime = System.currentTimeMillis();
        Duration duration = Duration.ofMillis(endTime - startTime);
        Log.debug("%s in %d.%ds", msg, duration.toSeconds(), duration.toMillisPart());
    }

    private final class InlineInvoker extends ScriptInvoker {
        private final Deque<Node> callStack = new ArrayDeque<>();
        private final Map<String, Node> methods = new HashMap<>();

        private InlineInvoker() {
            super(ctx);
        }

        @Override
        protected Node load(Script.Loader loader, Script.Source source) {
            // disable caching to get unique nodes for each invocation
            return source.readScript(false, Script.Loader.EMPTY);
        }

        @Override
        public boolean visit(Node node) {
            switch (node.kind()) {
                case CALL:
                    Script script = node.script();
                    String method = node.attribute("method").getString();
                    Node prototype = methods.computeIfAbsent(script.path() + ":" + method, k -> script.methods().get(method));
                    if (prototype == null) {
                        throw new IllegalStateException("Method not found: " + method);
                    }
                    String hash = md5(node.location());
                    script.methods().put(hash, prototype.deepCopy().attribute("name", hash));
                    callStack.push(node.attribute("method", hash));
                    break;
                case SOURCE:
                case EXEC:
                    if (node.attribute("url").isPresent()) {
                        // skip url invocation
                        return false;
                    }
                    callStack.push(node);
                    break;
                case FILE:
                case TEMPLATE:
                case FILES:
                case TEMPLATES:
                case OUTPUT:
                    workDirs.put(node, ctx.cwd());
                    break;
                case CONDITION:
                case VARIABLE_TEXT:
                case VARIABLE_ENUM:
                case VARIABLE_BOOLEAN:
                case VARIABLE_LIST:
                case PRESET_TEXT:
                case PRESET_ENUM:
                case PRESET_BOOLEAN:
                case PRESET_LIST:
                    return true;
                default:
            }
            return super.visit(node);
        }

        @Override
        public void postVisit(Node node) {
            switch (node.kind()) {
                case CALL:
                    String method = node.attribute("method").getString();
                    node.script().methods().remove(method);
                    break;
                case SOURCE:
                case EXEC:
                    if (node.attribute("url").isPresent()) {
                        // skip url invocation
                        return;
                    }
                    break;
                case METHOD:
                case SCRIPT:
                    if (!callStack.isEmpty()) {
                        callStack.pop().replace(node.children());
                    }
                    break;
                default:
            }
            super.postVisit(node);
        }
    }

    private final class RefsInvoker extends ScriptInvoker {

        private final Map<String, Expression> currentRefs = new HashMap<>();
        private final Set<Node> modifiedSteps = new HashSet<>();
        private int nextId = 0;

        private RefsInvoker() {
            super(ctx);
        }

        @Override
        public boolean visit(Node node) {
            node.id(nextId++);
            Scope scope = ctx.scope();
            switch (node.kind()) {
                case SOURCE:
                case EXEC:
                    scopes.put(node, scope.key());
                    if (node.attribute("url").isPresent()) {
                        // skip url invocation
                        return false;
                    }
                    break;
                case CONDITION:
                    scopes.put(node, scope.key());
                    Expression expr = inline(node, node.expression());
                    if (expr != Expression.FALSE) {
                        node.expression(expr);
                        refs.put(node, Map.copyOf(currentRefs));
                        return true;
                    }
                    // condition is always false
                    // skip traversal and prune (postVisit)
                    node.expression(Expression.FALSE);
                    return false;
                case INPUT_TEXT:
                case INPUT_BOOLEAN:
                case INPUT_LIST:
                case INPUT_ENUM:
                    scope = ctx.pushScope(s -> s.getOrCreate(node));
                    scopes.put(node, scope.key());
                    refTypes.putIfAbsent(scope.key(), node.kind().valueType());
                    currentRefs.compute(scope.key(), (k, v) -> expression(node.parent()).or(v).reduce());
                    if (inline(node, expression(node)) == Expression.FALSE) {
                        return false;
                    }
                    break;
                case INPUT_OPTION:
                    scopes.put(node, scope.key());
                    break;
                case VARIABLE_TEXT:
                case VARIABLE_ENUM:
                case VARIABLE_BOOLEAN:
                case VARIABLE_LIST:
                case PRESET_TEXT:
                case PRESET_ENUM:
                case PRESET_BOOLEAN:
                case PRESET_LIST:
                    scope = scope.getOrCreate("~" + Context.Key.normalize(node.attribute("path").getString()));
                    scopes.put(node, scope.key());
                    declaredValues.computeIfAbsent(scope.key(), k -> new LinkedHashSet<>()).add(node);
                    refTypes.putIfAbsent(scope.key(), node.kind().valueType());
                    currentRefs.compute(scope.key(), (k, v) -> expression(node.parent()).or(v).reduce());
                    return true;
                default:
            }
            return super.visit(node);
        }

        @Override
        public void postVisit(Node node) {
            Expression expr;
            switch (node.kind()) {
                case SOURCE:
                case EXEC:
                    if (node.attribute("url").isPresent()) {
                        // skip url invocation
                        return;
                    }
                    break;
                case CONDITION:
                    if (node.expression() == Expression.FALSE) {
                        remove(node);
                    }
                    break;
                case INPUT_BOOLEAN:
                case INPUT_TEXT:
                    expr = inline(node, expression(node));
                    if (expr == Expression.FALSE) {
                        remove(node);
                        node.ancestor(Kind.STEP::equals).ifPresent(modifiedSteps::add);
                    }
                    ctx.popScope();
                    break;
                case INPUT_LIST:
                case INPUT_ENUM:
                    expr = inline(node, expression(node));
                    if (expr == Expression.FALSE || node.children().isEmpty()) {
                        remove(node);
                        node.ancestor(Kind.STEP::equals).ifPresent(modifiedSteps::add);
                    }
                    ctx.popScope();
                    break;
                case INPUT_OPTION:
                    expr = inline(node, expression(node));
                    if (expr == Expression.FALSE) {
                        remove(node);
                    }
                    break;
                case STEP:
                    if (modifiedSteps.remove(node)) {
                        // only remove steps that have been modified
                        // to detect steps without inputs during validation
                        List<Node> inputs = node.collect(Kind::isInput);
                        if (inputs.isEmpty()) {
                            node.replace(node.children());
                        }
                    }
                    break;
                default:
            }
            super.postVisit(node);
        }

        void remove(Node node) {
            Log.debug("Removing %s, path: %s, expression: %s",
                    node,
                    Maps.mapValue(path(node), v -> Value.toString(v)),
                    expression(node.parent()));
            node.remove();
        }
    }

    private class InputVisitor implements Node.Visitor {
        private final Deque<Node> stack = new ArrayDeque<>();
        private final Map<Node, Node> mirrors;
        private final Image image;

        InputVisitor(Image image, Map<Node, Node> mirrors) {
            this.image = image;
            this.mirrors = mirrors;
            this.stack.push(image.node);
        }

        @Override
        public boolean visit(Node node) {
            switch (node.kind()) {
                case PRESET_BOOLEAN:
                case PRESET_ENUM:
                case PRESET_LIST:
                case PRESET_TEXT:
                    // use ~ to be parented at the root context node
                    // to maintain scope.key == scope.internalKey
                    Node preset = process(node, (b, n) -> Nodes.ensureLast(b, Kind.PRESETS).append(n));
                    preset.attribute("path", "~" + scopeId(node));
                    stack.push(preset);
                    break;
                case VARIABLE_BOOLEAN:
                case VARIABLE_ENUM:
                case VARIABLE_LIST:
                case VARIABLE_TEXT:
                    if (!options.contains(Options.NO_TRANSIENT)
                        || !node.attribute("transient").asBoolean().orElse(false)) {

                        // use ~ to be parented at the root context node
                        // to maintain scope.key == scope.internalKey
                        Node variable = process(node, (b, n) -> Nodes.ensureLast(b, Kind.VARIABLES).append(n));
                        variable.attribute("path", "~" + scopeId(node));
                        stack.push(variable);
                    }
                    break;
                case INPUT_BOOLEAN:
                case INPUT_ENUM:
                case INPUT_TEXT:
                case INPUT_LIST:
                    // flatten inputs to maintain scope.key == scope.internalKey (!)
                    // I.e. inputs are only nested to produce a dotted path

                    // flatten if enclosing input is effectively global (discrete)
                    // I.e., input is global or enclosing input is global
                    boolean global = node.attribute("global").asBoolean().orElse(false);
                    boolean discrete = global || node.ancestor(Kind::isInput)
                            .map(n -> n.attribute("global").asBoolean().orElse(false))
                            .orElse(false);

                    // process steps lazily
                    Node block = node.ancestor(Kind::isBlock).orElseThrow();
                    if (block.kind() == Kind.STEP || discrete) {
                        Node step = node.ancestor(Kind.STEP::equals).orElseThrow();
                        Node stepCopy = mirrors.get(step);
                        if (stepCopy == null) {
                            if (discrete) {
                                if (image.node != stack.peek()) {
                                    // ensure top level
                                    stack.push(image.node);
                                }
                            }

                            // create the step
                            stepCopy = process(step, Node::append);
                            stack.push(stepCopy);
                        } else if (discrete) {
                            // push copy as the current block
                            if (stepCopy != stack.peek()) {
                                stack.push(stepCopy);
                            }
                        }
                    }

                    // add input
                    stack.push(process(node, (b, n) -> Nodes.ensureLast(b, Kind.INPUTS).append(n)));
                    break;
                case INPUT_OPTION:
                case EXEC:
                case SOURCE:
                case CALL:
                    stack.push(process(node, Node::append));
                    break;
                default:
            }
            return true;
        }

        @Override
        public void postVisit(Node node) {
            if (node.kind() == Kind.SCRIPT) {

                // group steps with same input keys (pseudo breadth first)
                // this is required to avoid stubs that trump input values (!)
                List<Node> nodes = image.node.children();
                List<Node> steps = Lists.filter(nodes, n -> n.kind().is(Kind.CONDITION, Kind.STEP));
                if (!steps.isEmpty()) {

                    // sort the steps by nested input keys
                    Map<String, Set<Node>> refs = new LinkedHashMap<>();
                    for (Node step : steps) {
                        for (Node input : step.traverse(Kind::isInput)) {
                            refs.computeIfAbsent(scopeId(mirror(input)), k -> new LinkedHashSet<>()).add(step);
                        }
                    }

                    // group sorted steps by name
                    List<List<Node>> groups = Lists.groupingBy(Lists.flatMapDistinct(refs.values()),
                            n -> n.unwrap().attribute("name").getString());

                    // sort the groups using the highest depth first index in the source tree
                    List<List<Node>> sortedGroups = Lists.sorted(groups,
                            Comparator.comparingInt(g -> Lists.max(g, n -> mirror(n.unwrap()).id())));

                    // flatten the groups
                    List<Node> sortedSteps = Lists.flatMap(sortedGroups);

                    // replace steps
                    for (int i = 0, j = 0; i < nodes.size(); i++) {
                        if (nodes.get(i).unwrap().kind() == Kind.STEP) {
                            nodes.set(i, Nodes.parent(sortedSteps.get(j++), image.node));
                        }
                    }
                }
            } else {
                Node copy = mirrors.get(node);
                if (copy != null) {
                    while (!stack.isEmpty()) {
                        Node n = stack.pop();
                        if (copy == n) {
                            break;
                        }
                    }
                }
            }
        }

        Node process(Node node, BiConsumer<Node, Node> appender) {
            Node blockCopy = stack.getFirst();
            Node block = mirrors.get(blockCopy);

            Scope scope = scope(block);

            // "relativize" the expression within the block
            Expression expr = normalize(expression(block).relativize(expression(node.parent())), scope);

            // create copy
            Node copy = node.copy();
            appender.accept(blockCopy, copy.wrap(expr));

            mirrors.put(node, copy);
            mirrors.put(copy, node);
            return copy;
        }

        Node mirror(Node node) {
            Node mirror = mirrors.get(node);
            if (mirror != null) {
                return mirror;
            } else {
                throw new IllegalStateException("Mirror not found: " + node);
            }
        }
    }

    private final class OutputVisitor implements Node.Visitor {
        private final Map<String, Map<List<FileOp>, Expression>> fileOps = new HashMap<>();
        private final Set<FileObject> files = new TreeSet<>();
        private final Set<FileObject> templates = new TreeSet<>();
        private final Image image;

        private OutputVisitor(Image image) {
            this.image = image;
        }

        @Override
        public boolean visit(Node node) {
            switch (node.kind()) {
                case TRANSFORMATION:
                    List<FileOp> ops = new ArrayList<>();
                    for (Node n : node.traverse(Kind.REPLACE::equals)) {
                        ops.add(new FileOp(
                                n.attribute("regex").getString(),
                                n.attribute("replacement").getString()));
                    }
                    fileOps.computeIfAbsent(node.attribute("id").getString(), k -> new HashMap<>())
                            .compute(ops, (k, v) -> {
                                Expression expr = v != null ? v : Expression.FALSE;
                                return expr.or(normalize(expression(node), scope(node)));
                            });
                    break;
                case FILE:
                    files.add(resolveFile(node));
                    break;
                case TEMPLATE:
                    templates.add(resolveFile(node));
                    break;
                case FILES:
                    files.addAll(resolveFiles(node));
                    break;
                case TEMPLATES:
                    templates.addAll(resolveFiles(node));
                    break;
                default:
            }
            return true;
        }

        @Override
        public void postVisit(Node node) {
            if (node.kind() == Kind.SCRIPT) {
                Node output = Nodes.output();

                // add files
                for (Node n : renderFiles(files, "file", Nodes::files)) {
                    output.append(n);
                }

                // add templates
                for (Node n : renderFiles(templates, "template", Nodes::templates)) {
                    output.append(n);
                }

                // add models
                if (!output.children().isEmpty()) {
                    Node model = Nodes.model();
                    for (Node n : renderModels()) {
                        model.append(n);
                    }
                    if (!model.children().isEmpty()) {
                        output.append(model);
                    }
                }

                // add output
                if (!output.children().isEmpty()) {
                    image.node.append(output);
                }
            }
        }

        FileObject resolveFile(Node node) {
            String source = node.attribute("source").getString();
            String target = node.attribute("target").getString();
            Path path = workDirs.get(node).resolve(source);
            String checksum = checksum(path);
            image.blobs.putIfAbsent(checksum, readAllBytes(path));
            List<FileOp> fileOps = List.of(new FileOp(Pattern.quote(checksum), target));
            return new FileObject(checksum, fileOps, normalize(expression(node), scope(node)));
        }

        Set<FileObject> resolveFiles(Node node) {
            Set<FileObject> fileObjects = new TreeSet<>();

            // resolve variations of transformations
            List<List<FileOps>> allOps = Lists.filter(Variations.ofList(fileOps(node)), l -> !l.isEmpty());
            Set<FileOps> resolvedOps = new TreeSet<>(Lists.map(allOps, FileOps::combine));

            Map<String, Expression> includes = new HashMap<>();
            Map<String, Expression> excludes = new HashMap<>();

            Scope scope = scope(node);
            for (Node n : node.traverse()) {
                switch (n.kind()) {
                    case INCLUDE:
                    case EXCLUDE:
                        Map<String, Expression> map = n.kind() == Kind.INCLUDE ? includes : excludes;
                        map.compute(n.value().getString(), (k, v) -> {
                            Expression expr = v == null ? Expression.TRUE : v;
                            if (n.parent().kind() == Kind.CONDITION) {
                                expr = expr.and(normalize(n.parent().expression(), scope));
                            }
                            return expr;
                        });
                        break;
                    default:
                }
            }

            Path directory = workDirs.get(node).resolve(node.attribute("directory").getString());
            for (SourcePath file : SourcePath.scan(directory)) {

                // filter manually to collect expressions
                Expression filterExpr = Expression.FALSE;
                for (Expression v : Maps.filterKey(includes, file::matches).values()) {
                    filterExpr = filterExpr.or(v);
                }
                for (Expression v : Maps.filterKey(excludes, file::matches).values()) {
                    filterExpr = filterExpr.and(v.negate());
                }

                Expression blobExpr = normalize(expression(node), scope).and(filterExpr).reduce();
                if (includes.isEmpty() || blobExpr != Expression.FALSE) {
                    String source = file.asString(false);
                    Path path = directory.resolve(source);

                    // record blob
                    String checksum = checksum(path);
                    image.blobs.putIfAbsent(checksum, readAllBytes(path));

                    if (resolvedOps.isEmpty()) {
                        List<FileOp> fileOps = List.of(new FileOp(Pattern.quote(checksum), source));
                        fileObjects.add(new FileObject(checksum, fileOps, blobExpr));
                    } else {
                        // create a unique file object for each transformation variation
                        for (FileOps e : resolvedOps) {
                            List<FileOp> fileOps = e.resolve(checksum, source);
                            Expression fileExpr = blobExpr.and(e.expression).reduce();
                            fileObjects.add(new FileObject(checksum, fileOps, fileExpr));
                        }
                    }
                }
            }
            return fileObjects;
        }

        List<Node> renderFiles(Set<FileObject> files, String prefix, BiFunction<String, List<String>, Node> func) {
            List<Node> nodes = new ArrayList<>();

            // split duplicates in different groups
            Map<Integer, Set<FileObject>> groups = new HashMap<>();

            // the map key is the number of occurrences
            Map<String, Integer> groupIndexes = new HashMap<>();
            for (FileObject file : files) {
                int index = groupIndexes.compute(file.checksum, (k, v) -> v == null ? 1 : v + 1);
                groups.computeIfAbsent(index, i -> new TreeSet<>()).add(file);
            }

            // compute all transformations
            Map<String, Set<FileOp>> transformations = new TreeMap<>();

            // transformation ids by group index
            Map<Integer, List<String>> groupIds = new TreeMap<>();
            groups.forEach((index, groupFiles) -> {
                Set<String> ids = new TreeSet<>();
                for (FileObject file : groupFiles) {
                    String id;
                    if (file.ops.size() > 1) {
                        // ops are not folded, create a unique transformation
                        id = prefix + "-blobs-" + index + "-" + file.checksum;
                    } else {
                        id = prefix + "-blobs-" + index;
                    }
                    ids.add(id);
                    transformations.computeIfAbsent(id, k -> new LinkedHashSet<>()).addAll(file.ops);
                }
                groupIds.put(index, new ArrayList<>(ids));
            });

            // render all transformations
            transformations.forEach((id, v) -> {
                List<Node> children = Lists.map(v, op -> Nodes.replace(op.regex, op.replacement));
                nodes.add(Nodes.transformation(id, children));
            });

            // render resources directive
            groups.forEach((index, group) -> {
                List<String> ids = groupIds.getOrDefault(index, List.of());
                Node directive = func.apply("blobs", ids);
                Node includes = Nodes.includes();
                for (FileObject f : group) {
                    if (f.expression != Expression.FALSE) {
                        includes.append(Nodes.include(f.checksum).wrap(f.expression));
                    }
                }
                // add an empty include to avoid matching all blobs
                includes.append(Nodes.include(""));
                directive.append(includes);
                nodes.add(directive);
            });
            return nodes;
        }

        List<List<FileOps>> fileOps(Node node) {
            List<List<FileOps>> ops = new ArrayList<>();
            List<String> ids = node.attribute("transformations").asList().orElse(List.of());
            for (String id : ids) {
                Map<List<FileOp>, Expression> idOps = fileOps.getOrDefault(id, Map.of());
                ops.add(Lists.map(idOps.entrySet(), e -> new FileOps(e.getKey(), e.getValue())));
            }
            return ops;
        }

        List<Node> renderModels() {
            List<Node> nodes = new ArrayList<>();
            for (Node node : sourceNode.traverse(Kind::isModel)) {
                Node parent = node.parent();
                if (parent != null && parent.kind() == Kind.CONDITION) {
                    parent = parent.parent();
                }
                if (parent != null && parent.kind() == Kind.MODEL) {
                    Path basedir = workDirs.get(node.ancestor(Kind.OUTPUT::equals).orElseThrow());
                    if (basedir == null) {
                        throw new IllegalStateException("Unresolved cwd");
                    }
                    Scope scope = scope(node);
                    Expression expr = normalize(expression(node), scope).reduce();
                    if (expr != Expression.FALSE) {
                        Node copy = node.deepCopy();
                        for (Node n : copy.traverse(Kind.MODEL_VALUE::equals)) {
                            Node p = n.parent();
                            if (p.kind() == Kind.CONDITION) {
                                p.expression(normalize(p.expression(), scope));
                            }
                            String value = n.value().asString().orElse("");
                            if (value.matches("^\\s+.*") || value.matches(".*\\s+$") || value.contains("\n")) {
                                // move text model with leading, trailing whitespaces or newlines to blobs
                                String id = md5(n.location());
                                image.blobs.put(id, value.getBytes(StandardCharsets.UTF_8));
                                n.attribute("file", "blobs/" + id);
                                n.value(Value.empty());
                            } else {
                                String file = n.attribute("file").asString().orElse(null);
                                if (file != null) {
                                    Path path = basedir.resolve(file);
                                    String checksum = checksum(path);
                                    image.blobs.putIfAbsent(checksum, readAllBytes(path));
                                    n.attribute("file", "blobs/" + checksum);
                                    n.value(Value.empty());
                                } else if (value.isEmpty()) {
                                    // force an empty CDATA
                                    n.value(Value.EMPTY_STRING);
                                }
                            }
                        }
                        nodes.add(copy.wrap(expr));
                    }
                }
            }
            return nodes;
        }

        String checksum(Path path) {
            try {
                return md5(InputStreams.normalizeNewLines(Files.newInputStream(path)));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    private class StubsVisitor implements Node.Visitor {
        private final Map<String, Expression> currentRefs = new HashMap<>();
        private final Map<Node, Map<String, Expression>> refs = new LinkedHashMap<>();
        private final Map<Node, Node> mirrors;

        StubsVisitor(Map<Node, Node> mirrors) {
            this.mirrors = mirrors;
        }

        @Override
        public boolean visit(Node node) {
            switch (node.kind()) {
                case PRESET_BOOLEAN:
                case PRESET_ENUM:
                case PRESET_LIST:
                case PRESET_TEXT:
                case VARIABLE_BOOLEAN:
                case VARIABLE_ENUM:
                case VARIABLE_LIST:
                case VARIABLE_TEXT:
                case INPUT_BOOLEAN:
                case INPUT_ENUM:
                case INPUT_TEXT:
                case INPUT_LIST:
                    // NOTE: should substitute truthy expressions (E.g. ${flavor} == 'se' || ${flavor} == 'mp')
                    Node mirror = mirror(node);
                    Scope scope = scope(mirror);
                    Expression expr0 = expression(node.parent(), n -> scopeId(mirror(n)));
                    Expression expr = expr0.inline(s -> declaredValue(mirror, scope.get(s).key()));
                    currentRefs.compute(scopeId(mirror), (k, v) -> expr.or(v).reduce());
                    break;
                case CONDITION:
                    refs.put(node, Map.copyOf(currentRefs));
                    break;
                default:
            }
            return true;
        }

        @Override
        public void postVisit(Node node) {
            if (node.kind() == Kind.SCRIPT) {
                refs.forEach((n, snapshot) -> {
                    List<Node> stubs = resolveStubs(n, snapshot);
                    if (!stubs.isEmpty()) {
                        addVariables(n, stubs);
                    }
                });
            }
        }

        void addVariables(Node node, List<Node> nodes) {
            Node block = node.ancestor(Kind::isBlock).orElseThrow();

            // collect variables in the block
            Map<String, Expression> existing = new LinkedHashMap<>();
            for (Node n0 : block.children(Kind.VARIABLES::equals)) {
                for (Node n1 : n0.children()) {
                    existing.put(n1.unwrap().attribute("path").getString(), n1.expression());
                }
            }

            // filter stubs that already exist
            List<Node> stubs = new ArrayList<>();
            for (Node n : nodes) {
                String path = n.unwrap().attribute("path").getString();
                Expression expr = existing.get(path);
                if (!n.expression().equals(expr)) {
                    stubs.add(n);
                }
            }

            if (!stubs.isEmpty()) {
                Node container = null;
                for (Node n = node; n != null && container == null; n = n.parent()) {
                    Node parent = n.parent();
                    if (parent.kind() == Kind.INPUTS) {
                        int index = n.index();
                        if (index == 0) {
                            container = Nodes.ensureBefore(parent, Kind.VARIABLES);
                        } else {
                            int p = parent.index();

                            // insert a variables container
                            container = Nodes.variables();
                            parent.parent().append(p + 1, container);

                            // move remaining siblings
                            // to their own inputs container
                            Node inputs = Nodes.inputs();
                            parent.parent().append(p + 2, inputs);
                            for (int j = index, size = parent.children().size(); j < size; j++) {
                                inputs.append(parent.children().remove(index));
                            }
                        }
                    } else if (parent.kind().isBlock()) {
                        container = Nodes.ensureBefore(n, Kind.VARIABLES);
                    }
                }
                if (container != null) {
                    for (Node stub : stubs) {
                        container.append(stub);
                    }
                } else {
                    throw new IllegalStateException("Unable to resolve stubs container");
                }
            }
        }

        List<Node> resolveStubs(Node node, Map<String, Expression> snapshot) {
            Set<String> variables = node.expression().variables();
            if (variables.isEmpty()) {
                return List.of();
            }

            List<Node> nodes = new ArrayList<>();
            Node block = node.ancestor(Kind::isBlock).orElseThrow();
            Scope scope = ctx.scope().get("~" + scopes.getOrDefault(mirror(block), ""));
            Expression blockExpr = expression(block, n -> scopeId(mirror(n)));
            for (String ref : variables) {

                // normalize the ref
                String key = scope.key(ref);

                // expressions that represent all cases where the reference is defined
                Expression refExpr = snapshot.getOrDefault(key, Expression.FALSE);

                // substitute parent expression
                Expression refExpr1 = blockExpr.relativize(refExpr);
                if (refExpr1 == Expression.TRUE) {
                    // resolved
                    continue;
                }

                Expression stubExpr = refExpr1.negate().reduce();
                Type type = refTypes.getOrDefault(key, Type.EMPTY);
                nodes.add(Nodes.variable(type, "~" + key).wrap(stubExpr));
            }
            return nodes;
        }

        Node mirror(Node node) {
            Node mirror = mirrors.get(node);
            if (mirror != null) {
                return mirror;
            } else {
                throw new IllegalStateException("Mirror not found: " + node);
            }
        }
    }

    private final class DedupVisitor implements Node.Visitor {

        private final Map<String, List<Node>> steps = new LinkedHashMap<>();

        @Override
        public boolean visit(Node node) {
            if (node.kind() == Kind.STEP) {
                String name = node.attribute("name").getString();
                steps.computeIfAbsent(name, k -> new ArrayList<>()).add(node);
                return false;
            }
            return true;
        }

        @Override
        public void postVisit(Node node) {
            if (node.kind() == Kind.SCRIPT) {
                for (List<Node> steps : steps.values()) {
                    List<List<Node>> groups = Lists.groupingBy(steps, step -> Lists.map(step.collect(), Nodes::hash));
                    for (List<Node> group : groups) {
                        Node first = null;
                        for (Node step : group) {
                            if (first == null) {
                                first = step;
                            } else {
                                Node p = first.parent();
                                if (p.kind() == Kind.CONDITION) {
                                    p.expression(p.expression().or(expression(step)));
                                }
                                step.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    private final class VariationVisitor implements Node.Visitor {

        class Column {
            private final String name;
            private final String value;

            Column(String name, String value) {
                this.name = name;
                this.value = value;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Column)) {
                    return false;
                }
                Column column = (Column) o;
                return Objects.equals(name, column.name)
                       && Objects.equals(value, column.value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, value);
            }

            @Override
            public String toString() {
                return name + "=" + value;
            }
        }

        class Table {
            private final List<Column> columns = new ArrayList<>();
            private final Set<BitSet> rows = new LinkedHashSet<>();
            private final String id;
            private final Node node;
            private final Expression expr;

            Table(Node node) {
                this.id = scopeId(node);
                this.node = node;
                this.expr = expression(node.parent());
            }

            @Override
            public String toString() {
                return id;
            }
        }

        private final List<Table> inputs = new ArrayList<>();
        private final List<Column> columns = new ArrayList<>();
        private final Map<Column, Integer> indexes = new LinkedHashMap<>();
        private final Set<Map<String, String>> variations;
        private final List<Expression> filters;

        VariationVisitor(Set<Map<String, String>> variations, List<Expression> filters) {
            this.variations = variations;
            this.filters = filters;
        }

        @Override
        public boolean visit(Node node) {
            Table table;
            List<Node> options;
            switch (node.kind()) {
                case INPUT_TEXT:
                    table = new Table(node);
                    String textValue = declaredValue(node, table.id).asString()
                            .or(() -> node.attribute("default").asString())
                            .orElse("<?>");
                    table.columns.add(new Column(table.id, textValue));
                    table.rows.add(BitSets.of(0));
                    inputs.add(table);
                    break;
                case INPUT_BOOLEAN:
                    table = new Table(node);
                    table.columns.add(new Column(table.id, "true"));
                    table.columns.add(new Column(table.id, "false"));
                    table.rows.add(BitSets.of(0));
                    if (!declaredValue(node, table.id).asBoolean().orElse(false)) {
                        // no preset, add false
                        table.rows.add(BitSets.of(1));
                    }
                    inputs.add(table);
                    break;
                case INPUT_ENUM:
                    table = new Table(node);
                    options = options(node);
                    for (Node o : options) {
                        table.columns.add(new Column(table.id, o.value().getString()));
                    }
                    int index = declaredValue(node, table.id).asString()
                            .map(o -> optionIndex(o, options))
                            .orElse(-1);
                    if (index >= 0) {
                        // only add the preset option
                        table.rows.add(BitSets.of(index));
                    } else {
                        for (int i = 0; i < table.columns.size(); i++) {
                            table.rows.add(BitSets.of(i));
                        }
                    }
                    inputs.add(table);
                    break;
                case INPUT_LIST:
                    table = new Table(node);
                    options = options(node);
                    for (Node o : options) {
                        table.columns.add(new Column(table.id, o.value().getString()));
                    }
                    Value<List<String>> value = declaredValue(node, table.id).asList();
                    if (value.isPresent()) {
                        // only add the preset options
                        int p = 0;
                        for (String o : value.getList()) {
                            p |= 1 << optionIndex(o, options);
                        }
                        table.rows.add(BitSets.of((long) p));
                    } else {
                        for (int p = 1, permSize = 1 << table.columns.size(); p < permSize; p++) {
                            table.rows.add(BitSets.of((long) p));
                        }
                    }
                    table.columns.add(new Column(table.id, "none"));
                    table.rows.add(BitSets.of(table.columns.size() - 1));
                    inputs.add(table);
                    break;
                default:
            }
            return true;
        }

        @Override
        public void postVisit(Node node) {
            if (node.kind() == Kind.SCRIPT) {
                // aggregate all columns
                for (Table table : inputs) {
                    for (Column column : table.columns) {
                        int index = indexes.computeIfAbsent(column, e -> indexes.size());
                        if (index == columns.size()) {
                            columns.add(column);
                        }
                    }
                }

                // remap against the aggregated columns
                List<Table> tables = new ArrayList<>();
                for (Table input : inputs) {
                    Table table = new Table(input.node);
                    table.columns.addAll(input.columns);
                    for (BitSet row : input.rows) {
                        BitSet bitSet = new BitSet();
                        for (int i = row.nextSetBit(0); i >= 0 && i < Integer.MAX_VALUE; i = row.nextSetBit(i + 1)) {
                            bitSet.set(indexes.get(input.columns.get(i)));
                        }
                        table.rows.add(bitSet);
                    }
                    tables.add(table);
                }

                // compute the variations
                long computeStartTime = System.currentTimeMillis();
                Set<BitSet> merged = new LinkedHashSet<>();
                for (int i = 0; i < tables.size(); i++) {
                    Table table = tables.get(i);

                    // filter rows to compute based on input expression
                    List<BitSet> filtered = new ArrayList<>();
                    Iterator<BitSet> it = merged.iterator();
                    while (it.hasNext()) {
                        BitSet row = it.next();
                        Map<String, String> variation = variation(row);
                        if (eval(table.node, table.expr, variation)) {
                            filtered.add(row);
                            it.remove();
                        }
                    }

                    Log.debug("Progress: %d/%d - %s - filtered: %d, merged: %d",
                            i + 1,
                            tables.size(),
                            table,
                            filtered.size(),
                            merged.size());

                    // compute variations for the input
                    List<BitSet> computed = new ArrayList<>();
                    if (filtered.isEmpty()) {
                        computed.addAll(table.rows);
                    } else {
                        for (BitSet row1 : table.rows) {
                            for (BitSet row2 : filtered) {
                                computed.add(BitSets.or(BitSets.copyOf(row1), row2));
                            }
                        }
                    }

                    // apply excludes
                    for (BitSet row : computed) {
                        Map<String, String> vars = variation(row);
                        if (filter(node, vars)) {
                            merged.add(row);
                        }
                    }
                }
                logDuration(computeStartTime, "Computed " + merged.size() + " variations");

                // normalize variations
                // perform an execution and use the context values
                long normalizeStartTime = System.currentTimeMillis();
                Map<String, Map<String, String>> result = new HashMap<>();
                for (BitSet row : merged) {
                    Map<String, String> variation = variation(row);
                    Map<String, ScopeValue<?>> effective = execute(variation);
                    if (!effective.isEmpty()) {

                        // compute signature, sorted user values only
                        Map<String, String> normalized = Maps.mapValue(effective,
                                (k, v) -> v.kind() == ValueKind.USER, v -> Value.toString(v), TreeMap::new);

                        // signature
                        String sig = Lists.join(normalized.entrySet(), " ");

                        // compute duplicates
                        result.compute(sig, (k, v) -> {
                            // chose duplicates with the most values
                            if (v == null || effective.size() > v.size()) {
                                return Maps.mapValue(effective, e -> Value.toString(e));
                            }
                            return v;
                        });
                    }
                }
                logDuration(normalizeStartTime, "Normalized " + merged.size() + " variations");

                long sortStartTime = System.currentTimeMillis();
                variations.addAll(result.values());
                logDuration(sortStartTime, "Sorted " + variations.size() + " variations");
            }
        }

        Map<String, String> variation(BitSet row) {
            Map<String, String> variation = new LinkedHashMap<>();
            for (int i = row.nextSetBit(0); i >= 0 && i < Integer.MAX_VALUE; i = row.nextSetBit(i + 1)) {
                Column column = columns.get(i);
                variation.compute(column.name, (k, v) -> v == null ? column.value : v + "," + column.value);
            }
            return variation;
        }

        boolean filter(Node node, Map<String, String> variation) {
            for (Expression exclude : filters) {
                if (eval(node, exclude, variation)) {
                    if (LogLevel.isDebug()) {
                        Log.debug("Excluding variation, rule: %s, entries: %s", exclude.literal(), variation);
                    }
                    return false;
                }
            }
            return true;
        }

        boolean eval(Node node, Expression expr, Map<String, String> variation) {
            try {
                Scope scope = ctx.scope().get("~" + scopes.getOrDefault(node, ""));
                return expr.eval(s -> {
                    String v = variation.get(scope.key(s));
                    if (v != null) {
                        return Value.dynamic(v);
                    }
                    return null;
                });
            } catch (Expression.UnresolvedVariableException ignored) {
                return false;
            }
        }

        Map<String, ScopeValue<?>> execute(Map<String, String> variation) {
            try {
                // initialize a context with the variations
                Context context = new Context().pushCwd(ctx.cwd());
                variation.forEach((k, v) -> {
                    Scope scope = context.scope().getOrCreate(k);
                    scope.value(Value.dynamic(v), ValueKind.USER);
                });

                // record the scopes in traversal order
                Set<Scope> scopes = new LinkedHashSet<>();
                ScriptInvoker.invoke(sourceNode, context, new InputResolver.BatchResolver(context), n -> {
                    scopes.add(context.scope());
                    return true;
                });

                // return values in traversal order
                Map<String, ScopeValue<?>> values = new LinkedHashMap<>();
                for (Scope scope : scopes) {
                    if (scope.parent() != null) {
                        scope.values().forEach((k, v) -> {
                            if (v.isPresent()) {
                                switch (v.kind()) {
                                    case USER:
                                        if (!scopes.contains(v.scope())) {
                                            // not visited, discard
                                            return;
                                        }
                                        break;
                                    case DEFAULT:
                                        for (Object o : v.qualifiers()) {
                                            if (o == ResolvedKind.AUTO_CREATED) {
                                                return;
                                            }
                                        }
                                        break;
                                    default:
                                        return;
                                }
                                values.putIfAbsent(k, v);
                            }
                        });
                    }
                }
                return values;
            } catch (InvocationException ex) {
                if (!(ex.getCause() instanceof InvalidInputException)) {
                    Log.debug("Execution error: %s, inputs: %s",
                            ex.getCause().getMessage(),
                            variation);
                }
                return Map.of();
            }
        }
    }

    private static final class FileObject implements Comparable<FileObject> {
        private final String checksum;
        private final List<FileOp> ops;
        private final Expression expression;

        FileObject(String checksum, List<FileOp> ops, Expression expression) {
            this.checksum = checksum;
            this.ops = ops;
            this.expression = expression.reduce();
        }

        @Override
        public int compareTo(FileObject o) {
            int r = expression.compareTo(o.expression);
            if (r == 0) {
                r = checksum.compareTo(o.checksum);
                if (r == 0) {
                    r = Lists.compare(ops, o.ops);
                }
            }
            return r;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileObject)) {
                return false;
            }
            FileObject other = (FileObject) o;
            return Objects.equals(checksum, other.checksum)
                   && Objects.equals(ops, other.ops)
                   && Objects.equals(expression, other.expression);
        }

        @Override
        public int hashCode() {
            return Objects.hash(checksum, ops, expression);
        }
    }

    private static final class FileOps implements Comparable<FileOps> {
        private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{[^}]+}");
        private final Expression expression;
        private final List<FileOp> ops;

        FileOps(List<FileOp> ops, Expression expression) {
            this.expression = expression;
            this.ops = ops;
        }

        @Override
        public int compareTo(FileOps o) {
            int r = expression.compareTo(o.expression);
            if (r == 0) {
                r = Lists.compare(ops, o.ops);
            }
            return r;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileOps)) {
                return false;
            }
            FileOps other = (FileOps) o;
            return Objects.equals(expression, other.expression)
                   && Objects.equals(ops, other.ops);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expression, ops);
        }

        List<FileOp> resolve(String id, String path) {
            if (isFoldable()) {
                return List.of(new FileOp(Pattern.quote(id), fold(path)));
            } else {
                return Lists.addAll(ops, 0, new FileOp(Pattern.quote(id), path));
            }
        }

        String fold(String path) {
            String target = path;
            for (FileOp op : ops) {
                // escape ${}
                String replace = VAR_PATTERN.matcher(op.replacement)
                        .replaceAll(r -> Matcher.quoteReplacement(Matcher.quoteReplacement(r.group(0))));
                target = target.replaceAll(op.regex, replace);
            }
            return target;
        }

        boolean isFoldable() {
            boolean interpolated = false;
            ListIterator<FileOp> it = ops.listIterator();
            while (it.hasNext()) {
                FileOp op = it.next();
                if (op.replacement.contains("${")) {
                    if (interpolated) {
                        if (it.hasNext() || !"^(.*)$".equals(op.regex)) {
                            // only the last op can use interpolation
                            // except if the last regex is "globbing"
                            return false;
                        }
                    }
                    interpolated = true;
                }
            }
            return true;
        }

        static FileOps combine(List<FileOps> list) {
            List<FileOp> ops = new ArrayList<>();
            Expression expr = Expression.TRUE;
            for (FileOps e : list) {
                ops.addAll(e.ops);
                expr = expr.and(e.expression);
            }
            return new FileOps(ops, expr.reduce());
        }
    }

    private static final class FileOp implements Comparable<FileOp> {
        private final String regex;
        private final String replacement;

        FileOp(String regex, String replacement) {
            this.regex = regex;
            this.replacement = replacement;
        }

        @Override
        public int compareTo(FileOp o) {
            int r = replacement.compareTo(o.replacement);
            if (r == 0) {
                r = regex.compareTo(o.regex);
            }
            return r;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileOp)) {
                return false;
            }
            FileOp other = (FileOp) o;
            return Objects.equals(regex, other.regex)
                   && Objects.equals(replacement, other.replacement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regex, replacement);
        }
    }
}
