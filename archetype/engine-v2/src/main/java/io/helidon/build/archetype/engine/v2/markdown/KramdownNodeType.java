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
import java.util.Optional;

/**
 * Factory to create new instances of inheritors of the {@code KramdownNode}.
 */
enum KramdownNodeType {

    /**
     * SimpleTextKramdownNode with {@code primary} style.
     */
    PRIMARY("primary") {
        @Override
        public KramdownNode instance(String content) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("style", "background-color:#cce5ff; color:#004085; border-color:#b8daff;");
            return new SimpleTextKramdownNode(PRIMARY, content, "div", attributes);
        }
    },
    /**
     * SimpleTextKramdownNode with {@code secondary} style.
     */
    SECONDARY("secondary") {
        @Override
        public KramdownNode instance(String content) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("style", "color: #383d41; background-color: #e2e3e5; border-color: #d6d8db;");
            return new SimpleTextKramdownNode(SECONDARY, content, "div", attributes);
        }
    },
    /**
     * SimpleTextKramdownNode with {@code error} style.
     */
    ERROR("error") {
        @Override
        public KramdownNode instance(String content) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("style", "color: #721c24; background-color: #f8d7da; border-color: #f5c6cb;");
            return new SimpleTextKramdownNode(ERROR, content, "div", attributes);
        }
    },
    /**
     * SimpleTextKramdownNode with {@code info} style.
     */
    INFO("info") {
        @Override
        public KramdownNode instance(String content) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("style", "color: #0c5460; background-color: #d1ecf1; border-color: #bee5eb;");
            return new SimpleTextKramdownNode(INFO, content, "div", attributes);
        }
    },
    /**
     * SimpleTextKramdownNode with {@code success} style.
     */
    SUCCESS("success") {
        @Override
        public KramdownNode instance(String content) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("style", "color: #155724; background-color: #d4edda; border-color: #c3e6cb;");
            return new SimpleTextKramdownNode(SUCCESS, content, "div", attributes);
        }
    },
    /**
     * SimpleTextKramdownNode with {@code warning} style.
     */
    WARNING("warning") {
        @Override
        public KramdownNode instance(String content) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("style", "color: #856404; background-color: #fff3cd; border-color: #ffeeba;");
            return new SimpleTextKramdownNode(WARNING, content, "div", attributes);
        }
    };

    private static final Map<String, KramdownNodeType> KRAMDOWN_MAP;

    KramdownNodeType(String name) {
        this.name = name;
    }

    private final String name;

    static {
        KRAMDOWN_MAP = new HashMap<>();
        for (KramdownNodeType type : KramdownNodeType.values()) {
            KRAMDOWN_MAP.put(type.name, type);
        }
    }

    /**
     * Create a new instance of the inheritor of the KramdownNode.
     *
     * @param content content of the AST node.
     * @return KramdownNode
     */
    public abstract KramdownNode instance(String content);

    /**
     * Get KramdownNodeType by the name.
     *
     * @param name name
     * @return KramdownNodeType
     */
    public static KramdownNodeType find(String name) {
        return Optional
                .ofNullable(KRAMDOWN_MAP.get(name))
                .orElseThrow(() ->
                        new IllegalArgumentException(String.format("KramdownNode with name %s does not exist", name)));
    }
}
