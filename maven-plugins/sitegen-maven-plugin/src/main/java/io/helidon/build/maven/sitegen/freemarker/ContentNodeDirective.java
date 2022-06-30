/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.util.Map;

import io.helidon.build.maven.sitegen.models.Page;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.asciidoctor.ast.ContentNode;

/**
 * Base class for {@link ContentNode} related directives.
 */
abstract class ContentNodeDirective implements TemplateDirectiveModel {

    @Override
    public final void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {

        TemplateModel dataModel = env.getDataModel().get("this");
        if (!(dataModel instanceof ContentNodeHashModel)) {
            throw new TemplateModelException("Data model is not a ContentNodeHashModel");
        }
        ContentNode node = ((ContentNodeHashModel) dataModel).getContentNode();
        if (node == null) {
            throw new TemplateModelException("'this' has a null content-node");
        }
        if (body == null) {
            throw new TemplateModelException("Body is null");
        }
        doExecute(node, params, body);
    }

    /**
     * Execute.
     *
     * @param node   content node
     * @param params params
     * @param body   directive body
     * @throws TemplateModelException if an error occurs
     */
    abstract void doExecute(ContentNode node, Map<?, ?> params, TemplateDirectiveBody body)
            throws TemplateException, IOException;

    /**
     * Derive the page from the given node.
     *
     * @param node node
     * @return page
     * @throws TemplateModelException if an error occurs
     */
    Page page(ContentNode node) throws TemplateModelException {
        Object page = node.getDocument().getAttribute("page");
        if (page instanceof Page) {
            return (Page) page;
        }
        throw new TemplateModelException("Unable to get page instance");
    }
}
