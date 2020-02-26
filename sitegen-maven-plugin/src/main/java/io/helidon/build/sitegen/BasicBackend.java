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

package io.helidon.build.sitegen;

import java.util.Map;

import io.helidon.build.sitegen.asciidoctor.AsciidocPageRenderer;

import static io.helidon.build.sitegen.asciidoctor.AsciidocPageRenderer.ADOC_EXT;
import static io.helidon.common.CollectionsHelper.mapOf;

/**
 * A basic backend implementation.
 */
public class BasicBackend extends Backend {

    /**
     * The basic backend name.
     */
    public static final String BACKEND_NAME = "basic";
    private final Map<String, PageRenderer> pageRenderers;

    /**
     * Create a new instance of {@link BasicBackend}.
     */
    public BasicBackend() {
        super(BACKEND_NAME);
        this.pageRenderers = mapOf(
                ADOC_EXT, new AsciidocPageRenderer(BACKEND_NAME)
        );
    }

    @Override
    public Map<String, PageRenderer> pageRenderers() {
        return pageRenderers;
    }

    @Override
    public void generate(RenderingContext ctx) {
        ctx.processPages(ctx.getOutputdir(), "html");
        ctx.copyStaticAssets();
    }
}
