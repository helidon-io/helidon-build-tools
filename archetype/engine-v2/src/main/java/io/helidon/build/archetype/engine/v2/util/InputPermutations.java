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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.Context;
import io.helidon.build.archetype.engine.v2.ContextScope;
import io.helidon.build.archetype.engine.v2.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput;
import io.helidon.build.archetype.engine.v2.ast.Input.Option;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.Variable;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.Permutations;

import static io.helidon.build.archetype.engine.v2.InputResolver.defaultValue;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A utility to compute input permutations.
 */
public class InputPermutations implements Node.Visitor<Void>, Block.Visitor<Void> {

    private final Context context;
    private final Deque<List<Map<String, String>>> stack = new ArrayDeque<>();
    private final Deque<DeferredCondition> conditions = new ArrayDeque<>();

    private InputPermutations(Map<String, String> externalValues, Map<String, String> externalDefaults) {
        this.context = Context.create(null, externalValues, externalDefaults);
    }

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
     * @return list of permutations
     */
    public static List<Map<String, String>> compute(Script script,
                                                    Map<String, String> externalValues,
                                                    Map<String, String> externalDefaults) {

        InputPermutations visitor = new InputPermutations(externalValues, externalDefaults);
        Walker.walk(visitor, script, null);
        return visitor.stack.peek();
    }

    @Override
    public VisitResult visitVariable(Variable variable, Void arg) {
        context.scope().putValue(variable.path(), variable.value(), ValueKind.LOCAL_VAR);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitPreset(Preset preset, Void arg) {
        context.scope().putValue(preset.path(), preset.value(), ValueKind.PRESET);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitCondition(Condition condition, Void arg) {
        conditions.push(new DeferredCondition(condition, context.scope()));
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitBlock(Block block, Void arg) {
        if (block.kind() == Block.Kind.OUTPUT) {
            return VisitResult.SKIP_SUBTREE;
        }
        stack.push(new ArrayList<>());
        return block.accept((Block.Visitor<Void>) this, arg);
    }

    @Override
    public VisitResult postVisitBlock(Block block, Void arg) {
        return block.acceptAfter((Block.Visitor<Void>) this, null);
    }

    @Override
    public VisitResult visitInput(Input input0, Void arg) {
        if (input0 instanceof DeclaredInput) {
            DeclaredInput input = (DeclaredInput) input0;
            ContextScope scope = context.scope().getOrCreateScope("." + input.id(), input.isGlobal());
            context.pushScope(scope);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitInput(Input input, Void arg) {
        ContextScope scope = context.scope();
        String id = scope.path();
        List<Map<String, String>> permutations = requireNonNull(stack.pop());
        if (input instanceof Option) {
            String rawValue = ((Option) input).value();
            String value = scope.parent().interpolate(rawValue);
            stack.push(Maps.putIfAbsent(permutations, id, value));
        } else if (input instanceof DeclaredInput) {
            context.popScope();
            if (input instanceof Input.Boolean) {
                stack.push(Lists.addAll(
                        Maps.putAll(permutations, Maps.of(id, "true")),
                        Lists.of(Maps.of(id, "false"))));
            } else if (input instanceof Input.Options) {
                Map<String, List<Map<String, String>>> nested = Maps.keyedBy(permutations, id);
                if (input instanceof Input.Enum) {
                    stack.push(enumPermutations((Input.Enum) input, id, nested));
                } else if (input instanceof Input.List) {
                    stack.push(listPermutations((Input.List) input, id, nested));
                }
            } else if (input instanceof Input.Text) {
                Value defaultValue = defaultValue((Input.Text) input, context);
                if (defaultValue != null) {
                    String value = defaultValue.asString();
                    stack.push(Maps.putAll(permutations, Maps.of(id, value != null ? value : "xxx")));
                } else {
                    // add configuration for a map to supply data for text inputs
                    stack.push(Lists.of(Maps.of(id, "xxx")));
                }
            }
        }
        return postVisitAny(input, arg);
    }

    @Override
    public VisitResult postVisitAny(Block block, Void arg) {
        if (stack.size() > 1) {
            // add to parent
            List<Map<String, String>> popped = stack.pop();
            requireNonNull(stack.peek()).addAll(popped);
        }
        return postVisitAny((Node) block, arg);
    }

    @Override
    public VisitResult postVisitAny(Node node, Void arg) {
        if (!conditions.isEmpty()) {
            DeferredCondition deferredCondition = conditions.peek();
            if (node.equals(deferredCondition.condition.then())) {
                conditions.pop();
            }
        }
        return VisitResult.CONTINUE;
    }

    private List<Map<String, String>> enumPermutations(Input.Enum input,
                                                       String id,
                                                       Map<String, List<Map<String, String>>> nested) {

        return Lists.flatMap(options(input), o -> Lists.map(nested.get(o), m -> {
            Map<String, String> entry = Maps.of(id, o);
            return Maps.putAll(m, entry);
        }));
    }

    private List<Map<String, String>> listPermutations(Input.List input,
                                                       String id,
                                                       Map<String, List<Map<String, String>>> nested) {

        List<String> options = options(input);

        // permutations of the input options
        List<List<String>> optionPermutations = Permutations.of(options);
        return Lists.flatMap(optionPermutations, op -> {

            // entry to record the current options
            Map<String, String> entry = Maps.of(id, String.join(" ", op));

            if (op.isEmpty()) {
                return Lists.of(entry);
            }

            // list of nested permutations for the current options
            List<List<Map<String, String>>> nestedPermutations = Lists.map(op, nested::get);

            // effective permutations for the current options
            // i.e. permutations of the nested permutations
            List<List<Map<String, String>>> computed = Permutations.ofList(nestedPermutations);

            // merge the individual maps
            List<Map<String, String>> permutations = Lists.map(computed, Maps::putAll);

            // add the current options to all computed permutations
            return Maps.putAll(permutations, entry);
        });
    }

    private List<String> options(DeclaredInput node) {
        return node.children(n -> true, Option.class)
                   .map(Option::value)
                   .map(context.scope()::interpolate)
                   .collect(toList());
    }

    private static final class DeferredCondition {

        final Condition condition;
        final ContextScope scope;

        DeferredCondition(Condition condition, ContextScope scope) {
            this.condition = condition;
            this.scope = scope;
        }
    }
}
