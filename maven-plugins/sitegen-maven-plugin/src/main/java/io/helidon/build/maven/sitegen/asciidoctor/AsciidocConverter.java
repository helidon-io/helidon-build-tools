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

package io.helidon.build.maven.sitegen.asciidoctor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.sitegen.Context;
import io.helidon.build.maven.sitegen.RenderingException;
import io.helidon.build.maven.sitegen.SiteEngine;
import io.helidon.build.maven.sitegen.freemarker.FreemarkerEngine;
import io.helidon.build.maven.sitegen.freemarker.TemplateLoader;
import io.helidon.build.maven.sitegen.models.Page;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Cursor;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.converter.AbstractConverter;

import static io.helidon.build.maven.sitegen.asciidoctor.CardBlockProcessor.BLOCK_LINK_TEXT;

/**
 * An asciidoctor converter that supports backends implemented with Freemarker.
 * <p>
 * The Freemarker templates are loaded from classpath, see {@link TemplateLoader}
 */
public class AsciidocConverter extends AbstractConverter<String> {

    private final FreemarkerEngine templateEngine;
    private final Deque<String> frames = new ArrayDeque<>();
    private final Context ctx;
    private volatile Page page;
    private volatile Document document;

    /**
     * Create a new instance of {@link AsciidocConverter}.
     *
     * @param backend the backend name
     * @param opts    the asciidoctor invocation options
     */
    public AsciidocConverter(String backend, Map<String, Object> opts) {
        super(backend, opts);
        ctx = Context.get();
        SiteEngine engine = ctx.site().engine();
        templateEngine = engine.freemarker();
        engine.asciidoc().converter(this);
    }

    @Override
    public String convert(ContentNode node, String transform, Map<Object, Object> opts) {
        try {
            document = node.getDocument();
            page = (Page) Objects.requireNonNull(document.getAttribute("page"), "page is null!");
            frames.push(sourceLocation(node));
            return convert(node);
        } finally {
            frames.pop();
        }
    }

    private String convert(ContentNode node) {
        if (node == null || node.getNodeName() == null) {
            return "";
        }
        String templateName;
        if (node.equals(document)) {
            templateName = "document";
        } else if (node.isBlock()) {
            templateName = "block_" + node.getNodeName();
        } else {
            // detect phrase node for generated block links
            if (node.getNodeName().equals("inline_anchor")
                    && BLOCK_LINK_TEXT.equals(((PhraseNode) node).getText())) {

                // store the link model as an attribute in the corresponding block
                node.getParent()
                    .getParent()
                    .getAttributes()
                    .put("_link", node);
                // the template for the block is responsible for rendering
                // the link, discard the output
                return "";
            }
            templateName = node.getNodeName();
        }
        Log.debug("Rendering node: " + node);
        try {
            return templateEngine.renderString(templateName, node);
        } catch (RenderingException ex) {
            if (ex instanceof AsciidocRenderingException) {
                throw ex;
            }
            ctx.error(new AsciidocRenderingException(ex.getMessage(), frames, ex));
            return "";
        }
    }

    @Override
    public void write(String output, OutputStream out) throws IOException {
        out.write(output.getBytes());
    }

    /**
     * Get the frames.
     *
     * @return frames
     */
    Deque<String> frames() {
        return frames;
    }

    private String sourceLocation(ContentNode node) {
        Cursor location = cursor(node);
        if (location == null) {
            return "\tat ?:?";
        }
        Path sourcePath = ctx.resolvePath(page, location.getPath());
        String source = ctx.sourceDir().relativize(sourcePath).toString();
        return String.format("\tat %s:%s", source, location.getLineNumber());
    }

    private static Cursor cursor(ContentNode node) {
        while (node != null) {
            if (node instanceof StructuralNode) {
                Cursor sourceLocation = ((StructuralNode) node).getSourceLocation();
                if (sourceLocation != null) {
                    return sourceLocation;
                }
            }
            node = node.getParent();
        }
        return null;
    }

}
