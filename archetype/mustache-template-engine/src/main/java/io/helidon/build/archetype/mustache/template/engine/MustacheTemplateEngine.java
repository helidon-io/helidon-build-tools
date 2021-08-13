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

package io.helidon.build.archetype.mustache.template.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.helidon.build.archetype.engine.spi.TemplateEngine;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * Implementation of the {@link TemplateEngine} for Mustache.
 */
public class MustacheTemplateEngine implements TemplateEngine {

    @Override
    public String getName() {
        return "mustache";
    }

    @Override
    public void render(File templateFile, String templateName, Path target, Object scope) throws IOException {
        MustacheFactory factory = new DefaultMustacheFactory();
        Mustache mustache = factory.compile(new InputStreamReader(new FileInputStream(templateFile)), templateName);
        Files.createDirectories(target.getParent());
        try (Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            mustache.execute(writer, scope).flush();
        }
    }
}
