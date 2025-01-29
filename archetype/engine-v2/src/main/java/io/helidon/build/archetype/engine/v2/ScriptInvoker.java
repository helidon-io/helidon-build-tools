/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
import java.util.Iterator;
import java.util.List;

import io.helidon.build.archetype.engine.v2.Context.ValueKind;

import static io.helidon.build.common.FileUtils.pathOf;

/**
 * Script invoker.
 */
public class ScriptInvoker implements Node.Visitor {

    private final Deque<Node> callStack = new ArrayDeque<>();
    private final List<Node.Visitor> visitors;
    private final Context ctx;

    /**
     * Traverse the given node with the specified visitors.
     *
     * @param node     node
     * @param context  context
     * @param visitors visitors
     * @throws InvocationException if an exception is thrown while traversing
     */
    public static void invoke(Node node, Context context, Node.Visitor... visitors) {
        new ScriptInvoker(context, visitors).invoke(node);
    }

    /**
     * Create a new instance.
     *
     * @param ctx  context
     * @param visitors visitors
     */
    public ScriptInvoker(Context ctx, Node.Visitor... visitors) {
        this(ctx, List.of(visitors));
    }

    /**
     * Create a new instance.
     *
     * @param ctx  context
     * @param visitors visitors
     */
    public ScriptInvoker(Context ctx, List<Node.Visitor> visitors) {
        this.visitors = visitors;
        this.ctx = ctx;
    }

    /**
     * Invoke.
     *
     * @param node node
     */
    public void invoke(Node node) {
        try {
            node.visit(this);
        } catch (Throwable ex) {
            throw new InvocationException(callStack, node, ex);
        }
    }

    @Override
    public boolean visit(Node node) {
        switch (node.kind()) {
            case CONDITION:
                return node.expression().eval(s -> ctx.scope().get(s).value());
            case EXEC:
                callStack.push(node);
                Node target = resolveScript(node);
                ctx.pushCwd(target.script().path().getParent());
                target.visit(this);
                break;
            case SOURCE:
                callStack.push(node);
                resolveScript(node).visit(this);
                break;
            case CALL:
                callStack.push(node);
                resolveMethod(node).visit(this);
                break;
            case PRESET_LIST:
            case PRESET_BOOLEAN:
            case PRESET_ENUM:
            case PRESET_TEXT:
                ctx.scope().getOrCreate(
                                node.attribute("path").getString(),
                                node.attribute("model").asBoolean().orElse(false))
                        .value(node.value(), ValueKind.PRESET);
                break;
            case VARIABLE_LIST:
            case VARIABLE_BOOLEAN:
            case VARIABLE_ENUM:
            case VARIABLE_TEXT:
                ctx.scope().getOrCreate(
                                node.attribute("path").getString(),
                                node.attribute("model").asBoolean().orElse(false))
                        .value(node.value(), ValueKind.LOCAL_VAR);
                break;
            default:
                boolean traverse = true;
                for (Node.Visitor visitor : visitors) {
                    if (!visitor.visit(node)) {
                        traverse = false;
                    }
                }
                return traverse;
        }
        return true;
    }

    @Override
    public void postVisit(Node node) {
        switch (node.kind()) {
            case CALL:
            case SOURCE:
                callStack.pop();
                break;
            case EXEC:
                ctx.popCwd();
                callStack.pop();
                break;
            default:
                for (Node.Visitor visitor : visitors) {
                    visitor.postVisit(node);
                }
        }
    }

    /**
     * Get the context.
     *
     * @return Context
     */
    protected Context context() {
        return ctx;
    }

    /**
     * Load a script.
     *
     * @param loader loader
     * @param source source
     * @return Node
     */
    protected Node load(Script.Loader loader, Script.Source source) {
        return loader.get(source);
    }

    private Node resolveScript(Node inv) {
        Path path;
        String src = inv.attribute("src").asString().orElse(null);
        if (src != null) {
            Path cwd = ctx.cwd();
            if (cwd != null) {
                path = cwd.resolve(src);
            } else {
                throw new IllegalStateException("Unresolved script: " + src);
            }
        } else {
            String url = inv.attribute("url").getString();
            path = pathOf(URI.create(url), this.getClass().getClassLoader());
        }
        return load(inv.script().loader(), Script.Source.of(path));
    }

    private Node resolveMethod(Node node) {
        String methodName = node.attribute("method").getString();
        Node method = node.script().methods().get(methodName);
        if (method != null) {
            return method;
        }
        for (Node e : callStack) {
            method = e.script().methods().get(methodName);
            if (method != null) {
                return method;
            }
        }
        throw new IllegalStateException("Unresolved method: " + node);
    }

    private static StackTraceElement stackTraceElement(Node node) {
        String methodName = node.kind().name().toLowerCase();
        Node.Location location = node.location();
        return new StackTraceElement("archetype", methodName, location.fileName(), location.lineNumber());
    }

    /**
     * Invocation exception.
     */
    public static class InvocationException extends RuntimeException {

        /**
         * Create a new invocation exception.
         *
         * @param callStack invocation call stack
         * @param node      current node
         * @param cause     cause
         */
        InvocationException(Deque<Node> callStack, Node node, Throwable cause) {
            super(message(cause), cause, false, true);
            setStackTrace(stackTrace(callStack, node));
        }

        private static String message(Throwable cause) {
            String msg = "Invocation error";
            String causeMsg = cause.getMessage();
            if (causeMsg != null) {
                msg += ": " + causeMsg;
            }
            return msg;
        }

        private StackTraceElement[] stackTrace(Deque<Node> callStack, Node node) {
            StackTraceElement[] original = getStackTrace();
            int size = callStack.size() + 1;
            StackTraceElement[] stackTrace = new StackTraceElement[size + original.length];
            if (!callStack.isEmpty()) {
                Iterator<Node> it = callStack.iterator();
                for (int i = 0; it.hasNext(); i++) {
                    stackTrace[i] = stackTraceElement(it.next());
                }
            }
            stackTrace[size - 1] = stackTraceElement(node);
            System.arraycopy(original, 0, stackTrace, size, original.length);
            return stackTrace;
        }
    }
}
