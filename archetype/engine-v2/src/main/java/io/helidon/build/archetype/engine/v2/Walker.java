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

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Invocation.ScriptInvocation;
import io.helidon.build.archetype.engine.v2.ast.Method;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Script;

import static io.helidon.build.common.FileUtils.pathOf;

/**
 * Block walker.
 * Provides facility to do depth-first traversal of the AST tree.
 *
 * @param <A> visitor argument type
 */
public final class Walker<A> {

    private final Deque<Invocation> callStack = new ArrayDeque<>();
    private final Deque<Node> stack = new ArrayDeque<>();
    private final Deque<Node> parents = new ArrayDeque<>();
    private final Node.Visitor<A> visitor;
    private final Function<ScriptInvocation, Path> scriptResolver;
    private boolean traversing;

    /**
     * Traverse the given block node with the specified visitor and argument.
     *
     * @param visitor      visitor
     * @param block        node to traverse, must be non {@code null}
     * @param arg          visitor argument
     * @param pathResolver invocation path resolver
     * @param <A>          visitor argument type
     * @throws NullPointerException if block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static <A> void walk(Node.Visitor<A> visitor, Block block, A arg, Function<ScriptInvocation, Path> pathResolver) {
        new Walker<>(visitor, pathResolver).walk(block, arg);
    }

    /**
     * Traverse the given block node with the specified visitor and argument.
     *
     * @param visitor visitor
     * @param block   node to traverse, must be non {@code null}
     * @param arg     visitor argument
     * @param cwd     cwd supplier
     * @param <A>     visitor argument type
     * @throws NullPointerException if block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static <A> void walk(Node.Visitor<A> visitor, Block block, A arg, Supplier<Path> cwd) {
        new Walker<>(visitor, i -> resolveScript(cwd.get(), i)).walk(block, arg);
    }

    /**
     * Traverse the given block node with the specified visitor and argument.
     *
     * @param visitor visitor
     * @param block   node to traverse, must be non {@code null}
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @throws NullPointerException if block is {@code null}
     * @throws InvocationException  if an exception is thrown while traversing
     */
    public static <A> void walk(Node.Visitor<A> visitor, Block block, A arg) {
        new Walker<>(visitor, i -> resolveScript(i.scriptPath().getParent(), i)).walk(block, arg);
    }

    private static Path resolveScript(Path dir, ScriptInvocation invocation) {
        return dir != null ? dir.resolve(invocation.src()) : null;
    }

    private Walker(Node.Visitor<A> visitor, Function<ScriptInvocation, Path> scriptResolver) {
        this.visitor = new DelegateVisitor(visitor);
        this.scriptResolver = scriptResolver;
    }

    private VisitResult accept(Node node, A arg, boolean before) {
        try {
            return before ? node.accept(visitor, arg) : node.acceptAfter(visitor, arg);
        } catch (Throwable ex) {
            throw new InvocationException(callStack, node, ex);
        }
    }

    private void walk(Block block, A arg) {
        Objects.requireNonNull(block, "block is null");
        VisitResult result = accept(block, arg, true);
        if (result != VisitResult.CONTINUE || block.children().isEmpty()) {
            return;
        }
        while (!stack.isEmpty()) {
            traversing = false;
            Node node = stack.peek();
            Node parent = parents.peek();
            int parentId = parent != null ? parent.uid() : 0;
            int nodeId = node.uid();
            if (nodeId != parentId) {
                result = accept(node, arg, true);
            } else {
                if (node instanceof Block) {
                    result = accept(node, arg, false);
                }
                parentId = parents.pop().uid();
            }
            if (!traversing) {
                stack.pop();
                if (result == VisitResult.SKIP_SIBLINGS) {
                    skipSiblings(parentId);
                } else if (result == VisitResult.TERMINATE) {
                    return;
                }
            }
        }
        accept(block, arg, false);
    }

