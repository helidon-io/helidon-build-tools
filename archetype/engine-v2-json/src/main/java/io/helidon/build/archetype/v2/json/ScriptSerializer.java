/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.archetype.v2.json;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DeclaredBlock;
import io.helidon.build.archetype.engine.v2.ast.Expression;
import io.helidon.build.archetype.engine.v2.ast.Expression.Token;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.NamedInput;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Invocation.MethodInvocation;
import io.helidon.build.archetype.engine.v2.ast.Method;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.Variable;
import io.helidon.build.archetype.engine.v2.util.ClientCompiler;

// TODO don't include default attributes (global=false, optional=false etc)
// TODO don't include input prompt (for cli only)
// TODO bug with conditional preset
// TODO bug with variable/preset null value

/**
 * Script serializer.
 */
public final class ScriptSerializer implements Node.Visitor<Script>,
                                               Invocation.Visitor<Void>,
                                               Block.Visitor<JsonObjectBuilder> {

    private static final TokenVisitor TOKEN_VISITOR = new TokenVisitor();

    private final AtomicInteger nextExprId = new AtomicInteger();
    private final AtomicInteger nextStepId = new AtomicInteger();
    private final AtomicInteger nextMethodId = new AtomicInteger();
    private final Map<Expression, String> exprIds = new HashMap<>();
    private final Map<Step, String> stepsIds = new HashMap<>();
    private final Deque<Context> stack = new ArrayDeque<>();
    private final JsonObjectBuilder expressionsBuilder = JsonFactory.createObjectBuilder();
    private final Map<String, String> methodIds = new HashMap<>();
    private final Map<String, Context> methodContexts = new HashMap<>();
    private final JsonObjectBuilder methodsBuilder = JsonFactory.createObjectBuilder();
    private final JsonArrayBuilder directivesBuilder = JsonFactory.createArrayBuilder();
    private final boolean obfuscate;
    private volatile String exprId;

    private ScriptSerializer(boolean obfuscate) {
        this.obfuscate = obfuscate;
    }

    /**
     * Serialize the given archetype to JSON.
     *
     * @param archetype archetype file system
     * @return JsonObject
     */
    public static JsonObject serialize(FileSystem archetype) {
        return serialize(archetype, true);
    }

    /**
     * Serialize the given archetype to JSON.
     *
     * @param archetype archetype file system
     * @param obfuscate {@code true} if the script path and method name should be replaced with ids
     * @return JsonObject
     */
    public static JsonObject serialize(FileSystem archetype, boolean obfuscate) {
        Path scriptPath = archetype.getPath("/").resolve("main.xml");
        Script script = ScriptLoader.load(scriptPath);
        return serialize(script, obfuscate);
    }

    /**
     * Serialize the given archetype to JSON.
     *
     * @param script    entrypoint script
     * @return JsonObject
     */
    public static JsonObject serialize(Script script) {
        return serialize(script, true);
    }

    /**
     * Serialize the given archetype to JSON.
     *
     * @param script    entrypoint script
     * @param obfuscate {@code true} if the script path and method name should be replaced with ids
     * @return JsonObject
     */
    public static JsonObject serialize(Script script, boolean obfuscate) {
        ScriptSerializer serializer = new ScriptSerializer(obfuscate);
        Script compiledScript = ClientCompiler.compile(script, obfuscate);
        Walker.walk(serializer, compiledScript, compiledScript);
        return JsonFactory.createObjectBuilder()
                          .add("expressions", serializer.expressionsBuilder.build())
                          .add("methods", serializer.methodsBuilder.build())
                          .add("children", serializer.directivesBuilder.build())
                          .build();
    }

    /**
     * Convert the given expression to JSON.
     *
     * @param expression expression
     * @return JsonArray
     */
    static JsonArray serialize(Expression expression) {
        JsonArrayBuilder builder = JsonFactory.createArrayBuilder();
        for (Token token : expression.tokens()) {
            builder.add(convertToken(token));
        }
        return builder.build();
    }

    private static final class Context {

        private final JsonObjectBuilder block;
        private final JsonArrayBuilder children;
        private final String exprId;

        Context(JsonObjectBuilder block, JsonArrayBuilder children, String exprId) {
            this.block = block;
            this.children = children;
            this.exprId = exprId;
        }
    }

    @Override
    public VisitResult visitMethodInvocation(MethodInvocation inv, Void arg) {
        String exprId = this.exprId;
        this.exprId = null;
        JsonObjectBuilder builder = JsonFactory.createObjectBuilder();
        String methodId = methodIds.computeIfAbsent(inv.method(), this::methodId);
        builder.add("kind", "call")
               .add("method", methodId);
        if (exprId != null) {
            builder.add("if", exprId);
        }
        ctx().children.add(builder.build());
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitScriptInvocation(Invocation.ScriptInvocation invocation, Void arg) {
        return VisitResult.SKIP_SUBTREE;
    }

    @Override
    public VisitResult visitInvocation(Invocation invocation, Script script) {
        return invocation.accept((Invocation.Visitor<Void>) this, null);
    }

    @Override
    public VisitResult visitCondition(Condition cond, Script script) {
        exprId = exprIds.computeIfAbsent(cond.expression(), this::exprId);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitBlock(Block block, Script script) {
        Block.Kind kind = block.kind();
        if (block instanceof DeclaredBlock) {
            if (block.equals(script)) {
                stack.push(new Context(null, directivesBuilder, null));
                return VisitResult.CONTINUE;
            }
            if (block instanceof Method){
                String methodId = methodIds.get(((Method) block).name());
                Context ctx = methodContexts.get(methodId);
                if (ctx == null) {
                    JsonObjectBuilder builder = blockBuilder(block);
                    builder.add("name", methodId);
                    ctx = new Context(builder, JsonFactory.createArrayBuilder(), null);
                    methodContexts.put(methodId, ctx);
                    stack.push(ctx);
                    return VisitResult.CONTINUE;
                }
                stack.push(ctx);
                // method has already been visited
                // fall through and skip the subtree
                // i.e. no children for subsequent invocations of the same method
            }
            return VisitResult.SKIP_SUBTREE;
        }
        switch (kind) {
            case INVOKE:
                return VisitResult.CONTINUE;
            case INVOKE_DIR:
                return VisitResult.SKIP_SUBTREE;
            default:
        }
        JsonObjectBuilder builder = blockBuilder(block);
        stack.push(new Context(builder, JsonFactory.createArrayBuilder(), exprId));
        return block.accept(this, builder);
    }

    @Override
    public VisitResult postVisitBlock(Block block, Script script) {
        if (block instanceof DeclaredBlock) {
            if (block.equals(script)) {
                stack.pop();
                return VisitResult.CONTINUE;
            }
            if (block instanceof Method){
                String methodId = methodIds.get(((Method) block).name());
                JsonArray methodDirectives = stack.pop().children.build();
                if (!methodDirectives.isEmpty()) {
                    methodsBuilder.add(methodId, methodDirectives);
                }
                return VisitResult.CONTINUE;
            }
            return VisitResult.SKIP_SUBTREE;
        }
        switch (block.kind()) {
            case INVOKE_DIR:
                return VisitResult.SKIP_SUBTREE;
            case INVOKE:
                return VisitResult.CONTINUE;
            default:
        }
        Context ctx = stack.pop();
        if (ctx.exprId != null) {
            ctx.block.add("if", ctx.exprId);
        }
        JsonArray children = ctx.children.build();
        if (!children.isEmpty()) {
            ctx.block.add("children", children);
        }
        Context parentCtx = stack.peek();
        if (parentCtx == null) {
            throw new IllegalStateException("parent context is null");
        }
        parentCtx.children.add(ctx.block.build());
        return VisitResult.CONTINUE;
    }

    private VisitResult visitNamed(Input.NamedInput input, JsonObjectBuilder builder) {
        builder.add("name", input.name());
        if (input.isGlobal()) {
            builder.add("global", true);
        }
        if (input.isOptional()) {
            builder.add("optional", true);
        }
        Value defaultValue = input.defaultValue();
        if (defaultValue != null && defaultValue != Value.NULL) {
            builder.add("default", JsonFactory.createValue(defaultValue));
        }
        return VisitResult.CONTINUE;
    }

    private VisitResult visitOption(Input.Option option, JsonObjectBuilder builder) {
        builder.add("value", JsonFactory.createValue(option.value()));
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitInput(Input input, JsonObjectBuilder builder) {
        builder.add("label", input.label());
        if (input instanceof NamedInput) {
            return visitNamed((NamedInput) input, builder);
        } else if (input instanceof Input.Option) {
            return visitOption((Input.Option) input, builder);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitPreset(Preset preset, JsonObjectBuilder builder) {
        builder.add("path", preset.path())
               .add("value", JsonFactory.createValue(preset.value()));
        return VisitResult.SKIP_SUBTREE;
    }

    @Override
    public VisitResult visitVariable(Variable variable, JsonObjectBuilder builder) {
        builder.add("path", variable.path())
               .add("value", JsonFactory.createValue(variable.value()));
        return VisitResult.SKIP_SUBTREE;
    }

    @Override
    public VisitResult visitStep(Step step, JsonObjectBuilder builder) {
        builder.add("label", step.label())
               .add("id", stepsIds.computeIfAbsent(step, this::stepId));
        return VisitResult.CONTINUE;
    }

    private JsonObjectBuilder blockBuilder(Block block) {
        JsonObjectBuilder builder = JsonFactory.createObjectBuilder();
        builder.add("kind", block.kind().name().toLowerCase());
        return builder;
    }

    private Context ctx() {
        Context ctx = stack.peek();
        if (ctx == null) {
            throw new IllegalStateException("ctx is null");
        }
        return ctx;
    }

    @SuppressWarnings("unused")
    private String stepId(Step step) {
        return String.valueOf(nextStepId.incrementAndGet());
    }

    @SuppressWarnings("unused")
    private String methodId(String method) {
        if (obfuscate) {
            return String.valueOf(nextMethodId.incrementAndGet());
        } else {
            return method;
        }
    }

    private String exprId(Expression expr) {
        String exprId = String.valueOf(nextExprId.incrementAndGet());
        expressionsBuilder.add(exprId, serialize(expr));
        return exprId;
    }

    private static final class TokenVisitor implements Token.Visitor<JsonObjectBuilder> {

        @Override
        public void visitOperator(Expression.Operator operator, JsonObjectBuilder builder) {
            builder.add("kind", "operator")
                   .add("value", operator.symbol());
        }

        @Override
        public void visitVariable(String variable, JsonObjectBuilder builder) {
            builder.add("kind", "variable")
                   .add("value", variable);
        }

        @Override
        public void visitOperand(Value value, JsonObjectBuilder builder) {
            builder.add("kind", "literal")
                   .add("value", JsonFactory.createValue(value));
        }
    }

    private static JsonObject convertToken(Token token) {
        JsonObjectBuilder builder = JsonFactory.createObjectBuilder();
        token.accept(TOKEN_VISITOR, builder);
        return builder.build();
    }
}
