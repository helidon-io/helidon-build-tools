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

import java.util.Collections;
import java.util.Set;

/**
 * Render {@code KramdownNode} as html.
 */
class KramdownNodeRenderer implements NodeRenderer {

    private final HtmlWriter html;
    private final KramdownVisitor<HtmlWriter> kramdownVisitor;

    /**
     * Create a new instance.
     *
     * @param context context
     */
    KramdownNodeRenderer(HtmlNodeRendererContext context) {
        html = context.getWriter();
        kramdownVisitor = new KramdownHtmlRenderer(context);
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return Collections.singleton(SimpleTextKramdownNode.class);
    }

    @Override
    public void render(Node node) {
        ((KramdownNode) node).accept(kramdownVisitor, html);
    }
}
