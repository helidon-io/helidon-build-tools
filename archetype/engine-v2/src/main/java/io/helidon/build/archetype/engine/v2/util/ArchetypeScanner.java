/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.DeclaredBlock;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Script;

/**
 * Archetype scanner.
 */
public final class ArchetypeScanner implements Node.Visitor<Void> {

    private static final Path NULL_PATH = Path.of("");

    private final Predicate<Node> predicate;
    private final Deque<DeclaredBlock> currentBlock = new ArrayDeque<>();
    private final Deque<Invocation> currentInvocation = new ArrayDeque<>();
    private final Deque<Path> currentDirectory = new ArrayDeque<>();
    private final Map<Invocation, DeclaredBlock> targets = new HashMap<>();
    private final Map<Node, DeclaredBlock> blocks = new HashMap<>();
    private final Map<DeclaredBlock, Set<Node>> nodes = new HashMap<>();

    // TODO record var references (presets, variables)
    private ArchetypeScanner(Script script, Predicate<Node> predicate) {
        this.predicate = predicate;
        this.currentBlock.push(script);
        pushd(script.scriptPath().getParent());
    }

    private void pushd(Path path) {
        currentDirectory.push(path != null ? path : NULL_PATH);
    }

    /**
     * Walk the given script and scan the archetype info.
     *
     * @param script    script
     * @param predicate node predicate
     * @return ArchetypeInfo
     * @see ClientPredicate
     */
    public static ArchetypeInfo scan(Script script, Predicate<Node> predicate) {
        ArchetypeScanner scanner = new ArchetypeScanner(script, predicate);
        Walker.walk(scanner, script, null, scanner.currentDirectory::peek);
        return new ArchetypeInfo(scanner.targets, scanner.blocks, scanner.nodes);
    }

    @Override
    public VisitResult visitInvocation(Invocation invocation, Void arg) {
        currentInvocation.push(invocation);
        addNode(invocation);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitBlock(Block block, Void arg) {
        if (!predicate.test(block)) {
            return VisitResult.SKIP_SUBTREE;
        }
        Path scriptPath = block.scriptPath();
        Block.Kind kind = block.kind();
        switch (kind) {
            case INVOKE_DIR:
                pushd(scriptPath.getParent());
                return VisitResult.CONTINUE;
            case INVOKE:
                return VisitResult.CONTINUE;
            case METHOD:
            case SCRIPT:
                if (!currentInvocation.isEmpty()) {
                    targets.putIfAbsent(currentInvocation.pop(), (DeclaredBlock) block);
                }
                currentBlock.push((DeclaredBlock) block);
                return VisitResult.CONTINUE;
            default:
        }
        if (!predicate.test(block)) {
            return VisitResult.SKIP_SUBTREE;
        }
        addNode(block);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitBlock(Block block, Void arg) {
        switch (block.kind()) {
            case INVOKE_DIR:
                currentDirectory.pop();
                break;
            case SCRIPT:
            case METHOD:
                currentBlock.pop();
                break;
            default:
        }
        if (!predicate.test(block)) {
            return VisitResult.SKIP_SUBTREE;
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitCondition(Condition condition, Void arg) {
        Node then = condition.then();
        if (then instanceof Block) {
            if (!predicate.test(then)) {
                return VisitResult.SKIP_SUBTREE;
            }
        }
        addNode(condition);
        return VisitResult.CONTINUE;
    }

    private void addNode(Node node) {
        DeclaredBlock block = currentBlock.peek();
        blocks.put(node, block);
        nodes.computeIfAbsent(block, s -> new HashSet<>()).add(node);
    }
}
