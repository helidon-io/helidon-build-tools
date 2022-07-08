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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

        // Make a single pass to capture all permutations
        VisitorImpl visitor = new VisitorImpl(Context.builder().cwd(cwd).build());
        Walker.walk(visitor, script, null, visitor.context::cwd);

        // for each permutation, perform a normal execution
        // and use the resulting context values as permutation
        List<Map<String, String>> permutations = visitor.permutations.pop();
        Set<TreeMap<String, String>> result = new LinkedHashSet<>();
        permutations.forEach(permutation -> {
            try {
                Context context = Context.builder().cwd(cwd).build();
                Controller.walk(new InputResolverImpl(permutation), script, context);
                Map<String, String> contextValues = Maps.mapValue(context.scope().values(),
                        (k, v) -> v.kind() == ValueKind.USER, Value::asText);
                if (!contextValues.isEmpty()) {
                    result.add(new TreeMap<>(contextValues));
                }
            } catch (InvocationException ignored) {
                // invalid option
            }
        });

        // sort the result
        List<Map<String, String>> list = new LinkedList<>(result);
        list.sort(Comparator.comparing(m -> m.entrySet().iterator().next().toString()));
        return list;
    }

    private static final class InputResolverImpl extends InputResolver {

        final Map<String, String> permutation;

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

        private VisitResult visit(DeclaredInput input, Context context) {
            ContextScope nextScope = context.scope().getOrCreate(input.id(), input.isGlobal());
            VisitResult result = onVisitInput(input, nextScope, context);
            if (result == null) {
                String path = nextScope.path();
                String rawValue = permutation.get(path);
                if (rawValue == null) {
                    throw new UnresolvedInputException(path);
                }
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

        // TODO use a unique deque as List<List<Map<String, String>
        final Deque<List<List<Map<String, String>>>> parents = new ArrayDeque<>();
        final Deque<List<Map<String, String>>> permutations = new ArrayDeque<>();
        final Context context;

        VisitorImpl(Context context) {
            this.context = context;
            permutations.push(Lists.of());
            parents.push(Lists.of());
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
            switch (block.kind()) {
                case OUTPUT:
                    return VisitResult.SKIP_SUBTREE;
                case INVOKE_DIR:
                    context.pushCwd(block.scriptPath().getParent());
                    return VisitResult.CONTINUE;
                case INPUTS:
                    parents.push(Lists.of());
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
                case INPUTS:
                    List<Map<String, String>> parent = permutations.peek();
                    if (parent == null) {
                        throw new IllegalStateException("Unable to add permutation");
                    }
                    List<List<Map<String, String>>> perms = parents.pop();
                    List<List<Map<String, String>>> computed = Permutations.ofList(perms);
                    List<Map<String, String>> reduced = Lists.map(computed, Maps::merge);
                    parent.addAll(reduced);
                    return VisitResult.CONTINUE;
                default:
                    return block.acceptAfter((Block.Visitor<Void>) this, null);
            }
        }

        @Override
        public VisitResult visitInput(Input input0, Void arg) {
            permutations.push(Lists.of());
            if (input0 instanceof DeclaredInput) {
                DeclaredInput input = (DeclaredInput) input0;
                context.pushScope(input.id(), input.isGlobal());
            }
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult postVisitInput(Input input, Void arg) {
            List<Map<String, String>> perms = permutations.pop();
            if (input instanceof DeclaredInput) {
                List<List<Map<String, String>>> parent = parents.peek();
                if (parent == null) {
                    throw new IllegalStateException("Unable to add permutation");
                }
                String path = context.scope().path();
                context.popScope();
                if (input instanceof Input.Boolean) {
                    if (perms.isEmpty()) {
                        perms.add(Maps.of(path, "true"));
                    } else {
                        perms.forEach(p -> p.put(path, "true"));
                    }
                    perms.add(Maps.of(path, "false"));
                    parent.add(perms);
                } else if (input instanceof Input.Options) {
                    if (input instanceof Input.List) {
                        // permutation grouped by options
                        List<List<Map<String, String>>> options = Lists.groupingBy(perms, p -> p.get(path));
                        // generate permutations for the groups
                        List<List<List<Map<String, String>>>> values = Permutations.of(options);
                        List<List<Map<String, String>>> computed = Lists.map(values, l -> {
                            switch (l.size()) {
                                case 0:
                                    // make sure the empty permutation has an entry
                                    return Lists.of(Maps.of(path, ""));
                                case 1:
                                    // single option
                                    return l.get(0);
                                default:
                                    // multiple options, requires merging the maps
                                    List<List<Map<String, String>>> comp = Permutations.ofList(l);
                                    return Lists.map(comp, l2 -> Maps.merge(l2, (v1, v2) -> v1 + "," + v2));
                            }
                        });
                        // reduce under a single list to avoid re-computing at the parent level
                        parent.addAll(Lists.of(Lists.flatMap(computed)));
                    } else if (input instanceof Input.Enum) {
                        parent.add(perms);
                    }
                } else if (input instanceof Input.Text) {
                    // TODO configuration for text values
                    Value defaultValue = defaultValue((Input.Text) input, context);
                    String value = Optional.ofNullable(defaultValue)
                                           .map(Value::asString)
                                           .orElse("xxx");
                    if (perms.isEmpty()) {
                        perms.add(Maps.of(path, value));
                    } else {
                        perms.forEach(p -> p.put(path, value));
                    }
                    parent.add(perms);
                }
            } else if (input instanceof Input.Option) {
                List<Map<String, String>> parent = permutations.peek();
                if (parent == null) {
                    throw new IllegalStateException("Unable to add permutation");
                }
                Input.Option optionBlock = (Input.Option) input;
                String path = context.scope().path();
                if (perms.isEmpty()) {
                    perms.add(Maps.of(path, optionBlock.value()));
                } else {
                    perms.forEach(p -> p.put(path, optionBlock.value()));
                }
                parent.addAll(perms);
            }
            return VisitResult.CONTINUE;
        }
    }
}
