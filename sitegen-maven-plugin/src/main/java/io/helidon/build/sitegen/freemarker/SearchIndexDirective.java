/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.helidon.build.sitegen.Page;
import io.helidon.build.sitegen.SearchEntry;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.StructuralNode;

/**
 * A freemarker directive to accumulate entries for the search index.
 */
public class SearchIndexDirective implements TemplateDirectiveModel {

    private final List<SearchEntry> entries = new ArrayList<>();

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

        String title = null;
        if (params.containsKey("title")) {
            Object titleParam = params.get("title");
            if (!(titleParam instanceof TemplateScalarModel)) {
                throw new TemplateModelException(
                        "The title parameter must be a string");
            }
            title = ((TemplateScalarModel) titleParam).getAsString();
        } else if (node instanceof StructuralNode) {
            title = stripHtmlMarkups(((StructuralNode) node).getTitle());
        }

        if (title == null) {
            throw new TemplateModelException("missing title");
        }

        Object pageAttr = node.getDocument().getAttribute("page");
        if (!(pageAttr instanceof Page)) {
            throw new TemplateModelException(
                    "document attribute page is not valid");
        }
        Page page = (Page) pageAttr;

        if (body == null) {
            throw new TemplateModelException("Body is null");
        }
        StringWriter writer = new StringWriter();
        body.render(writer);

        SearchEntry entry = new SearchEntry(
                page.getTargetPath(), stripHtmlMarkups(writer.toString()), title);
        entries.add(entry);
    }

    // TODO write a unit test for this
    private static String stripHtmlMarkups(String content){
        if (content == null) {
            return null;
        }
        return content
                .replaceAll("\\<.*?\\>", " ")
                .replaceAll("\\\\n", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * Get the search index entries accumulated.
     *
     * @return the list of search index entries.
     */
    public List<SearchEntry> getEntries() {
        return entries;
    }
}
