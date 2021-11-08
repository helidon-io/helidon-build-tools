/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.common.markdown;

/**
 * Extension that provides support for the non-standard markdown syntax (kramdown extension).
 * See <a href="https://kramdown.gettalong.org/syntax.html#extensions>Kramdown extensions</a>.
 * <p>
 * Create it with {@link #create()} and then configure it on the builders
 * ({@link Parser.Builder#extensions(Iterable)},
 * {@link HtmlRenderer.Builder#extensions(Iterable)}).
 * </p>
 */
public class KramdownExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    private KramdownExtension() {
    }

    /**
     * Create a new instance of the class.
     *
     * @return Extension
     */
    public static Extension create() {
        return new KramdownExtension();
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.postProcessor(new KramdownPostProcessor());
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder) {
        rendererBuilder.nodeRendererFactory(KramdownNodeRenderer::new);
    }
}
