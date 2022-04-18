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

package io.helidon.build.archetype.engine.v2;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.NamedInput;
import io.helidon.build.archetype.engine.v2.ast.Input.Option;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.common.GenericType;

/**
 * Input resolver.
 * Provides input resolution and controls the traversal of input nodes.
 *
 * @see Controller
 */
public abstract class InputResolver implements Input.Visitor<Context> {

    private final Deque<NamedInput> parents = new ArrayDeque<>();
    private final Deque<Step> currentSteps = new ArrayDeque<>();
    private final Set<Step> visitedSteps = new HashSet<>();

    /**
     * Get the stack of steps.
     *
     * @return steps
     */
    Deque<Step> steps() {
        return currentSteps;
    }

    /**
     * Invoked for every visited step.
     *
     * @param step    step
     * @param context context
     */
    protected void onVisitStep(Step step, Context context) {
    }

    /**
     * Invoked for every named input visit.
     *
     * @param input   input
     * @param context context
     * @return visit result if a value already exists, {@code null} otherwise
     */
    protected VisitResult onVisitInput(NamedInput input, Context context) {
        Step currentStep = currentSteps.peek();
        if (currentStep == null) {
            throw new IllegalStateException("Invalid state, input not nested inside a step");
        }
        parents.push(input);
        boolean global = input.isGlobal();
        String path = context.path(input.name());
        if (global) {
            if (!path.equals(input.name())) {
                throw new IllegalStateException("Invalid state, input '" + path + "' cannot be global");
            }
        }
        Value value = context.get(path);
        if (value == null) {
            if (!visitedSteps.contains(currentStep)) {
                visitedSteps.add(currentStep);
                onVisitStep(currentStep, context);
            }
            return null;
        }
        input.validate(value, path);
        context.push(path, global);
        return input.visitValue(value);
    }

    /**
     * Compute the default value for an input.
     *
     * @param input   input
     * @param context context
     * @return default value or {@code null} if none
     */
    protected Value defaultValue(NamedInput input, Context context) {
        Value defaultValue = context.defaultValue(input.name());
        if (defaultValue == null) {
            defaultValue = input.defaultValue();
        }
        if (defaultValue != null) {
            GenericType<?> valueType = defaultValue.type();
            if (valueType == ValueTypes.STRING) {
                String value = context.substituteVariables(input.normalizeOptionValue(defaultValue.asString()));
                return Value.create(value);
            } else if (valueType == ValueTypes.STRING_LIST) {
                return Value.create(defaultValue.asList().stream()
                                                .map(context::substituteVariables)
                                                .map(input::normalizeOptionValue)
                                                .collect(Collectors.toList()));
            }
        }
        return defaultValue;
    }

    @Override
    public VisitResult visitOption(Option option, Context context) {
        if (parents.isEmpty()) {
            throw new IllegalStateException("parents is empty");
        }
        NamedInput parent = parents.peek();
        Value inputValue = context.lookup("PARENT." + parent.name());
        if (inputValue != null) {
            return parent.visitOption(inputValue, option);
        }
        return VisitResult.SKIP_SUBTREE;
    }

    @Override
    public VisitResult postVisitAny(Input input, Context context) {
        if (input instanceof NamedInput) {
            parents.pop();
            if (!((NamedInput) input).isGlobal()) {
                context.pop();
            }
        }
        return VisitResult.CONTINUE;
    }
}
