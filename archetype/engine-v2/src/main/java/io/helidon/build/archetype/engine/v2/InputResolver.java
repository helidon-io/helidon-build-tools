/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput;
import io.helidon.build.archetype.engine.v2.ast.Input.Option;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Validation;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextScope;
import io.helidon.build.archetype.engine.v2.context.ContextValue;
import io.helidon.build.common.GenericType;

import static io.helidon.build.archetype.engine.v2.ast.Input.Enum.optionIndex;

/**
 * Input resolver.
 * Provides input resolution and controls the traversal of input nodes.
 *
 * @see Controller
 */
public abstract class InputResolver implements Input.Visitor<Context>, Validation.Visitor<Context> {

    private final Map<String, List<Validation.Regex>> validations = new HashMap<>();
    private final Deque<DeclaredInput> parents = new ArrayDeque<>();
    private final Deque<Step> currentSteps = new ArrayDeque<>();
    private final Set<Step> visitedSteps = new HashSet<>();
    private String validationId;
    private LinkedList<Validation.Regex> regexs;

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
     * @param scope   scope
     * @param context context
     * @return visit result if a value already exists, {@code null} otherwise
     */
    protected VisitResult onVisitInput(DeclaredInput input, ContextScope scope, Context context) {
        Step currentStep = currentSteps.peek();
        if (currentStep == null) {
            throw new IllegalStateException(input.location() + " Input not nested inside a step");
        }
        if (input.isGlobal()) {
            DeclaredInput parent = parents.peek();
            if (parent != null && !parent.isGlobal()) {
                throw new IllegalStateException(input.location() + " Parent input is not global");
            }
        }
        parents.push(input);
        Value value = existingValue(input, scope, context);
        if (value == null) {
            if (!visitedSteps.contains(currentStep)) {
                visitedSteps.add(currentStep);
                onVisitStep(currentStep, context);
            }
            return null;
        }
        input.validate(value, scope.path(true), validations);
        return input.visitValue(value);
    }

    private Value existingValue(DeclaredInput input, ContextScope scope, Context context) {
        Value value = context.getValue(input.id());
        if (value != null) {
            return value;
        }
        if (input instanceof Input.Options) {
            Input.Options optionsBlock = (Input.Options) input;
            List<Option> options = optionsBlock.options(n -> Condition.filter(n, context::getValue));
            if ((input instanceof Input.List)) {
                // auto create a value for lists without options
                if (options.isEmpty()) {
                    return context.putValue(input.id(), Value.create(List.of()), ContextValue.ValueKind.DEFAULT);
                }
            } else if (input instanceof Input.Enum) {
                // auto create a value if there is only one option with a default value
                Value defaultValue = defaultValue(input, scope, context);
                if (defaultValue != null) {
                    int defaultIndex = optionIndex(defaultValue.asString(), options);
                    if (options.size() == 1 && defaultIndex >= 0) {
                        return context.putValue(input.id(), defaultValue, ContextValue.ValueKind.DEFAULT);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Compute the default value for an input.
     *
     * @param input   input
     * @param scope   scope
     * @param context context
     * @return default value or {@code null} if none
     */
    public static Value defaultValue(DeclaredInput input, ContextScope scope, Context context) {
        Value defaultValue = context.defaultValue(scope.path());
        if (defaultValue == null) {
            defaultValue = input.defaultValue();
        }
        if (defaultValue != null) {
            GenericType<?> valueType = defaultValue.type();
            if (valueType == ValueTypes.STRING) {
                String value = context.interpolate(input.normalizeOptionValue(defaultValue.asString()));
                return Value.create(value);
            } else if (valueType == ValueTypes.STRING_LIST) {
                return Value.create(defaultValue.asList().stream()
                                                .map(context::interpolate)
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
        DeclaredInput parent = parents.peek();
        Value inputValue = context.scope().getValue("");
        if (inputValue != null) {
            return parent.visitOption(inputValue, option);
        }
        return VisitResult.SKIP_SUBTREE;
    }

    @Override
    public VisitResult postVisitAny(Input input, Context context) {
        if (input instanceof DeclaredInput) {
            parents.pop();
            context.popScope();
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitValidation(Validation validation, Context arg) {
        validationId = validation.id();
        this.regexs = new LinkedList<>();
        this.validations.put(validation.id(), this.regexs);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitValidation(Validation validation, Context arg) {
        validationId = null;
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitRegex(Validation.Regex regex, Context arg) {
        this.regexs.add(regex);
        return VisitResult.CONTINUE;
    }

}
