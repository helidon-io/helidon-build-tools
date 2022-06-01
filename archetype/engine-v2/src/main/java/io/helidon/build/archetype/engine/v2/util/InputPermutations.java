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
import java.util.Map.Entry;

import io.helidon.build.archetype.engine.v2.Context;
import io.helidon.build.archetype.engine.v2.ContextValue;
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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A utility to compute input permutations.
 */
public class InputPermutations implements Node.Visitor<Void>, Block.Visitor<Void> {

    private final Context context;
    private final Deque<List<Map<String, String>>> stack = new ArrayDeque<>();

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
    public VisitResult visitCondition(Condition condition, Void arg) {
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
    public VisitResult visitInput(Input input0, Void arg) {
        if (input0 instanceof DeclaredInput) {
            DeclaredInput input = (DeclaredInput) input0;
            Context.Scope scope = context.newScope(input.id(), input.isGlobal());
            context.pushScope(scope);
        }
        return VisitResult.CONTINUE;
    }

    private List<Map<String, String>> enumPermutations(Input.Enum input,
                                                       String id,
                                                       Map<String, List<Map<String, String>>> nested) {
        return options(input).stream()
                             .flatMap(o -> {
                                 Map<String, String> value = Map.of(id, o);
                                 return nested.get(o).stream().map(m -> Maps.putAll(m, value));
                             })
                             .collect(toList());
    }

    private List<Map<String, String>> listPermutations(Input.List input,
                                                       String id,
                                                       Map<String, List<Map<String, String>>> nested) {

        return Lists.flatMapElement(Permutations.of(options(input)), inputValue -> {
            // set of permutations for the input value
            List<List<Entry<String, String>>> ivEntries = Lists.mapElement(
                    inputValue,
                    v -> Lists.flatMapElement(nested.get(v), m -> m.entrySet().stream()));

            // computed permutations for the input value
            List<Map<String, String>> ivPermutations = Permutations.ofList(ivEntries)
                                                                   .stream()
                                                                   .map(Maps::fromEntries)
                                                                   .collect(toList());

            // add an entry for each permutation to represent the input value
            return Maps.putAll(ivPermutations, Map.of(id, String.join(" ", inputValue)))
                       .stream();
        });
    }

    @Override
    public VisitResult postVisitInput(Input input, Void arg) {
        String id = context.peekScope().id();
        List<Map<String, String>> permutations = requireNonNull(stack.pop());
        if (input instanceof Option) {
            String value = ((Option) input).value();
            stack.push(Maps.putIfAbsent(permutations, id, value));
        } else if (input instanceof Input.Boolean) {
            stack.push(Lists.addAll(Maps.putAll(permutations, Map.of(id, "true")), List.of(Map.of(id, "false"))));
            context.popScope();
        } else if (input instanceof Input.Options) {
            // nested permutations
            Map<String, List<Map<String, String>>> nested = Maps.keyedBy(permutations, id);
            if (input instanceof Input.Enum) {
                stack.push(enumPermutations((Input.Enum) input, id, nested));
            } else if (input instanceof Input.List) {
                stack.push(listPermutations((Input.List) input, id, nested));
            }
            context.popScope();
        } else if (input instanceof Input.Text) {
            Value defaultValue = ((Input.Text) input).defaultValue();
            if (defaultValue != null) {
                String value = defaultValue.asString();
                stack.push(Maps.putAll(permutations, Map.of(id, value)));
            } else {
                stack.push(List.of());
            }
            context.popScope();
        }
        return postVisitAny(input, arg);
    }

    @Override
    public VisitResult visitVariable(Variable variable, Void arg) {
        context.setVariable(variable.path(), variable.value());
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitPreset(Preset preset, Void arg) {
        context.setValue(preset.path(), preset.value(), ContextValue.ValueKind.PRESET);
        return VisitResult.CONTINUE;
    }

    private boolean filterNode(Node node) {
        try {
            return context.filterNode(node);
        } catch (IllegalArgumentException ex){
            // include the node even if variable cannot be resolved
            // worse case we generate a bad permutation
            return true;
        }
    }

    @Override
    public VisitResult postVisitAny(Block block, Void arg) {

        // unset presets
        block.children(this::filterNode, Block.class, b -> b.kind() == Block.Kind.PRESETS)
             .flatMap(b -> b.children(context::filterNode, Preset.class))
             .forEach(p -> context.unsetValue(p.path()));

        // unset variables
        block.children(this::filterNode, Block.class, b -> b.kind() == Block.Kind.VARIABLES)
             .flatMap(b -> b.children(context::filterNode, Variable.class))
             .forEach(p -> context.unsetVariable(p.path()));

        if (stack.size() > 1) {
            // add to parent
            List<Map<String, String>> popped = stack.pop();
            requireNonNull(stack.peek()).addAll(popped);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitBlock(Block block, Void arg) {
        return block.acceptAfter((Block.Visitor<Void>) this, null);
    }

    private List<String> options(DeclaredInput node) {
        return node.children(this::filterNode, Option.class).map(Option::value).collect(toList());
    }
}
