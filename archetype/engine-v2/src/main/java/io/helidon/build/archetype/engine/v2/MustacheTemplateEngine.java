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

package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * Implementation of the {@link TemplateEngine} for Mustache.
 */
public class MustacheTemplateEngine implements TemplateEngine {

    @Override
    public String name() {
        return "mustache";
    }

    @Override
    public void render(
            InputStream template, String templateName, Charset charset, OutputStream target, Object scope
    ) throws IOException {
        MustacheFactory factory = new DefaultMustacheFactory();
        Mustache mustache = factory.compile(new InputStreamReader(template), templateName);
        try (Writer writer = new OutputStreamWriter(target, charset)) {
            mustache.execute(writer, scope).flush();
        }
    }
}
