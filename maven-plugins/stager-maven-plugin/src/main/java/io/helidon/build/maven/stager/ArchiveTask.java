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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;

/**
 * Generate an archive with a set of tasks.
 */
@SuppressWarnings("unused")
class ArchiveTask extends StagingTask {

    private final String target;
    private final String includes;
    private final String excludes;

    ArchiveTask(XMLElement element, List<StagingTask> tasks) {
        super(element, tasks);
        this.target = Strings.requireValid(element.attribute("target", null), "target is required");
        this.includes = element.attribute("includes", null);
        this.excludes = element.attribute("excludes", null);
    }

    @Override
    public String toString() {
        return "ArchiveTask{"
               + "target='" + target + '\''
               + ", includes='" + includes + '\''
               + ", excludes='" + excludes + '\''
               + '}';
    }

    @Override
    protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> execTask(StagingContext ctx, Path dir, Map<String, String> vars) {
        var resolvedTarget = resolveVar(target, vars);
        var targetFile = dir.resolve(resolvedTarget).normalize();
        Path stageDir;
        try {
            stageDir = ctx.createTempDirectory("archive-task");
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(ex);
        }
        ctx.logInfo("Creating archive %s", resolvedTarget);
        return super.execTask(ctx, stageDir, vars)
                .thenRun(() -> archive(ctx, stageDir, targetFile, vars));
    }

    private void archive(StagingContext ctx, Path source, Path targetFile, Map<String, String> variables) {
        var resolvedIncludes = resolveVar(includes, variables);
        var resolvedExcludes = resolveVar(excludes, variables);
        ctx.archive(source, targetFile, resolvedIncludes, resolvedExcludes);
    }
}
