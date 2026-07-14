/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.common.Strings;

import com.github.mustachejava.DefaultMustacheFactory;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Render a mustache template.
 */
final class TemplateTask extends StagingTask {

    private final String source;
    private final TemplateModel model;

    TemplateTask(ActionIterators iterators, Map<String, String> attrs, TemplateModel model) {
        super("template", null, iterators, attrs);
        this.source = Strings.requireValid(attrs.get("source"), "source is required");
        this.model = model == null ? TemplateModel.empty() : model;
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        String resolvedTarget = resolveVar(target(), vars);
        String resolvedSource = resolveVar(source, vars);
        Path sourceFile = ctx.resolve(resolvedSource);
        if (!Files.exists(sourceFile)) {
            throw new IllegalStateException(sourceFile + " does not exist");
        }
        Path targetFile = dir.resolve(resolvedTarget).normalize();
        ctx.ensureDirectory(targetFile.getParent());
        try (Reader reader = Files.newBufferedReader(sourceFile);
                Writer writer = Files.newBufferedWriter(targetFile, CREATE, TRUNCATE_EXISTING)) {
            new DefaultMustacheFactory().compile(reader, resolvedSource)
                    .execute(writer, model.resolve(vars).root())
                    .flush();
        }
    }

    String source() {
        return source;
    }

    TemplateModel model() {
        return model;
    }
}
