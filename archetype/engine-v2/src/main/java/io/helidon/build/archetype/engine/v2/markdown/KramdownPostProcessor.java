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
package io.helidon.build.archetype.engine.v2.markdown;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostProcessor for the {@code KramdownNode}.
 */
class KramdownPostProcessor implements PostProcessor {

    private static final Pattern KRAMDOWN_PATTERN = Pattern.compile("\\{::(?<name>[a-z,-]+)}(?<content>[^{}]+)\\{:/}");
    private final NodeVisitor visitor = new NodeVisitor();

    @Override
    public Node process(Node node) {
        node.accept(visitor);
        return node;
    }

    private void processText(Text textNode) {
        Node lastNode = textNode;
        String literal = textNode.getLiteral();

        Matcher matcher = KRAMDOWN_PATTERN.matcher(textNode.getLiteral());
        while (matcher.find()) {
            String name = matcher.group("name");
            String content = matcher.group("content");
            String group = matcher.group();

            String beforeText = literal.substring(0, literal.indexOf(group));
            if (!beforeText.isEmpty()) {
                Text beforeNode = new Text(beforeText);
                insertNode(beforeNode, lastNode);
                lastNode = beforeNode;
            }

            KramdownNode kramdownNode = KramdownNodeType.find(name).instance(content);
            insertNode(kramdownNode, lastNode);
            lastNode = kramdownNode;

            literal = literal.substring(literal.indexOf(group) + group.length());
            if (!literal.isEmpty()) {
                Text afterNode = new Text(literal);
                insertNode(afterNode, lastNode);
                lastNode = afterNode;
            }
        }

        if (lastNode != textNode) {
            textNode.unlink();
        }
    }

    private static Node insertNode(Node node, Node insertAfterNode) {
        insertAfterNode.insertAfter(node);
        return node;
    }

    private class NodeVisitor implements Visitor {

        @Override
        public void visit(Text text) {
            processText(text);
        }

    }
}
