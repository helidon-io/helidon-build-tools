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

package io.helidon.sitegen.asciidoctor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import io.helidon.sitegen.SiteEngine;
import io.helidon.sitegen.freemarker.FreemarkerEngine;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.converter.AbstractConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An asciidoctor converter that supports backends implemented with Freemarker.
 *
 * The Freemarker templates are loaded from classpath, see {@link io.helidon.sitegen.freemarker.TemplateLoader}
 *
 * @author rgrecour
 */
public class AsciidocConverter extends AbstractConverter<String> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AsciidocConverter.class);

    private final FreemarkerEngine templateEngine;

    /**
     * Create a new instance of {@link AsciidocConverter}.
     * @param backend the backend name
     * @param opts the asciidoctor invocation options
     */
    public AsciidocConverter(String backend, Map<String, Object> opts) {
        super(backend, opts);
        templateEngine = SiteEngine.get(backend).freemarker();
    }

    @Override
    public String convert(ContentNode node,
                          String transform,
                          Map<Object, Object> opts) {

        if (node != null && node.getNodeName() != null) {
            LOGGER.debug("Rendering node: {}", node);
            String templateName;
            if (node.equals(node.getDocument())) {
                templateName = "document";
            } else if (node.isBlock()) {
                templateName = "block_" + node.getNodeName();
            } else {
                templateName = node.getNodeName();
            }
            return templateEngine.renderString(templateName, node);
        } else {
            return "";
        }
    }

    @Override
    public void write(String output, OutputStream out) throws IOException {
        out.write(output.getBytes());
    }
}
