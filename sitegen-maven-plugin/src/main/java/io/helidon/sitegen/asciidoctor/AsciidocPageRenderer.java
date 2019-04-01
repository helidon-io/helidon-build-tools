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

import java.io.File;
import java.util.Map;

import io.helidon.sitegen.Page;
import io.helidon.sitegen.PageRenderer;
import io.helidon.sitegen.RenderingContext;
import io.helidon.sitegen.SiteEngine;

import static io.helidon.common.CollectionsHelper.mapOf;
import static io.helidon.sitegen.Helper.asString;
import static io.helidon.sitegen.Helper.checkNonNull;
import static io.helidon.sitegen.Helper.checkNonNullNonEmpty;
import static io.helidon.sitegen.Helper.checkValidDir;
import static io.helidon.sitegen.Page.Metadata;

/**
 * Implementation of a {@link PageRenderer} for asciidoc documents.
 *
 * @author rgrecour
 */
public class AsciidocPageRenderer implements PageRenderer {

    /**
     * Constant for the asciidoc file extension.
     */
    public static final String ADOC_EXT = "adoc";
    private final String backendName;

    /**
     * Create a new instance of {@link AsciidocPageRenderer}.
     * @param backendName the name of the backend
     */
    public AsciidocPageRenderer(String backendName) {
        this.backendName = backendName;
    }

    @Override
    public void process(Page page, RenderingContext ctx, File pagesdir, String ext) {
        checkNonNull(page, "page");
        checkNonNull(ctx, "ctx");
        checkValidDir(pagesdir, "pagesdir");
        checkNonNullNonEmpty(ext, "ext");
        SiteEngine siteEngine = SiteEngine.get(backendName);
        File target = new File(pagesdir, page.getTargetPath() + "." + ext);
        siteEngine.asciidoc().render(page, ctx, target,
                mapOf("page", page,
                      "pages", ctx.getPages()));
    }

    @Override
    public Metadata readMetadata(File source) {
        checkNonNull(source, "source");
        SiteEngine siteEngine = SiteEngine.get(backendName);
        Map<String, Object> docHeader = siteEngine
                .asciidoc().readDocumentHeader(source);
        return new Metadata(
                asString(docHeader.get("description")),
                asString(docHeader.get("keywords")),
                asString(docHeader.get("h1")),
                asString(docHeader.get("doctitle")));
    }
}
