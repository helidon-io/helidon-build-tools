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
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextNode;
import io.helidon.build.archetype.engine.v2.context.ContextPrinter;
import io.helidon.build.archetype.engine.v2.context.ContextScope;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.context.CopyOnWriteContextEdge;
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
import io.helidon.build.common.Permutations;

import static io.helidon.build.archetype.engine.v2.InputResolver.defaultValue;
import static java.util.stream.Collectors.toList;

/**
 * A utility to compute input permutations.
 */
public class InputPermutations implements Node.Visitor<Void>, Block.Visitor<Void> {

    private final Context context;
    private final Deque<DeferredCondition> conditions = new ArrayDeque<>();

    private InputPermutations(Map<String, String> externalValues, Map<String, String> externalDefaults) {
        this.context = Context.builder()
                              .externalValues(externalValues)
                              .externalDefaults(externalDefaults)
                              .scope(ContextNode.create(CopyOnWriteContextEdge::create))
                              .build();
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

        // Collect all permutations as a single path into a context with multi-edges nodes
        InputPermutations visitor = new InputPermutations(externalValues, externalDefaults);
        Walker.walk(visitor, script, null);

        System.out.println(ContextPrinter.print(visitor.context.scope()));

        // TODO 1. print root scope
        // TODO 2. filter tree
        return List.of();
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
    public VisitResult visitCondition(Condition condition, Void arg) {
        conditions.push(new DeferredCondition(condition, context.scope()));
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitBlock(Block block, Void arg) {
        if (block.kind() == Block.Kind.OUTPUT) {
            return VisitResult.SKIP_SUBTREE;
        }
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
            List<DeferredCondition> conditions = Lists.of(this.conditions);
            if (input instanceof Input.Boolean) {
                context.putValue(input.id(), new ConditionalValue(Value.TRUE, conditions), ValueKind.USER);
                context.putValue(input.id(), new ConditionalValue(Value.FALSE, conditions), ValueKind.USER);
            } else if (input instanceof Input.Options) {
                List<String> options = options(input);
                if (input instanceof Input.Enum) {
                    for (String permutation : options) {
                        Value value = new ConditionalValue(Value.create(permutation), conditions);
                        context.putValue(input.id(), value, ValueKind.USER);
                    }
                } else if (input instanceof Input.List) {
                    for (List<String> permutation : Permutations.of(options)) {
                        Value value = new ConditionalValue(Value.create(permutation), conditions);
                        context.putValue(input.id(), value, ValueKind.USER);
                    }
                }
            } else if (input instanceof Input.Text) {
                Value defaultValue = defaultValue(input, context);
                String rawValue = defaultValue != null ? defaultValue.asString() : null;
                // TODO add configuration for a map to supply data for text inputs
                Value value = rawValue != null ? Value.create(rawValue) : Value.create("xxx");
                context.putValue(input.id(), value, ValueKind.USER);
            }
            context.pushScope(input.id(), input.isGlobal());
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitInput(Input input, Void arg) {
        if (input instanceof DeclaredInput) {
            context.popScope();
        }
        return postVisitAny(input, arg);
    }

    @Override
    public VisitResult postVisitAny(Block block, Void arg) {
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

    private List<String> options(DeclaredInput node) {
        return node.children(n -> true, Option.class)
                   .map(Option::value)
                   .collect(toList());
    }

    private static final class ConditionalValue extends ValueDelegate {

        final List<DeferredCondition> conditions;

        ConditionalValue(Value value, List<DeferredCondition> conditions) {
            super(value);
            this.conditions = conditions;
        }

        @Override
        public String toString() {
            return "ConditionalValue{"
                    + "value=" + value().unwrap()
                    + ", condition=" + conditions.stream()
                                                 .map(c -> c.condition)
                                                 .map(Condition::rawExpression)
                                                 .collect(Collectors.joining(" && "))
                    + '}';
        }
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
