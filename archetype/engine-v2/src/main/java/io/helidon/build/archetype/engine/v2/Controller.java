/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

import java.util.Deque;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Variable;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextScope;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;

/**
 * Controller.
 * Context aware visitor adapter with convenience methods to perform full AST traversal with complete flow control.
 * Always uses an implementation of {@link InputResolver} in order to control the flow of input nodes.
 */
public final class Controller extends VisitorAdapter<Context> {

    private final Deque<Step> steps;

    private Controller(InputResolver inputResolver,
                       Output.Visitor<Context> outputVisitor,
                       Model.Visitor<Context> modelVisitor) {

        super(inputResolver, outputVisitor, modelVisitor, inputResolver);
        this.steps = inputResolver.steps();
    }

    @Override
    public VisitResult visitStep(Step step, Context ctx) {
        steps.push(step);
        return super.visitStep(step, ctx);
    }

    @Override
    public VisitResult postVisitStep(Step step, Context arg) {
        steps.pop();
        return super.postVisitStep(step, arg);
    }

    @Override
    public VisitResult visitPreset(Preset preset, Context ctx) {
        ctx.putValue(preset.path(), preset.value(), ValueKind.PRESET);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitVariable(Variable variable, Context ctx) {
        ctx.putValue(variable.path(), variable.value(), ValueKind.LOCAL_VAR);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitBlock(Block block, Context ctx) {
        if (block.kind() == Block.Kind.INVOKE_DIR) {
            ctx.pushCwd(block.scriptPath().getParent());
            return VisitResult.CONTINUE;
        }
        return super.visitBlock(block, ctx);
    }

    @Override
    public VisitResult postVisitBlock(Block block, Context ctx) {
        if (block.kind() == Block.Kind.INVOKE_DIR) {
            ctx.popCwd();
            return VisitResult.CONTINUE;
        }
        return super.postVisitBlock(block, ctx);
    }

    @Override
    public VisitResult visitCondition(Condition condition, Context ctx) {
        if (condition.expression().eval(ctx::getValue)) {
            return VisitResult.CONTINUE;
        }
        return VisitResult.SKIP_SUBTREE;
    }

    /**
     * Walk.
     *
     * @param inputResolver input resolver
     * @param block         block, must be non {@code null}
     * @param context       context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static void walk(InputResolver inputResolver, Block block, Context context) {
        walk(inputResolver, null, null, block, context);
    }

    /**
     * Walk.
     *
     * @param inputResolver input resolver
     * @param outputVisitor output visitor
     * @param block         block, must be non {@code null}
     * @param context       context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    @SuppressWarnings("unused")
    public static void walk(InputResolver inputResolver,
                            Output.Visitor<Context> outputVisitor,
                            Block block,
                            Context context) {

        walk(inputResolver, outputVisitor, null, block, context);
    }

    /**
     * Walk.
     *
     * @param inputResolver input resolver
     * @param modelVisitor  model visitor
     * @param block         block, must be non {@code null}
     * @param context       context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    @SuppressWarnings("unused")
    public static void walk(InputResolver inputResolver,
                            Model.Visitor<Context> modelVisitor,
                            Block block,
                            Context context) {

        walk(inputResolver, null, modelVisitor, block, context);
    }

    /**
     * Walk using a {@link BatchInputResolver} input resolver.
     *
     * @param block   block, must be non {@code null}
     * @param context context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static void walk(Block block, Context context) {
        walk(new BatchInputResolver(), null, null, block, context);
    }

    /**
     * Walk using a {@link BatchInputResolver} input resolver.
     *
     * @param outputVisitor output visitor
     * @param block         block, must be non {@code null}
     * @param context       context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static void walk(Output.Visitor<Context> outputVisitor, Block block, Context context) {
        walk(new BatchInputResolver(), outputVisitor, null, block, context);
    }

    /**
     * Walk using a {@link BatchInputResolver} input resolver.
     *
     * @param modelVisitor model visitor
     * @param block        block, must be non {@code null}
     * @param context      context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static void walk(Model.Visitor<Context> modelVisitor, Block block, Context context) {
        walk(new BatchInputResolver(), null, modelVisitor, block, context);
    }

    /**
     * Walk.
     *
     * @param resolver      input resolver
     * @param outputVisitor output visitor
     * @param modelVisitor  model visitor
     * @param block         block, must be non {@code null}
     * @param context       context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static void walk(InputResolver resolver,
                            Output.Visitor<Context> outputVisitor,
                            Model.Visitor<Context> modelVisitor,
                            Block block,
                            Context context) {

        Objects.requireNonNull(context, "context is null");
        ContextScope scope = context.scope();
        Controller controller = new Controller(resolver, outputVisitor, modelVisitor);
        Walker.walk(controller, block, context, context::cwd);
        if (scope != context.scope()) {
            throw new IllegalStateException("Invalid scope after walking block");
        }
    }
}
