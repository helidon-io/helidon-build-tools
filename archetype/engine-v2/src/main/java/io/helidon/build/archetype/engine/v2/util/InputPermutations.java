/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2.util;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;

import io.helidon.build.archetype.engine.v2.Controller;
import io.helidon.build.archetype.engine.v2.InputResolver;
import io.helidon.build.archetype.engine.v2.InvocationException;
import io.helidon.build.archetype.engine.v2.UnresolvedInputException;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Expression;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.ast.Variable;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextScope;
import io.helidon.build.archetype.engine.v2.context.ContextValue;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.Permutations;
import io.helidon.build.common.logging.Log;

import static io.helidon.build.archetype.engine.v2.ast.Input.Enum.optionIndex;

/**
 * A utility to compute input permutations.
 */
public class InputPermutations {

    private static final Random RANDOM = new Random();

    private final Script script;
    private final Path cwd;
    private final Map<String, String> externalValues;
    private final Map<String, String> externalDefaults;
    private final List<Expression> inputFilters;
    private final List<Expression> permutationFilters;

    private InputPermutations(Builder builder) {
        script = Objects.requireNonNull(builder.script, "script is null!");
        cwd = script.scriptPath().getParent();
        externalValues = builder.externalValues;
        externalDefaults = builder.externalDefaults;
        inputFilters = builder.inputFilters;
        permutationFilters = builder.permutationFilters;
    }

    /**
     * Compute the input permutations.
     *
     * @return list of permutations
     */
    public List<Map<String, String>> compute() {

        // Make a single pass to capture all permutations
        VisitorImpl visitor = new VisitorImpl(Context.builder().cwd(cwd).build());
        Walker.walk(visitor, script, null, visitor.context::cwd);

        // for each permutation, perform a normal execution
        // and use the resulting context values as permutation
        List<Map<String, String>> permutations = Lists.flatMap(visitor.stack.pop());
        Map<String, List<TreeMap<String, ContextValue>>> result0 = new LinkedHashMap<>();
        for (Map<String, String> permutation : permutations) {
            if (!filter(permutationFilters, permutation)) {
                continue;
            }
            Map<String, ContextValue> contextValues = execute(permutation);
            if (contextValues != null && !contextValues.isEmpty()) {
                Map<String, ContextValue> filteredValues = Maps.mapValue(contextValues,
                        (k, v) -> {
                            switch (v.kind()) {
                                case USER:
                                case DEFAULT:
                                    return true;
                                default:
                                    return false;
                            }
                        }, v -> v);
                if (!filteredValues.isEmpty()) {
                    Map<String, String> effectivePermutation = Maps.mapValue(filteredValues, Value::asText);
                    if (filter(permutationFilters, effectivePermutation)) {
                        result0.computeIfAbsent(effectivePermutation.toString(), k -> new ArrayList<>())
                               .add(new TreeMap<>(filteredValues));
                    }
                }
            }
        }

        // filter implicit duplicates (default values)
        List<Map<String, String>> result1 = Lists.map(result0.values(), l -> {
            List<Map<String, String>> duplicates = Lists.map(l, p -> Maps.mapValue(p,
                    (k, v) -> v.kind() == ValueKind.USER, Value::asText));
            Map<String, String> chosen = null;
            for (Map<String, String> p : duplicates) {
                if (chosen == null) {
                    chosen = p;
                    continue;
                }
                if (p.size() > chosen.size()) {
                    chosen = p;
                }
            }
            return chosen;
        });

        // sort the result
        List<Map<String, String>> list = new LinkedList<>(result1);
        list.sort(Comparator.comparing(m -> m.entrySet().iterator().next().toString()));
        return list;
    }

    private Map<String, ContextValue> execute(Map<String, String> permutation) {
        Context context = Context.builder().cwd(cwd).build();
        try {
            Controller.walk(new InputResolverImpl(permutation), script, context);
            return context.scope().values();
        } catch (InvocationException ex) {
            if (!(ex.getCause() instanceof InvalidOption)) {
                Log.warn(ex, "Permutation error: %s, permutation: %s",
                        ex.getCause().getMessage(),
                        permutation);
            }
        }
        return null;
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link InputPermutations} builder.
     */
    public static final class Builder {

        private Script script;
        private final Map<String, String> externalValues = new HashMap<>();
        private final Map<String, String> externalDefaults = new HashMap<>();
        private final List<Expression> inputFilters = new ArrayList<>();
        private final List<Expression> permutationFilters = new ArrayList<>();

        /**
         * Set the script.
         *
         * @param script script
         * @return this builder
         */
        public Builder script(Script script) {
            this.script = script;
            return this;
        }

