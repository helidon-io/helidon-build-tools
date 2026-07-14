/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Generate or copy a file to a given target location.
 */
final class FileTask extends StagingTask {

    private final String content;
    private final String source;

    FileTask(ActionIterators iterators, List<TextAction> nested, Map<String, String> attrs, String content) {
        super("file", nested, iterators, attrs);
        this.content = content;
        this.source = attrs.get("source");
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        String resolvedTarget = resolveVar(target(), vars);
        String resolvedSource = resolveVar(source, vars);
        String resolvedContent = resolveVar(content, vars);
        Path targetFile = dir.resolve(resolvedTarget).normalize();
        ctx.ensureDirectory(targetFile.getParent());
        if (resolvedSource != null && !resolvedSource.isEmpty()) {
            Path sourceFile = ctx.resolve(resolvedSource);
            if (!Files.exists(sourceFile)) {
                throw new IllegalStateException(sourceFile + " does not exist");
            }
            ctx.logInfo("Copying %s to %s", sourceFile, targetFile);
            Files.copy(sourceFile, targetFile, REPLACE_EXISTING);
        } else {
            if (resolvedContent != null && !resolvedContent.isEmpty()) {
                Files.writeString(targetFile, resolvedContent, CREATE, TRUNCATE_EXISTING);
            } else {
                try (BufferedWriter writer = Files.newBufferedWriter(targetFile, CREATE, TRUNCATE_EXISTING)) {
                    for (StagingAction task : tasks()) {
                        if (task instanceof TextAction textAction) {
                            writer.write(textAction.text(vars));
                        }
                    }
                }
            }
        }
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
}
