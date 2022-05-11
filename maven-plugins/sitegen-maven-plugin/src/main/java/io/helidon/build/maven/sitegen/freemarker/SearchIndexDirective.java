/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.helidon.build.maven.sitegen.models.Page;
import io.helidon.build.maven.sitegen.models.SearchEntry;

import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.StructuralNode;

/**
 * A freemarker directive to accumulate entries for the search index.
 */
public final class SearchIndexDirective extends ContentNodeDirective {

    private final List<SearchEntry> entries = new ArrayList<>();

    @Override
    void doExecute(ContentNode node, Map<?, ?> params, TemplateDirectiveBody body)
            throws TemplateException, IOException {

        String title = null;
        if (params.containsKey("title")) {
            Object titleParam = params.get("title");
            if (!(titleParam instanceof TemplateScalarModel)) {
                throw new TemplateModelException("The title parameter must be a string");
            }
            title = ((TemplateScalarModel) titleParam).getAsString();
        } else if (node instanceof StructuralNode) {
            title = stripHtmlMarkups(((StructuralNode) node).getTitle());
        }

        if (title == null) {
            throw new TemplateModelException("missing title");
        }

        Page page = page(node);
        StringWriter writer = new StringWriter();
        body.render(writer);
        SearchEntry entry = SearchEntry.create(page.target(), stripHtmlMarkups(writer.toString()), title);
        entries.add(entry);
    }

    // TODO write a unit test for this
    private static String stripHtmlMarkups(String content) {
        if (content == null) {
            return null;
        }
        return content.replaceAll("\\<.*?\\>", " ")
                      .replaceAll("\\\\n", "")
                      .replaceAll("\\s+", " ");
    }

    /**
     * Get the search index entries accumulated.
     *
     * @return the list of search index entries.
     */
    public List<SearchEntry> entries() {
        return entries;
    }
}
