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

package io.helidon.build.common.markdown;

import java.util.HashMap;
import java.util.Map;

class NodeRendererMap {

    private final Map<Class<? extends Node>, NodeRenderer> renderers = new HashMap<>(32);

    public void add(NodeRenderer nodeRenderer) {
        for (Class<? extends Node> nodeType : nodeRenderer.nodeTypes()) {
            renderers.put(nodeType, nodeRenderer);
        }
    }

    public void render(Node node) {
        NodeRenderer nodeRenderer = renderers.get(node.getClass());
        if (nodeRenderer != null) {
            nodeRenderer.render(node);
        }
    }
}
