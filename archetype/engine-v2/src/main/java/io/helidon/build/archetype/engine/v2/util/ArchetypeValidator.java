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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.build.archetype.engine.v2.Context;
import io.helidon.build.archetype.engine.v2.ContextScope;
import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Expression;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.DeclaredInput;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.Variable;

import static java.util.Objects.requireNonNull;

/**
 * Archetype validator.
 * Performs complex validations that cannot be enforced with the XML schema.
 */
public final class ArchetypeValidator implements Node.Visitor<Context>, Block.Visitor<Context> {

    static final String PRESET_UNRESOLVED = "Preset input cannot be resolved";
    static final String PRESET_TYPE_MISMATCH = "Preset type mismatch";
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

    private final List<String> errors = new ArrayList<>();
    private final Deque<StepState> steps = new ArrayDeque<>();
    private final Map<String, List<Block>> allRefs = new HashMap<>();
    private final Deque<Map<String, DeclaredInput>> scopes = new ArrayDeque<>(List.of(new HashMap<>()));
    private final Deque<Set<String>> options = new ArrayDeque<>();
    private final List<Preset> presets = new ArrayList<>();
    private String inputPath = null;

    /**
     * Validate the given entry point.
     *
     * @param script entry point
     * @return list of errors
     */
    public static List<String> validate(Path script) {
        ArchetypeValidator validator = new ArchetypeValidator();
        Context context = Context.create(script.getParent());
        Walker.walk(validator, ScriptLoader.load(script), context, context::cwd);
        validator.validatePresets();
        return validator.errors;
    }

    private static final class StepState {

        private int inputs = 0;
        private boolean optional = true;
        private final List<StepState> steps = new ArrayList<>();

        boolean isOptional() {
            if (!optional) {
                return false;
            }
            Deque<StepState> stack = new ArrayDeque<>(steps);
            while (!stack.isEmpty()) {
                StepState elt = stack.pop();
                if (!elt.optional) {
                    return false;
                }
                elt.steps.forEach(stack::push);
            }
            return true;
        }
    }

    private void validatePresets() {
        for (Preset preset : presets) {
            String path = preset.path();
            List<Block> refs = allRefs.get(path);
            if (refs == null || refs.isEmpty()) {
                errors.add(String.format(
                        "%s %s: '%s'",
                        preset.location(),
                        PRESET_UNRESOLVED,
                        path));
            } else {
                Block ref = refs.get(0);
                if ((preset.kind() != ref.kind())) {
                    errors.add(String.format(
                            "%s %s: '%s', expected: %s, actual: %s",
                            preset.location(),
                            PRESET_TYPE_MISMATCH,
                            path,
                            ref.kind(),
                            preset.kind()));
                }
            }
        }
    }

    private List<Block> refs(String path, Context ctx) {
        List<Block> refs = allRefs.get(ctx.scope().path(path));
        if (refs != null) {
            return refs;
        }
        return allRefs.get(path);
    }

