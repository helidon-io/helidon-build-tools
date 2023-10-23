/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DeclaredBlock;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.common.Maps;

/**
 * Optimized walker.
 *
 * @param <T> visitor argument type
 */
public class OptimizedWalker<T> implements Node.Visitor<Void> {

    private final Node.Visitor<T> visitor;
    private final T visitorArg;
    private final Set<Node> inlined;
    private final Map<Invocation, DeclaredBlock> invocations;
    private final Map<Node, DeclaredBlock> blocks;
    private final Map<DeclaredBlock, Set<Node>> nodes;

    private OptimizedWalker(Node.Visitor<T> visitor, T visitorArg, ArchetypeInfo info) {
        this.visitor = visitor;
        this.visitorArg = visitorArg;
        this.inlined = new HashSet<>();
        this.invocations = new HashMap<>(info.invocations());
        this.blocks = new HashMap<>(info.blocks());
        this.nodes = new HashMap<>(info.nodes());
        optimize();
    }

    /**
     * Perform an optimized walk.
     *
     * @param visitor    visitor
     * @param script     script
     * @param visitorArg visitor arg
     * @param info       archetype info
     * @param <T>        visitor argument type
     */
    public static <T> void walk(Node.Visitor<T> visitor, Script script, T visitorArg, ArchetypeInfo info) {
        OptimizedWalker<T> optimizedWalker = new OptimizedWalker<>(visitor, visitorArg, info);
        Walker.walk(optimizedWalker, script, null, optimizedWalker::resolveScriptPath);
    }

    private Path resolveScriptPath(Invocation invocation) {
        return invocations.get(invocation).scriptPath();
    }

    @Override
    public Node.VisitResult visitInvocation(Invocation invocation, Void arg) {
        Node invoked = invocations.get(invocation);
        if (isInlined(invoked)) {
            return Node.VisitResult.CONTINUE;
        }
        return visitAny(invocation, arg);
    }

    @Override
    public Node.VisitResult visitBlock(Block block, Void arg) {
        switch (block.kind()) {
            case SCRIPT:
            case METHOD:
                if (isInlined(block)) {
                    return Node.VisitResult.CONTINUE;
                }
                return block.accept(visitor, this.visitorArg);
            case INVOKE_DIR:
            case INVOKE:
                return Node.VisitResult.CONTINUE;
            default:
                return visitAny(block, arg);
        }
    }

    @Override
    public Node.VisitResult postVisitBlock(Block block, Void arg) {
        switch (block.kind()) {
            case SCRIPT:
            case METHOD:
                if (isInlined(block)) {
                    return Node.VisitResult.CONTINUE;
                }
                return block.acceptAfter(visitor, this.visitorArg);
            case INVOKE_DIR:
            case INVOKE:
                return Node.VisitResult.CONTINUE;
            default:
                return postVisitAny(block, arg);
        }
    }

    @Override
    public Node.VisitResult visitCondition(Condition condition, Void arg) {
        if (isSkipped(condition.then())) {
            return Node.VisitResult.SKIP_SUBTREE;
        }
        return condition.accept(visitor, this.visitorArg);
    }

    @Override
    public Node.VisitResult visitAny(Node node, Void arg) {
        if (isSkipped(node)) {
            return Node.VisitResult.SKIP_SUBTREE;
        }
        return node.accept(visitor, this.visitorArg);
    }

    @Override
    public Node.VisitResult postVisitAny(Node node, Void arg) {
        if (isSkipped(node)) {
            return Node.VisitResult.SKIP_SUBTREE;
        }
        return node.acceptAfter(visitor, this.visitorArg);
    }

    private boolean isInlined(Node node) {
        return inlined.contains(node);
    }

    // TODO skip variable and preset blocks that are not referenced
    private boolean isSkipped(Node node) {
        DeclaredBlock block = blocks.get(node);
        if (block != null) {
            Set<Node> blockNodes = nodes.get(block);
            return blockNodes == null || !blockNodes.contains(node);
        }
        return true;
    }

    private void optimize() {
        boolean completed = false;
        while (!completed) {
            Map<DeclaredBlock, Set<Invocation>> refsMap = Maps.reverse(invocations);
            Iterator<Map.Entry<DeclaredBlock, Set<Invocation>>> it = refsMap.entrySet().iterator();
            completed = true;
            while (it.hasNext()) {
                Map.Entry<DeclaredBlock, Set<Invocation>> entry = it.next();
                DeclaredBlock target = entry.getKey();
                Set<Invocation> refs = entry.getValue();
                if (!nodes.containsKey(target)) {
                    // target is empty
                    it.remove();
                    inlined.remove(target);
                    for (Invocation ref : refs) {
                        invocations.remove(ref);
                        DeclaredBlock block = blocks.get(ref);
                        if (block != null) {
                            Set<Node> enclosingNodes = nodes.get(block);
                            enclosingNodes.remove(ref);
                            if (enclosingNodes.isEmpty()) {
                                nodes.remove(block);
                                completed = false;
                            }
                        }
                    }
                } else if (refs.size() <= 1) {
                    // inline script
                    it.remove();
                    inlined.add(target);
                }
            }
        }
    }
}
