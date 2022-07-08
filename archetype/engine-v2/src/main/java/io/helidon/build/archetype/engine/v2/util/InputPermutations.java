/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import io.helidon.build.archetype.engine.v2.Controller;
import io.helidon.build.archetype.engine.v2.InputResolver;
import io.helidon.build.archetype.engine.v2.InvocationException;
import io.helidon.build.archetype.engine.v2.UnresolvedInputException;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Variable;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextScope;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.Permutations;

import static io.helidon.build.archetype.engine.v2.InputResolver.defaultValue;

/**
 * A utility to compute input permutations.
 */
public class InputPermutations {

    /**
     * Compute the input permutation for the given script.
     *
     * @param script input script
     * @return list of permutations
     */
    public static List<Map<String, String>> compute(Script script) {
        return compute(script, Map.of(), Map.of());
    }

    /**
     * Compute the input permutation for the given script.
     *
     * @param script           input script
     * @param externalValues   external values
     * @param externalDefaults external defaults
     * @return set of permutations
     */
    public static List<Map<String, String>> compute(Script script,
                                                    Map<String, String> externalValues,
                                                    Map<String, String> externalDefaults) {

        Path cwd = script.scriptPath().getParent();

        // Make a single pass to capture all variations
        // basically a map of each input (by absolute path) and their possible values
        VisitorImpl visitor = new VisitorImpl(Context.builder().cwd(cwd).build());
        Walker.walk(visitor, script, null, visitor.context::cwd);

        // convert each variation into a separate map, each map is single element that can be permuted
        // this is the input to our permutation algorithm (List<List<T>>)
        List<List<Map<String, InputValue>>> elements =
                Lists.map(visitor.variations.entrySet(), e -> Lists.map(e.getValue(), v -> Map.of(e.getKey(), v)));

        // TODO configuration for max and log a warning when greater
        int permSize = elements.stream().map(List::size).reduce(1, Math::multiplyExact);

        Set<TreeMap<String, String>> result = new LinkedHashSet<>();
        Set<Map<String, InputValue>> set = new LinkedHashSet<>();

        Iterator<List<Map<String, InputValue>>> it = new Permutations.ListIterator<>(elements);
        for (int i=0; it.hasNext() && result.size() <= 200; i++) {

            // unfiltered permutation that was captured regardless of the control flow
            Permutation unfiltered = new Permutation(it.next());

            // update permutation, i.e. remove incoherent values
            Map<String, InputValue> permutation2 = unfiltered.update(visitor.inputs);
            if (!set.add(permutation2)) {
                // skip duplicates
                continue;
            }

            try {
                System.out.println(i + "/" + permSize);

                // for each permutation, perform a normal execution
                // and use the resulting context values as permutation
                Context context = Context.builder().cwd(cwd).build();
                Controller.walk(new InputResolverImpl(permutation2), script, context);
                Map<String, String> contextValues = Maps.mapValue(context.scope().values(),
                        (k, v) -> v.kind() == ValueKind.USER, Value::asText);
                if (!contextValues.isEmpty()) {
                    result.add(new TreeMap<>(contextValues));
                }
            } catch (InvocationException ignored) {
                // invalid option
            }
        }

        // sort the result
        List<Map<String, String>> list = new LinkedList<>(result);
        list.sort(Comparator.comparing(m -> m.entrySet().iterator().next().toString()));
        return list;
    }

    private static final class Permutation {

        final Map<String, InputValue> permutation;

        Permutation(List<Map<String, InputValue>> permutation) {
            this.permutation = Maps.fromEntries(Lists.flatMap(permutation, Map::entrySet));
        }

