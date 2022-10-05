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

package io.helidon.build.maven.sitegen;

import java.nio.file.Path;

import io.helidon.build.maven.sitegen.models.Page;
import io.helidon.build.maven.sitegen.models.Page.Metadata;

/**
 * Render and read metadata for specific type of document.
 */
public interface PageRenderer {

    /**
     * Test if this renderer supports a given file.
     *
     * @param source source path
     * @return {@code true} if supported, {@code false} otherwise
     */
    boolean supports(Path source);

    /**
     * Read a given document metadata.
     *
     * @param source the file to read the metadata from
     * @return the {@link Metadata} instance, never {@code null}
     */
    Metadata readMetadata(Path source);

    /**
     * Process the rendering of a given document.
     *
     * @param page      the {@link Page} representing the document
     * @param ctx       the context representing the site processing invocation
     * @param outputDir the directory where to generate the rendered pages
     * @param ext       the file extension to use for the rendered pages
     */
    void process(Page page, Context ctx, Path outputDir, String ext);
}
