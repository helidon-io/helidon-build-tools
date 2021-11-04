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

import java.util.Map;

interface HtmlNodeRendererContext {

    /**
     * @param url to be encoded
     * @return an encoded URL (depending on the configuration)
     */
    String encodeUrl(String url);

    /**
     * Let extensions modify the HTML tag attributes.
     *
     * @param node the node for which the attributes are applied
     * @param tagName the HTML tag name that these attributes are for (e.g. {@code h1}, {@code pre}, {@code code}).
     * @param attributes the attributes that were calculated by the renderer
     * @return the extended attributes with added/updated/removed entries
     */
    Map<String, String> extendAttributes(Node node, String tagName, Map<String, String> attributes);

    /**
     * @return the HTML writer to use
     */
    HtmlWriter getWriter();

    /**
     * Render the specified node and its children using the configured renderers. This should be used to render child
     * nodes; be careful not to pass the node that is being rendered, that would result in an endless loop.
     *
     * @param node the node to render
     */
    void render(Node node);

    /**
     * @return Sanitizer to use for securing {@link Link} href
     */
    UrlSanitizer urlSanitizer();
}
