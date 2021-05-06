/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.freemarker;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.TemplateDirectiveModel;

/**
 * A {@link TemplateSession} instance is an object used to share state across
 * multiple template rendering operations.
 *
 * This is especially useful for asciidoc rendering where many templates are
 * recursively invoked to render a single document.
 */
public class TemplateSession {

    private final Map<String, TemplateDirectiveModel> directives = new HashMap<>();
    private final SearchIndexDirective searchIndexDirective = new SearchIndexDirective();
    private final VueBindingsDirective vueBindingsDirective = new VueBindingsDirective();
    private final CustomLayoutDirective customLayoutDirective = new CustomLayoutDirective();

    /**
     * Create a new TemplateSession instance.
     */
    public TemplateSession() {
        directives.put("searchIndex", searchIndexDirective);
        directives.put("vueBindings", vueBindingsDirective);
        directives.put("customLayout", customLayoutDirective);
    }

    /**
     * Get the directives for this session.
     *
     * @return the list of directive
     */
    public Map<String, TemplateDirectiveModel> getDirectives() {
        return directives;
    }

    /**
     * Get the search index directive of this session.
     *
     * @return the search index directive
     */
    public SearchIndexDirective getSearchIndex() {
        return searchIndexDirective;
    }

    /**
     * Get the vue bindings directive of this session.
     *
     * @return the vue binding directive
     */
    public VueBindingsDirective getVueBindings(){
        return vueBindingsDirective;
    }

    /**
     * Get the custom layout directive of this session.
     *
     * @return the custom layout directive
     */
    public CustomLayoutDirective getCustomLayouts(){
        return customLayoutDirective;
    }
}