        /**
         * Set the external values.
         *
         * @param externalValues external values
         * @return this builder
         */
        public Builder externalValues(Map<String, String> externalValues) {
            if (externalValues != null) {
                this.externalValues.putAll(externalValues);
            }
            return this;
        }

        /**
         * Set the external defaults.
         *
         * @param externalDefaults external values
         * @return this builder
         */
        public Builder externalDefaults(Map<String, String> externalDefaults) {
            if (externalDefaults != null) {
                this.externalDefaults.putAll(externalDefaults);
            }
            return this;
        }

        /**
         * Set the input filters.
         *
         * @param filters filters
         * @return this builder
         */
        public Builder inputFilters(Collection<String> filters) {
            if (filters != null) {
                this.inputFilters.addAll(Lists.map(filters, Expression::create));
            }
            return this;
        }

        /**
         * Set the permutation filters.
         *
         * @param filters filters
         * @return this builder
         */
        public Builder permutationFilters(Collection<String> filters) {
            if (filters != null) {
                this.permutationFilters.addAll(Lists.map(filters, Expression::create));
            }
            return this;
        }

        /**
         * Build the instance.
         *
         * @return new instance
         */
        public InputPermutations build() {
            return new InputPermutations(this);
        }
    }

    private static boolean filter(List<Expression> filters, Map<String, String> permutation) {
        for (Expression filter : filters) {
            try {
                boolean result = filter.eval(s -> {
                    String v = permutation.get(s);
                    if (v != null) {
                        return DynamicValue.create(v);
                    }
                    return null;
                }).asBoolean();
                if (!result) {
                    return false;
                }
            } catch (Expression.UnresolvedVariableException ignore) {
            }
        }
        return true;
    }

    private final class InputResolverImpl extends InputResolver {

        private final Map<String, String> permutation;

        InputResolverImpl(Map<String, String> permutation) {
            this.permutation = permutation;
        }

        @Override
        public VisitResult visitAny(Input input, Context context) {
            if (input instanceof DeclaredInput) {
                return visit((DeclaredInput) input, context);
            }
            return VisitResult.CONTINUE;
        }

        @Override
        protected VisitResult onVisitInput(DeclaredInput input, ContextScope scope, Context context) {
            String rawValue = externalValues.get(scope.path());
            if (rawValue != null) {
                context.putValue(input.id(), DynamicValue.create(rawValue), ValueKind.USER);
            }
            return super.onVisitInput(input, scope, context);
        }

        private VisitResult visit(DeclaredInput input, Context context) {
            ContextScope nextScope = context.scope().getOrCreate(input.id(), input.isModel(), input.isGlobal());
            VisitResult result = onVisitInput(input, nextScope, context);
            if (result == null) {
                String path = nextScope.path();
                String rawValue = permutation.get(path);
                if (rawValue == null) {
                    Value defaultValue = defaultValue(input, nextScope, context);
                    if (defaultValue == null) {
                        throw new UnresolvedInputException(path);
                    }
                    context.putValue(input.id(), defaultValue, ValueKind.DEFAULT);
                    context.pushScope(nextScope);
                    return input.visitValue(defaultValue);
                }
                Value value = DynamicValue.create(context.interpolate(rawValue));
                if (input instanceof Input.Options) {
                    Input.Options optionsBlock = (Input.Options) input;
                    List<Input.Option> optionNodes = optionsBlock.options(n -> Condition.filter(n, context::getValue));
                    List<String> optionValues = Lists.map(optionNodes, o -> context.interpolate(o.value()));
                    if (input instanceof Input.List) {
                        validateValue(optionValues, value);
                    } else if (input instanceof Input.Enum) {
                        String opt = value.asString();
                        if (!optionValues.contains(opt)) {
                            throw new InvalidOption(opt, permutation);
                        }
                    }
                    result = VisitResult.CONTINUE;
                } else {
                    result = input.visitValue(value);
                }
                context.putValue(input.id(), value, ValueKind.USER);
            }
            context.pushScope(nextScope);
            return result;
        }

        private void validateValue(List<String> optionValues, Value value) {
            if (!"none".equals(value.asText())) {
                for (String opt : value.asList()) {
                    if (!optionValues.contains(opt)) {
                        throw new InvalidOption(opt, permutation);
                    }
                }
            }
        }
    }

    private final class VisitorImpl implements Node.Visitor<Void>, Block.Visitor<Void> {

        private final Deque<List<List<Map<String, String>>>> stack = new ArrayDeque<>();
        private final Context context;

