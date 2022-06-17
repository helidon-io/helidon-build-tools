/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

import java.util.stream.Collectors;

import io.helidon.build.maven.sitegen.Context;
import io.helidon.build.maven.sitegen.RenderingException;
import io.helidon.build.maven.sitegen.models.Link;
import io.helidon.build.maven.sitegen.models.Page;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.PhraseNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.helidon.build.maven.sitegen.Site.Options.STRICT_IMAGES;
import static java.util.Objects.requireNonNull;

/**
 * Link hash model.
 */
public final class HelperHashModel implements TemplateHashModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelperHashModel.class);

    private final ObjectWrapper objectWrapper;
    private volatile Page page;

    /**
     * Create a new instance.
     *
     * @param objectWrapper object wrapper
     */
    HelperHashModel(ObjectWrapper objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    private void setPage(ContentNode node) {
        page = (Page) requireNonNull(node.getDocument().getAttribute("page"), "page is null!");
    }

    /**
     * Image uri.
     *
     * @param node         node
     * @param declaredPath declared path
     * @return imageUri
     */
    public String imageUri(ContentNode node, String declaredPath) {
        if (node != null) {
            setPage(node);
            String imageUri = node.imageUri(declaredPath);
            if (!imageUri.contains("://")) {
                Context ctx = Context.get();
                String target = ctx.outputDir().resolve(imageUri).normalize().toString();
                // not a URI, validate the path...
                if (!ctx.resolvedAssets().contains(target)) {
                    Boolean strict = ctx.option(STRICT_IMAGES, Boolean.class).orElse(true);
                    String msg = String.format("Image not found! path: %s, document: %s", target, page.source());
                    if (strict) {
                        throw new RenderingException(msg);
                    }
                    LOGGER.warn(msg);
                }
            }
            return imageUri;
        }
        return null;
    }

    /**
     * Create a new link helper.
     *
     * @param node the node representing the link
     * @return the link helper or {@code null} if the provided node is {@code null}
     */
    public Link link(PhraseNode node) {
        if (node != null) {
            setPage(node);
            return Link.builder()
                       .page(page)
                       .path((String) node.getAttribute("path"))
                       .refId((String) node.getAttribute("refid"))
                       .fragment((String) node.getAttribute("fragment"))
                       .title((String) node.getAttribute("title"))
                       .target(node.getTarget())
                       .window((String) node.getAttribute("window"))
                       .options(node.getAttributes()
                                    .keySet()
                                    .stream()
                                    .filter(k -> k.endsWith("-option"))
                                    .map(k -> k.substring(0, k.length() - "-option".length()))
                                    .collect(Collectors.toList()))
                       .type(node.getType())
                       .text(node.getText())
                       .role(node.getRole())
                       .id(node.getId())
                       .build();
        }
        return null;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        // return method model if method name found for key
        if (SimpleMethodModel.hasMethodWithName(this, key)) {
            return new SimpleMethodModel(objectWrapper, this, key);
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
