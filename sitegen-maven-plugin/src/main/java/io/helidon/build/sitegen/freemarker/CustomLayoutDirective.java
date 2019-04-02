/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import io.helidon.build.sitegen.Page;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.asciidoctor.ast.ContentNode;

/**
 * A freemarker directive to accumulate custom layout mappings.
 *
 * @author rgrecour
 */
public class CustomLayoutDirective implements TemplateDirectiveModel {

    private final Map<String, String> mappings = new HashMap<>();

    @Override
    public void execute(Environment env,
                        Map params,
                        TemplateModel[] loopVars,
                        TemplateDirectiveBody body)
            throws TemplateException, IOException {

        TemplateModel dataModel = env.getDataModel().get("this");
        if (!(dataModel instanceof ContentNodeHashModel)) {
            throw new TemplateModelException(
                    "Data model is not a ContentNodeHashModel");
        }
        ContentNode node = ((ContentNodeHashModel) dataModel).getContentNode();
        if (node == null) {
            throw new TemplateModelException("'this' has a null content-node");
        }

        Object page = node.getDocument().getAttribute("page");
        if (page == null || !(page instanceof Page)) {
            throw new TemplateModelException("Unable to get page instance");
        }

        if (body == null) {
            throw new TemplateModelException("Body is null");
        }
        StringWriter writer = new StringWriter();
        body.render(writer);
        mappings.put(((Page) page).getSourcePath(), writer.toString());
    }

    /**
     * Get the stored mappings.
     * @return {@code Map<String, String>}, never {@code null}
     */
    public Map<String, String> getMappings() {
        return mappings;
    }
}