    @Override
    public VisitResult visitCondition(Condition condition, Context ctx) {
        try {
            condition.expression().eval(variable -> {
                List<Block> refs = refs(variable, ctx);
                if (refs == null || refs.isEmpty()) {
                    return null;
                }
                Block ref = refs.get(0);
                Block.Kind kind = ref.kind();
                switch (kind) {
                    case LIST:
                    case ENUM:
                        String value = ((Input.Options) ref).options(ctx::filterNode).get(0).value();
                        return kind == Input.Kind.LIST ? Value.create(List.of(value)) : Value.create(value);
                    case TEXT:
                        return Value.create("some text");
                    case BOOLEAN:
                        return Value.FALSE;
                    default:
                        throw new IllegalStateException("Bad input type");
                }
            });
        } catch (Expression.UnresolvedVariableException ex) {
            errors.add(String.format("%s %s: '%s'",
                    condition.location(),
                    EXPR_UNRESOLVED_VARIABLE,
                    ex.variable()));
        } catch (IllegalStateException ex) {
            errors.add(String.format(
                    "%s %s: '%s'",
                    condition.location(),
                    EXPR_EVAL_ERROR,
                    ex.getMessage()));
        }
        // visit all branches
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitPreset(Preset preset, Context ctx) {
        presets.add(preset);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitVariable(Variable variable, Context arg) {
        allRefs.computeIfAbsent(variable.path(), k -> new ArrayList<>()).add(variable);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitBlock(Block block, Context ctx) {
        if (block.kind() == Block.Kind.INVOKE_DIR) {
            ctx.pushCwd(block.scriptPath().getParent());
            return VisitResult.CONTINUE;
        }
        return block.accept((Block.Visitor<Context>) this, ctx);
    }

    @Override
    public VisitResult postVisitBlock(Block block, Context ctx) {
        if (block.kind() == Block.Kind.INVOKE_DIR) {
            ctx.popCwd();
            return VisitResult.CONTINUE;
        }
        return block.acceptAfter((Block.Visitor<Context>) this, ctx);
    }

    @Override
    public VisitResult visitStep(Step step, Context ctx) {
        StepState currentStep = steps.peek();
        StepState nextStep = new StepState();
        if (currentStep != null) {
            currentStep.steps.add(nextStep);
        }
        steps.push(nextStep);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitStep(Step step, Context ctx) {
        StepState stepState = steps.pop();
        boolean declaredOptional = step.isOptional();
        boolean optional = stepState.isOptional();
        if (stepState.inputs == 0) {
            errors.add(String.format("%s %s",
                    step.location(),
                    STEP_NO_INPUT));
        } else if (declaredOptional && !optional) {
            errors.add(String.format("%s %s",
                    step.location(),
                    STEP_DECLARED_OPTIONAL));
        } else if (!declaredOptional && optional) {
            errors.add(String.format("%s %s",
                    step.location(),
                    STEP_NOT_DECLARED_OPTIONAL));
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitInput(Input input0, Context ctx) {
        // process scope
        Map<String, DeclaredInput> currentScope = requireNonNull(scopes.peek(), "current scope is null");
        scopes.push(new HashMap<>());

        if (input0 instanceof DeclaredInput) {
            DeclaredInput input = (DeclaredInput) input0;
            ContextScope ctxScope = ctx.scope().getOrCreate(input.id(), input.isGlobal());
            inputPath = ctxScope.id();
            ctx.pushScope(ctxScope);
            allRefs.computeIfAbsent(inputPath, k -> new ArrayList<>());

            if (input instanceof Input.Options) {
                options.push(new HashSet<>());
            }

            if (currentScope.containsKey(inputPath)) {
                errors.add(String.format("%s %s: '%s'",
                        input.location(),
                        INPUT_ALREADY_DECLARED,
                        inputPath));
            } else {
                currentScope.put(inputPath, input);
                List<Block> duplicates = requireNonNull(allRefs.get(inputPath), "duplicate refs is null");
                if (!duplicates.isEmpty()) {
                    Block duplicate = duplicates.get(0);
                    if (duplicate.kind() != input.kind()) {
                        errors.add(String.format("%s %s: '%s'",
                                input.location(),
                                INPUT_TYPE_MISMATCH,
                                inputPath));
                    }
                }
                duplicates.add(input);
            }

            boolean optional = input.isOptional();
            if (optional && input.defaultValue().unwrap() == null) {
                errors.add(String.format("%s %s: '%s'",
                        input.location(),
                        INPUT_OPTIONAL_NO_DEFAULT,
                        inputPath));
            }

            StepState stepState = steps.peek();
            if (stepState == null) {
                errors.add(String.format("%s %s: '%s'",
                        input.location(),
                        INPUT_NOT_IN_STEP,
                        inputPath));
            } else {
                stepState.optional = stepState.optional && optional;
                stepState.inputs++;
            }
        } else if (input0 instanceof Input.Option) {
            Input.Option option = (Input.Option) input0;
            String value = option.value();
            if (!requireNonNull(options.peek(), "option values is null").add(value)) {
                errors.add(String.format(
                        "%s %s: '%s'",
                        option.location(),
                        OPTION_VALUE_ALREADY_DECLARED,
                        value));
            }
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitInput(Input input, Context ctx) {
        if (input instanceof DeclaredInput) {
            ctx.popScope();
            if (input instanceof Input.Options) {
                options.pop();
            }
            inputPath = null;
        }
        scopes.pop();
        return VisitResult.CONTINUE;
    }
}
