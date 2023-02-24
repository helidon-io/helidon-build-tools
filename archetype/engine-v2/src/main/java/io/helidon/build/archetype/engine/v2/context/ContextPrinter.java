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
package io.helidon.build.archetype.engine.v2.context;

import java.util.List;
import java.util.function.Function;

import io.helidon.build.archetype.engine.v2.context.ContextEdge.Visitor;

/**
 * Context printer.
 */
public final class ContextPrinter implements Visitor {

    private final StringBuilder sb;
    private final Function<ContextValue, String> valuePrinter;

    private ContextPrinter(StringBuilder sb, Function<ContextValue, String> valuePrinter) {
        this.sb = sb;
        this.valuePrinter = valuePrinter;
    }

    private static boolean isLastChild(ContextNode node) {
        ContextNode parent = node.parent0();
        List<ContextNode> children = parent.edge().children();
        return children.indexOf(node) == children.size() - 1;
    }

    private static String printValue(ContextValue value) {
        return value == null ? "null" : value.value().unwrap() + " (" + value.kind() + ')';
    }

    @Override
    public void visit(ContextEdge edge) {
        ContextNode node = edge.node();
        ContextNode parent = node.parent0();
        if (parent != null) {
            int startIndex = sb.length();
            ContextNode n = parent;
            while (n.parent0() != null) {
                sb.append(' ');
                if (isLastChild(n)) {
                    sb.append(' ');
                } else {
                    sb.append('|');
                }
                n = n.parent0();
            }
            sb.append(' ');
            if (isLastChild(node)) {
                sb.append('\\');
            } else {
                sb.append('+');
            }
            sb.append("- ");
            String indent = " ".repeat((sb.length() + 1) - startIndex);
            sb.append(node.id());
            sb.append(System.lineSeparator());
            for (ContextEdge variation : edge.variations()) {
                sb.append(indent);
                sb.append(": ").append(valuePrinter.apply(variation.value()));
                sb.append(System.lineSeparator());
            }
        }
    }

    /**
     * Pretty print the given context tree.
     *
     * @param scope        context tree node
     * @param valuePrinter value printer function
     * @return String
     */
    public static String print(ContextScope scope, Function<ContextValue, String> valuePrinter) {
        StringBuilder sb = new StringBuilder();
        scope.visitEdges(new ContextPrinter(sb, valuePrinter), false);
        return sb.toString();
    }

    /**
     * Pretty print the given context tree.
     *
     * @param scope context tree node
     * @return String
     */
    public static String print(ContextScope scope) {
        StringBuilder sb = new StringBuilder();
        scope.visitEdges(new ContextPrinter(sb, ContextPrinter::printValue), false);
        return sb.toString();
    }
}
