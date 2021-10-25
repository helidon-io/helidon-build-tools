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

package io.helidon.build.archetype.engine.v2.spi;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import io.helidon.build.archetype.engine.v2.ast.Block;

/**
 * Template support.
 */
public interface TemplateSupport {

    /**
     * Render a template.
     *
     * @param template     template to render
     * @param templateName name of the template
     * @param charset      charset for the written characters
     * @param target       path to target file to create
     * @param extraScope   extra scope, may be {@code null}
     */
    void render(InputStream template, String templateName, Charset charset, OutputStream target, Block extraScope);
}
