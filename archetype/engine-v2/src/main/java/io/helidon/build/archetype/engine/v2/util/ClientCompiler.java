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
package io.helidon.build.archetype.engine.v2.util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DeclaredBlock;
import io.helidon.build.archetype.engine.v2.ast.DeclaredValue;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Method;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.BuilderInfo;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.Variable;

/**
 * Client script builder.
 */
public class ClientCompiler implements Node.Visitor<Script> {

    private final LinkedList<Node.Builder<?, ?>> stack = new LinkedList<>();
    private final ScriptLoader loader = ScriptLoader.create();
    private final Path path = loader.unknownPath();
    private final Map<String, Method.Builder> methodBuilders = new HashMap<>();
    private final ArchetypeInfo info;
    private final boolean obfuscate;
    private final Block.Builder methodsBuilder;
    private final Script.Builder scriptBuilder;

    private ClientCompiler(ArchetypeInfo info, boolean obfuscate) {
        this.info = info;
        this.obfuscate = obfuscate;
        BuilderInfo builderInfo = BuilderInfo.of(loader, path);
        methodsBuilder = Block.builder(builderInfo, Block.Kind.METHODS);
        scriptBuilder = Script.builder(builderInfo);
        scriptBuilder.addChild(methodsBuilder);
    }

    void addChild(Node.Builder<? extends Node, ?> builder) {
        Node.Builder<?, ?> parent = stack.peek();
        if (parent == null) {
            throw new IllegalStateException("parent builder is null");
        }
        if (parent instanceof Condition.Builder) {
            stack.pop();
            ((Condition.Builder) parent).then(builder);
        } else {
            parent.addChild(builder);
        }
    }

    private BuilderInfo builderInfo(Node node) {
        return BuilderInfo.of(loader, path, node.location());
    }

    @Override
    public Node.VisitResult visitInvocation(Invocation invocation, Script script) {
        DeclaredBlock declaredBlock = info.invocations().get(invocation);
        if (declaredBlock == null) {
            throw new IllegalStateException("Unresolved invocation: " + invocation);
        }
        Invocation.Builder builder = Invocation.builder(builderInfo(invocation), Invocation.Kind.CALL);
        builder.attribute("method", Value.create(methodName(declaredBlock)));
        addChild(builder);
        return Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult visitCondition(Condition condition, Script script) {
        Condition.Builder builder = Condition.builder(builderInfo(condition)).expression(condition.expression());
        addChild(builder);
        stack.push(builder);
        return Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult visitBlock(Block block, Script script) {
        Block.Kind kind = block.kind();
        BuilderInfo builderInfo = builderInfo(block);
        if (block instanceof DeclaredBlock) {
            if (block.equals(script)) {
                stack.push(scriptBuilder);
            } else {
                String methodName = methodName((DeclaredBlock) block);
                Method.Builder builder = methodBuilders.get(methodName);
                if (builder == null) {
                    builder = Method.builder(builderInfo);
                    builder.attribute("name", Value.create(methodName));
                    methodsBuilder.addChild(builder);
                    methodBuilders.put(methodName, builder);
                }
                stack.push(builder);
            }
        } else {
            Block.Builder builder;
            if (block instanceof DeclaredValue) {
                if (block instanceof Preset) {
                    builder = Preset.builder(builderInfo(block), kind);
                } else if (block instanceof Variable) {
                    builder = Variable.builder(builderInfo(block), kind);
                } else {
                    throw new UnsupportedOperationException("Unsupported declared value: " + block);
                }
                DeclaredValue declaredValue = (DeclaredValue) block;
                if (kind == Block.Kind.LIST) {
                    for (String value : declaredValue.value().asList()) {
                        builder.addChild(Block.builder(builderInfo, Block.Kind.VALUE).value(value));
                    }
                } else {
                    String value = String.valueOf(declaredValue.value().unwrap());
                    if (kind == Block.Kind.ENUM) {
                        builder.value(value);
                    } else {
                        builder.addChild(Block.builder(builderInfo, Block.Kind.VALUE).value(value));
                    }
                }
            } else if (block instanceof Step) {
                builder = Step.builder(builderInfo(block));
            } else if (block instanceof Input) {
                builder = Input.builder(builderInfo, kind);
            } else {
                builder = Block.builder(builderInfo, kind);
            }
            builder.attributes(block.attributes());
            addChild(builder);
            stack.push(builder);
        }
        return Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult postVisitBlock(Block block, Script arg) {
        Node.Builder<?, ?> builder = stack.pop();
        if (builder.children().isEmpty()) {
            // skip certain blocks without children
            switch (block.kind()) {
                case STEP:
                case INPUTS:
                case PRESETS:
                case VARIABLES:
                case INVOKE:
                case INVOKE_DIR:
                    Node.Builder<?, ?> parentBuilder = stack.peek();
                    if (parentBuilder != null) {
                        parentBuilder.nestedBuilders().remove(builder);
                    }
                    return Node.VisitResult.CONTINUE;
                default:
            }
        }
        builder.build();
        return Node.VisitResult.CONTINUE;
    }

    private String methodName(DeclaredBlock block) {
        if (obfuscate) {
            return String.valueOf(block.nodeId());
        } else {
            return block.blockName();
        }
    }

    private Script buildScript() {
        Script script = scriptBuilder.build();
        loader.add(script);
        return script;
    }

    /**
     * Perform a pseudo compilation for the given script.
     * <ul>
     *     <li>The returned script is a filtered copy of the input script that matches {@link ClientPredicate#test}</li>
     *     <li>Script invocations are converted to method invocations to produce an all-in-one script</li>
     *     <li>Empty {@link DeclaredBlock} and corresponding invocations are removed</li>
     *     <li>Single invocations have their target block inlined inside their enclosing block</li>
     * </ul>
     *
     * @param script    script
     * @param obfuscate {@code true} if the script paths and method names should be replaced with ids
     * @return compiled script
     */
    public static Script compile(Script script, boolean obfuscate) {
        ArchetypeInfo info = ArchetypeScanner.scan(script, ClientPredicate::test);
        ClientCompiler compiler = new ClientCompiler(info, obfuscate);
        OptimizedWalker.walk(compiler, script, script, info);
        return compiler.buildScript();
    }
}
