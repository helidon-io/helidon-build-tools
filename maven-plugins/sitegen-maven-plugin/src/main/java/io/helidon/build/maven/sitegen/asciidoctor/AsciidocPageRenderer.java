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

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import io.helidon.build.common.FileUtils;
import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.Context;
import io.helidon.build.maven.sitegen.PageRenderer;
import io.helidon.build.maven.sitegen.models.Page;

import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.Strings.requireValid;
import static io.helidon.build.maven.sitegen.models.Page.Metadata;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of a {@link PageRenderer} for asciidoc documents.
 */
public final class AsciidocPageRenderer implements PageRenderer {

    private final AsciidocEngine asciidocEngine;

    /**
     * Create a new page renderer.
     *
     * @param asciidocEngine asciidocEngine
     */
    public AsciidocPageRenderer(AsciidocEngine asciidocEngine) {
        this.asciidocEngine = Objects.requireNonNull(asciidocEngine, "asciidocEngine is null!");
    }

    @Override
    public void process(Page page, Context ctx, Path outputDir, String ext) {
        requireNonNull(page, "page is null!");
        requireNonNull(ctx, "ctx is null!");
        requireValid(ext, "ext is invalid!");
        Path target = requireDirectory(outputDir).resolve(page.target() + "." + ext);
        asciidocEngine.render(page, ctx, target);
    }

    @Override
    public boolean supports(Path source) {
        return "adoc".equals(FileUtils.fileExt(source));
    }

    @Override
    public Metadata readMetadata(Path source) {
        requireNonNull(source, "source is null!");
        Map<String, Object> docHeader = asciidocEngine.readDocumentHeader(source);
        return Metadata.create(Config.create(docHeader, Map.of()));
    }
}
