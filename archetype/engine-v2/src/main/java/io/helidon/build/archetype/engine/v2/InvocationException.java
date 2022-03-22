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

import java.util.Deque;
import java.util.Iterator;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Node;

/**
 * Invocation exception.
 */
public class InvocationException extends RuntimeException {

    /**
     * Create a new invocation exception.
     *
     * @param callStack invocation call stack
     * @param node      current node
     * @param cause     cause
     */
    InvocationException(Deque<Invocation> callStack, Node node, Throwable cause) {
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

    private StackTraceElement[] stackTrace(Deque<Invocation> callStack, Node node) {
        StackTraceElement[] original = getStackTrace();
        int size = callStack.size() + 1;
        StackTraceElement[] stackTrace = new StackTraceElement[size + original.length];
        stackTrace[0] = stackTraceElement(node);
        if (!callStack.isEmpty()) {
            Iterator<Invocation> it = callStack.iterator();
            for (int i = 1; it.hasNext(); i++) {
                stackTrace[i] = stackTraceElement(it.next());
            }
        }
        System.arraycopy(original, 0, stackTrace, size, original.length);
        return stackTrace;
    }

    private static StackTraceElement stackTraceElement(Node node) {
        String fileName = node.scriptPath().toString();
        int lineNumber = node.position().lineNumber();
        return new StackTraceElement("archetype", method(node), fileName, lineNumber);
    }

    private static String method(Node node) {
        if (node instanceof Condition) {
            return "condition";
        }
        if (node instanceof Invocation) {
            return ((Invocation) node).kind().name().toLowerCase();
        }
        if (node instanceof Block) {
            return ((Block) node).kind().name().toLowerCase();
        }
        return "?";
    }
}
