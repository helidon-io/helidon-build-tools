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

import java.util.HashMap;
import java.util.Map;

/**
 * Simple kramdown extension for markdown, that contains as child AST node an {@link Text} node.
 */
class SimpleTextKramdownNode extends KramdownNode {

    private final Map<String, String> attributes = new HashMap<>();
    private final String tag;

    /**
     * Create a new instance.
     * @param type type of the node
     * @param content content of the node
     * @param tag surrounding tag
     * @param attributes attributes of the tag
     */
    SimpleTextKramdownNode(KramdownNodeType type, String content, String tag, Map<String, String> attributes) {
        super(type);
        Text text = new Text(content);
        this.appendChild(text);
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    <T> void accept(KramdownVisitor<T> visitor, T arg) {
        visitor.visit(this, arg);
    }
}
