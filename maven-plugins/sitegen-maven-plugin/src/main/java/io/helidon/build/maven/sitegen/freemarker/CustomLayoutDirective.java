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
import java.util.HashMap;
import java.util.Map;

import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import org.asciidoctor.ast.ContentNode;

/**
 * A freemarker directive to accumulate custom layout mappings.
 */
public final class CustomLayoutDirective extends ContentNodeDirective {

    private final Map<String, String> mappings = new HashMap<>();

    @Override
    void doExecute(ContentNode node, Map<?, ?> params, TemplateDirectiveBody body)
            throws TemplateException, IOException {

        if (body == null) {
            throw new TemplateModelException("Body is null");
        }
        StringWriter writer = new StringWriter();
        body.render(writer);
        mappings.put(page(node).source(), writer.toString());
    }

    /**
     * Get the stored mappings.
     *
     * @return {@code Map<String, String>}, never {@code null}
     */
    public Map<String, String> mappings() {
        return mappings;
    }
}
