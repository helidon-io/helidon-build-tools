/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen.asciidoctor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import io.helidon.build.sitegen.SiteEngine;
import io.helidon.build.sitegen.freemarker.FreemarkerEngine;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.converter.AbstractConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.helidon.build.sitegen.asciidoctor.CardBlockProcessor.BLOCKLINK_TEXT;

/**
 * An asciidoctor converter that supports backends implemented with Freemarker.
 *
 * The Freemarker templates are loaded from classpath, see {@link io.helidon.build.sitegen.freemarker.TemplateLoader}
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
            String templateName;
            if (node.equals(node.getDocument())) {
                templateName = "document";
            } else if (node.isBlock()) {
                templateName = "block_" + node.getNodeName();
            } else {
                // detect phrase node for generated block links
                if (node.getNodeName().equals("inline_anchor")
                        && BLOCKLINK_TEXT.equals(((PhraseNode) node).getText())) {
                    // store the link model as an attribute in the corresponding
                    // block
                    node.getParent().getParent().getAttributes()
                            .put("_link", (PhraseNode) node);
                    // the template for the block is responsible for rendering
                    // the link, discard the output
                    return "";
                }
                templateName = node.getNodeName();
            }
            LOGGER.debug("Rendering node: {}", node);
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