    private void skipSiblings(int parentId) {
        while (!stack.isEmpty()) {
            Node n = stack.peek();
            if (n instanceof Block) {
                if (n.uid() == parentId) {
                    break;
                }
                stack.pop();
            }
        }
    }

    private Script resolveScript(ScriptInvocation invocation) {
        Path scriptPath;
        if (invocation.src() != null) {
            scriptPath = scriptResolver.apply(invocation);
        } else {
            String url = invocation.url();
            scriptPath = pathOf(URI.create(url), this.getClass().getClassLoader());
        }
        if (scriptPath == null) {
            throw new IllegalStateException("Unresolved script: " + invocation);
        }
        return invocation.loader().get(scriptPath);
    }

    private Method resolveMethod(Invocation.MethodInvocation invocation) {
        String methodName = invocation.method();
        Method method = invocation.script().methods().get(methodName);
        if (method != null) {
            return method;
        }
        for (Node node : callStack) {
            method = node.script().methods().get(methodName);
            if (method != null) {
                return method;
            }
        }
        throw new IllegalStateException("Unresolved method: " + invocation);
    }

    private class DelegateVisitor implements Node.Visitor<A>, Invocation.Visitor<A> {

        private final Node.Visitor<A> delegate;

        DelegateVisitor(Node.Visitor<A> delegate) {
            this.delegate = delegate;
        }

        @Override
        public VisitResult visitCondition(Condition condition, A arg) {
            VisitResult result = delegate.visitCondition(condition, arg);
            if (result == VisitResult.CONTINUE) {
                stack.push(condition.then());
                parents.push(condition);
                traversing = true;
            }
            return result;
        }

        @Override
        public VisitResult visitInvocation(Invocation invocation, A arg) {
            VisitResult result = delegate.visitInvocation(invocation, arg);
            if (result != VisitResult.SKIP_SUBTREE && result != VisitResult.TERMINATE) {
                invocation.accept((Invocation.Visitor<A>) this, arg);
            }
            return result;
        }

        @Override
        public VisitResult visitScriptInvocation(ScriptInvocation invocation, A arg) {
            invoke(invocation, resolveScript(invocation));
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult visitMethodInvocation(Invocation.MethodInvocation invocation, A arg) {
            invoke(invocation, resolveMethod(invocation));
            return VisitResult.CONTINUE;
        }

        private void invoke(Invocation invocation, Block target) {
            if (invocation.kind() == Invocation.Kind.EXEC) {
                stack.push(target.wrap(Block.Kind.INVOKE_DIR));
            } else {
                stack.push(target.wrap(Block.Kind.INVOKE));
            }
            callStack.push(invocation);
            parents.push(invocation);
            traversing = true;
        }

        @Override
        public VisitResult visitBlock(Block block, A arg) {
            if (block.kind().equals(Block.Kind.METHODS)) {
                return VisitResult.SKIP_SUBTREE;
            }
            VisitResult result = delegate.visitBlock(block, arg);
            if (result != VisitResult.TERMINATE) {
                List<Node> children = block.children();
                int childrenSize = children.size();
                if (result != VisitResult.SKIP_SUBTREE && childrenSize > 0) {
                    ListIterator<Node> it = children.listIterator(childrenSize);
                    while (it.hasPrevious()) {
                        stack.push(it.previous());
                    }
                    parents.push(block);
                    traversing = true;
                } else {
                    result = visitor.postVisitBlock(block, arg);
                }
            }
            return result;
        }

        @Override
        public VisitResult postVisitBlock(Block block, A arg) {
            switch (block.kind()) {
                case INVOKE:
                case INVOKE_DIR:
                    callStack.pop();
                    break;
                default:
            }
            return delegate.postVisitBlock(block, arg);
        }

        @Override
        public VisitResult visitAny(Node node, A arg) {
            return delegate.visitAny(node, arg);
        }

        @Override
        public VisitResult postVisitAny(Node node, A arg) {
            return delegate.visitAny(node, arg);
        }
    }
}