        Map<String, InputValue> update(Map<DeclaredInput, String> inputs) {
            Map<String, InputValue> result = new HashMap<>();
            for (Map.Entry<String, InputValue> entry : permutation.entrySet()) {
                InputValue inputValue = entry.getValue();
                if (inputValue.validate(this, inputs)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
            return result;
        }
    }

    private static final class InputValue {

        final List<Input> parents;
        final Value value;

        InputValue(List<Input> parents, Value value) {
            this.parents = Objects.requireNonNull(parents, "parents is null");
            this.value = Objects.requireNonNull(value, "value is null");
        }

        boolean validate(Input input, InputValue inputValue, String option) {
            if (input instanceof Input.Boolean) {
                return inputValue.value.asBoolean();
            } else if (input instanceof Input.Options) {
                if (option == null) {
                    throw new IllegalStateException("Options without option: " + input);
                }
                String value = inputValue.value.asText();
                if (option.contains("$") || value.contains("$")) {
                    return true;
                }
                if (input instanceof Input.Enum) {
                    return option.equals(value);
                } else if (input instanceof Input.List) {
                    return inputValue.value.asList().contains(option);
                }
            }
            return true;
        }

        boolean validate(Permutation permutation, Map<DeclaredInput, String> inputs) {
            String option = null;
            for (Input input0 : parents) {
                if (input0 instanceof DeclaredInput) {
                    DeclaredInput input = (DeclaredInput) input0;
                    InputValue parentValue =
                            Optional.ofNullable(inputs.get(input))
                                    .map(permutation.permutation::get)
                                    .orElseThrow(() -> new IllegalStateException(
                                            "Input value not found for parent: " + input0));

                    if (!validate(input0, parentValue, option)) {
                        return false;
                    }
                    option = null;
                } else if (input0 instanceof Input.Option) {
                    option = ((Input.Option) input0).value();
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InputValue inputValue = (InputValue) o;
            return value.equals(inputValue.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    private static final class InputResolverImpl extends InputResolver {

        final Map<String, InputValue> permutation;

        InputResolverImpl(Map<String, InputValue> permutation) {
            this.permutation = permutation;
        }

        @Override
        public VisitResult visitAny(Input input, Context context) {
            if (input instanceof DeclaredInput) {
                return visit((DeclaredInput) input, context);
            }
            return VisitResult.CONTINUE;
        }

        private VisitResult visit(DeclaredInput input, Context context) {
            ContextScope nextScope = context.scope().getOrCreate(input.id(), input.isGlobal());
            VisitResult result = onVisitInput(input, nextScope, context);
            if (result == null) {
                String path = nextScope.path(true);
                InputValue inputValue = permutation.get(path);
                if (inputValue == null) {
                    throw new UnresolvedInputException(path);
                }
                String rawValue = inputValue.value.asText();
                Value value = DynamicValue.create(context.interpolate(rawValue));
                if (input instanceof Input.Options) {
                    Input.Options optionsBlock = (Input.Options) input;
                    List<Input.Option> optionNodes = optionsBlock.options(n -> Condition.filter(n, context::getValue));
                    List<String> optionValues = Lists.map(optionNodes, o -> context.interpolate(o.value()));
                    if (input instanceof Input.List) {
                        for (String opt : value.asList()) {
                            if (!optionValues.contains(opt)) {
                                throw new IllegalArgumentException(String.format(
                                        "Invalid option value: %s, permutation: %s", opt, permutation));
                            }
                        }
                    } else if (input instanceof Input.Enum) {
                        String opt = value.asString();
                        if (!optionValues.contains(opt)) {
                            throw new IllegalArgumentException(String.format(
                                    "Invalid option value: %s, permutation: %s", opt, permutation));
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
    }

    private static final class VisitorImpl implements Node.Visitor<Void>, Block.Visitor<Void> {

        final Deque<Input> parents = new ArrayDeque<>();
        final Map<String, Set<InputValue>> variations = new HashMap<>();
        final Map<DeclaredInput, String> inputs = new HashMap<>();
        final Context context;

        VisitorImpl(Context context) {
            this.context = context;
        }

        @Override
        public VisitResult visitVariable(Variable variable, Void arg) {
            context.putValue(variable.path(), variable.value(), ValueKind.LOCAL_VAR);
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult visitPreset(Preset preset, Void arg) {
            context.putValue(preset.path(), preset.value(), ValueKind.PRESET);
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult visitBlock(Block block, Void arg) {
            if (block.kind() == Block.Kind.OUTPUT) {
                return VisitResult.SKIP_SUBTREE;
            }
            if (block.kind() == Block.Kind.INVOKE_DIR) {
                context.pushCwd(block.scriptPath().getParent());
                return VisitResult.CONTINUE;
            }
            return block.accept((Block.Visitor<Void>) this, arg);
        }

        @Override
        public VisitResult postVisitBlock(Block block, Void arg) {
            if (block.kind() == Block.Kind.INVOKE_DIR) {
                context.popCwd();
                return VisitResult.CONTINUE;
            }
            return block.acceptAfter((Block.Visitor<Void>) this, null);
        }

        @Override
        public VisitResult visitInput(Input input0, Void arg) {
            if (input0 instanceof DeclaredInput) {
                DeclaredInput input = (DeclaredInput) input0;
                ContextScope scope = context.scope().getOrCreate(input.id(), input.isGlobal());
                String path = scope.path(true);
                inputs.put(input, path);
                variations.computeIfAbsent(path, p -> new HashSet<>())
                          .addAll(Lists.map(variations(input), v -> new InputValue(Lists.of(parents), v)));
                context.pushScope(scope);
            }
            parents.push(input0);
            return VisitResult.CONTINUE;
        }

        private List<Value> variations(DeclaredInput input) {
            if (input instanceof Input.Boolean) {
                return Lists.of(Value.FALSE, Value.TRUE);
            } else if (input instanceof Input.Options) {
                Input.Options optionsBlock = (Input.Options) input;
                List<String> optionsValues = Lists.map(optionsBlock.options(n -> true), Input.Option::value);
                if (input instanceof Input.Enum) {
                    return Lists.map(optionsValues, Value::create);
                } else if (input instanceof Input.List) {
                    return Lists.map(Permutations.of(optionsValues), Value::create);
                }
            } else if (input instanceof Input.Text) {
                Value defaultValue = defaultValue(input, context);
                return Lists.of(Optional.ofNullable(defaultValue)
                                        .map(Value::asString)
                                        .map(Value::create)
                                        // TODO configuration for text values
                                        .orElseGet(() -> Value.create("xxx")));
            }
            throw new IllegalArgumentException("Unsupported input: " + input);
        }

        @Override
        public VisitResult postVisitInput(Input input, Void arg) {
            if (input instanceof DeclaredInput) {
                context.popScope();
            }
            parents.pop();
            return VisitResult.CONTINUE;
        }
    }
}
