/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.stager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generate or copy a file to a given target location.
 */
final class FileTask extends StagingTask {

    private final String content;
    private final String source;

    FileTask(List<Map<String, List<String>>> iterators, String target, String content, String source) {
        super(iterators, target);
        this.content = content;
        this.source = source;
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
    public String content() {
        return content;
    }

    @Override
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        String resolvedTarget = resolveVar(target(), variables);
        String resolvedSource = resolveVar(source, variables);
        String resolvedContent = resolveVar(content, variables);
        Path targetFile = dir.resolve(resolvedTarget);
        if (resolvedSource != null && !resolvedSource.isEmpty()) {
            Path sourceFile = dir.resolve(resolvedSource);
            if (!Files.exists(sourceFile)) {
                throw new IllegalStateException(sourceFile + " does not exist");
            }
            context.logInfo("Copying %s to %s", sourceFile, targetFile);
            Files.copy(sourceFile, targetFile);
        } else {
            Files.createFile(targetFile);
            if (resolvedContent != null && !resolvedContent.isEmpty()) {
                Files.writeString(targetFile, resolvedContent);
            }
        }
    }
}