        VisitorImpl(Context context) {
            this.context = context;
            stack.push(Lists.of());
        }

        @Override
        public VisitResult visitPreset(Preset preset, Void arg) {
            // Use local var instead of preset to allow overrides
            context.putValue(preset.path(), preset.value(), preset.isModel(), ValueKind.LOCAL_VAR);
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult visitVariable(Variable variable, Void arg) {
            context.putValue(variable.path(), variable.value(), variable.isModel(), ValueKind.LOCAL_VAR);
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult visitBlock(Block block, Void arg) {
            switch (block.kind()) {
                case OUTPUT:
                    return VisitResult.SKIP_SUBTREE;
                case INVOKE_DIR:
                    context.pushCwd(block.scriptPath().getParent());
                    return VisitResult.CONTINUE;
                case SCRIPT:
                case STEP:
                case INPUTS:
                    stack.push(Lists.of());
                    return VisitResult.CONTINUE;
                default:
                    return block.accept((Block.Visitor<Void>) this, arg);
            }
        }

        @Override
        public VisitResult postVisitBlock(Block block, Void arg) {
            switch (block.kind()) {
                case INVOKE_DIR:
                    context.popCwd();
                    return VisitResult.CONTINUE;
                case SCRIPT:
                case STEP:
                case INPUTS:
                    // nested permutations
                    List<List<Map<String, String>>> perms = stack.pop();

                    int permSize = perms.stream().map(List::size).reduce(1, (a, b) -> a * b);
                    if (permSize > 150000) {
                        Log.warn("Too many permutations: %s, block: %s", permSize, block.location());
                    }

                    // compute permutations
                    List<List<Map<String, String>>> computed = compute(perms);

                    // reduce and merge to avoid computing again at the parent level
                    List<Map<String, String>> merged = Lists.map(computed, Maps::merge);

                    // filtered
                    List<Map<String, String>> filtered = filterPermutations(merged);

                    addPermutations(filtered);
                    return VisitResult.CONTINUE;
                default:
                    return block.acceptAfter((Block.Visitor<Void>) this, null);
            }
        }

        List<List<Map<String, String>>> compute(List<List<Map<String, String>>> perms) {
            List<List<Map<String, String>>> computed = new LinkedList<>();
            Iterator<List<Map<String, String>>> it = new Permutations.ListIterator<>(perms);
            while (it.hasNext()) {
                List<Map<String, String>> next = it.next();
                computed.add(next);
            }
            return computed;
        }

        @Override
        public VisitResult visitInput(Input input0, Void arg) {
            stack.push(Lists.of());
            if (input0 instanceof DeclaredInput) {
                DeclaredInput input = (DeclaredInput) input0;
                context.pushScope(input.id(), input.isModel(), input.isGlobal());
                if (input instanceof Input.Enum) {
                    Input.Enum enumInput = (Input.Enum) input;
                    List<Input.Option> options = enumInput.options();
                    int defaultIndex = optionIndex(enumInput.defaultValue().asString(), options);
                    // skip if there is only one option with a default value
                    if (options.size() == 1 && defaultIndex >= 0) {
                        return VisitResult.SKIP_SUBTREE;
                    }
                }
                ContextValue value = context.getValue("");
                if (value != null) {
                    // control the flow if there is a value
                    // we only maintain values for variables and presets
                    return input.visitValue(value);
                }
            } else if (input0 instanceof Input.Option) {
                // clear all the nested values from a previous pass
                context.scope().clear();
            }
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult postVisitInput(Input input, Void arg) {
            // compute permutations for the input
            List<List<Map<String, String>>> computed = Lists.filter(compute(input), l -> !l.isEmpty());

            // parent level computes permutations for the first dimension
            // we reduce to avoid computing again at the parent level
            // e.g.
            // computed: [[{colors=red}], [{colors=orange}]]
            // reduced: [[{colors=red}, {colors=orange}]]
            List<Map<String, String>> reduced = Lists.flatMap(computed);

            List<Map<String, String>> filtered;
            if (input instanceof DeclaredInput) {
                filtered = filterPermutations(reduced);
            } else {
                filtered = reduced;
            }
            addPermutations(filtered);
            return VisitResult.CONTINUE;
        }

        private List<List<Map<String, String>>> compute(Input input0) {
            // nested permutations
            // each element of the list represents the permutations for a child
            List<List<Map<String, String>>> perms = Lists.filter(stack.pop(), l -> !l.isEmpty());
            String path = context.scope().path();
            if (input0 instanceof DeclaredInput) {
                DeclaredInput input = (DeclaredInput) input0;
                ContextValue value = context.getValue("");
                context.popScope();
                if (value != null && input.visitValue(value) != VisitResult.CONTINUE) {
                    return perms;
                }
                switch (input0.kind()) {
                    case BOOLEAN:
                        // add true to all nested permutations and a new permutation for false
                        // e.g.
                        // nested:   [[{colors=red}], [{colors=orange}]]
                        // computed: [[{colors=red, path=true}, [{colors=orange, path=true}], [{path=false}]]
                        putValue(perms, path, "true");
                        perms.add(Lists.of(Maps.of(path, "false")));
                        return perms;
                    case LIST:
                        // the nested permutations represent the permutations for each option
                        // compute permutations for the options
                        // e.g.
                        // options: [a, b]
                        // computed: [[], [a], [b], [a,b]]
                        return Lists.map(Permutations.of(perms), option -> {
                            switch (option.size()) {
                                case 0:
                                    // empty permutation needs an entry
                                    return Lists.of(Maps.of(path, "none"));
                                case 1:
                                    // single permutation (single option)
                                    return option.get(0);
                                default:
                                    // multiple options
                                    // e.g.
                                    //
                                    // options:  [[{colors=red,    red=burgundy},  {colors=red,    red=auburn}],
                                    //            [{colors=orange, orange=salmon}, {colors=orange, orange=peach}]]
                                    //
                                    // computed: [[{colors=red, red=burgundy}, {colors=orange, orange=salmon}],
                                    //            [{colors=red, red=auburn},   {colors=orange, orange=salmon}],
                                    //            [{colors=red, red=burgundy}, {colors=orange, orange=peach}],
                                    //            [{colors=red, red=auburn},   {colors=orange, orange=peach}]]
                                    //
                                    // merged:   [{colors='red,orange', red=burgundy, orange=salmon},
                                    //            {colors='red,orange', red=burgundy, orange=peach},
                                    //            {colors='red,orange', red=auburn,   orange=peach},
                                    //            {colors='red,orange', red=auburn,   orange=peach}]
                                    List<List<Map<String, String>>> comp = compute(option);
                                    return Lists.map(comp, l -> Maps.merge(l, (v1, v2) -> v1 + "," + v2));
                            }
                        });
                    case TEXT:
                        String inputValue = Optional.ofNullable(externalValues.get(path))
                                                    .or(() -> Optional.ofNullable(externalDefaults.get(path)))
                                                    .or(() -> Optional.ofNullable(input.defaultValue())
                                                                      .filter(v -> v.type() == ValueTypes.STRING)
                                                                      .map(Value::asString))
                                                    .map(Value::create)
                                                    .map(Value::asString)
                                                    .orElse(input.id() + "-" + RANDOM.nextInt());

                        // add the text value to all nested permutations
                        // e.g.
                        // nested:   [[{colors=red}], [{colors=orange}]]
                        // computed: [[{colors=red, path=xxx}, [{colors=orange, path=xxx}]]
                        putValue(perms, path, inputValue);
                        return perms;
                    default:
                        // enum does not need computing (one-of)
                        return perms;
                }
            } else if (input0 instanceof Input.Option) {
                // add the option value to all nested permutations
                // nested: [[{colors=red}], [{colors=orange}]]
                // computed: [[{colors=red, path=option}, [{colors=orange, path=option}]]
                putValue(perms, path, ((Input.Option) input0).value());
                return perms;
            }
            throw new IllegalArgumentException("Unsupported input: " + input0);
        }

        private void addPermutations(List<Map<String, String>> perms) {
            List<List<Map<String, String>>> parent = stack.peek();
            if (parent == null) {
                throw new IllegalStateException("Unable to add permutations");
            }
            parent.add(perms);
        }

        private List<Map<String, String>> filterPermutations(List<Map<String, String>> perms) {
            if (!perms.isEmpty()) {
                return Lists.filter(perms, m -> filter(inputFilters, m));
            }
            return perms;
        }

        private void putValue(List<List<Map<String, String>>> perms, String path, String value) {
            if (perms.isEmpty()) {
                perms.add(Lists.of(Maps.of(path, value)));
            } else {
                perms.forEach(l -> {
                    if (l.isEmpty()) {
                        l.add(Maps.of(path, value));
                    } else {
                        l.forEach(p -> p.put(path, value));
                    }
                });
            }
        }
    }

    private static final class InvalidOption extends RuntimeException {
        InvalidOption(String option, Map<String, String> permutation) {
            super(String.format("Invalid option value: %s, permutation: %s", option, permutation));
        }
    }
}
