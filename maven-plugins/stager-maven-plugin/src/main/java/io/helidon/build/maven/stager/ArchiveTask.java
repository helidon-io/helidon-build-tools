/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.util.concurrent.CompletionStage;

/**
 * Generate an archive with a set of tasks.
 */
@SuppressWarnings("unused")
class ArchiveTask extends StagingTask {

    static final String ELEMENT_NAME = "archive";

    private final String includes;
    private final String excludes;

    ArchiveTask(ActionIterators iterators, List<StagingAction> nested, Map<String, String> attrs) {
        super(ELEMENT_NAME, nested, iterators, attrs);
        this.includes = attrs.get("includes");
        this.excludes = attrs.get("excludes");
    }

    /**
     * Get the includes.
     *
     * @return includes, may be {@code null}
     */
    String includes() {
        return includes;
    }

    /**
     * Get the excludes.
     *
     * @return excludes, may be {@code null}
     */
    String excludes() {
        return excludes;
    }

    @Override
    public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
        try {
            Path stageDir = ctx.createTempDirectory("archive-task");
            return super.execute(ctx, stageDir, vars)
                        .thenRun(() -> archive(ctx, stageDir, dir, vars));
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Override
    protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        return CompletableFuture.completedFuture(null);
    }

    private void archive(StagingContext context, Path source, Path targetDir, Map<String, String> variables) {
        String resolvedTarget = resolveVar(target(), variables);
        String resolvedIncludes = resolveVar(includes, variables);
        String resolvedExcludes = resolveVar(excludes, variables);
        context.logInfo("Archiving %s to %s", source, resolvedTarget);
        context.archive(source, targetDir.resolve(resolvedTarget), resolvedIncludes, resolvedExcludes);
    }

    @Override
    public String describe(Path dir, Map<String, String> vars) {
        return ELEMENT_NAME + "{"
                + "target=" + resolveVar(target(), vars)
                + ", includes='" + resolveVar(includes, vars) + '\''
                + ", excludes='" + resolveVar(excludes, vars) + '\''
                + '}';
    }
}
