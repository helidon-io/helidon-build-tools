/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.helidon.build.common.FileUtils.ensureDirectory;

/**
 * Generate or copy a file to a given target location.
 */
final class FileTask extends StagingTask {

    static final String ELEMENT_NAME = "file";

    private final String content;
    private final String source;

    FileTask(ActionIterators iterators, List<TextAction> nested, Map<String, String> attrs, String content) {
        super(ELEMENT_NAME, nested, iterators, attrs);
        this.content = content;
        this.source = attrs.get("source");
    }

    /**
     * Get the source.
     *
     * @return source, may be {@code null}
     */
    String source() {
        return source;
    }

    /**
     * Get the content.
     *
     * @return content, may be {@code null}
     */
    String content() {
        return content;
    }

    @Override
    @SuppressWarnings("unchecked")
    List<TextAction> tasks() {
        return (List<TextAction>) super.tasks();
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        String resolvedTarget = resolveVar(target(), vars);
        String resolvedSource = resolveVar(source, vars);
        String resolvedContent = resolveVar(content, vars);
        Path targetFile = dir.resolve(resolvedTarget).normalize();
        ensureDirectory(targetFile.getParent());
        if (resolvedSource != null && !resolvedSource.isEmpty()) {
            Path sourceFile = ctx.resolve(resolvedSource);
            if (!Files.exists(sourceFile)) {
                throw new IllegalStateException(sourceFile + " does not exist");
            }
            ctx.logInfo("Copying %s to %s", sourceFile, targetFile);
            Files.copy(sourceFile, targetFile);
        } else {
            Files.createFile(targetFile);
            if (resolvedContent != null && !resolvedContent.isEmpty()) {
                Files.writeString(targetFile, resolvedContent);
            } else {
                try (BufferedWriter writer = Files.newBufferedWriter(targetFile)) {
                    for (TextAction task : tasks()) {
                        writer.write(task.text());
                    }
                }
            }
        }
    }
}
