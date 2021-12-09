/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
    InvocationException(Deque<Node> callStack, Node node, Throwable cause) {
        super(String.format("Invocation error: %s\n%s", cause.getMessage(), printStackTrace(callStack, node)), cause);
    }

    private static String printStackTrace(Deque<Node> callStack, Node node) {
        StringBuilder sb = new StringBuilder(printStackItem(node));
        if (!callStack.isEmpty()) {
            sb.append("\n");
            Iterator<Node> it = callStack.iterator();
            while (it.hasNext()) {
                sb.append(printStackItem(it.next()));
                if (it.hasNext()) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static String printStackItem(Node node) {
        return "\tat " + node.scriptPath() + ":" + node.position();
    }
}
