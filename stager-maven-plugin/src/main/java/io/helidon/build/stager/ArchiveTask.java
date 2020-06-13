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
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generate an archive with a set of tasks.
 */
final class ArchiveTask extends StagingTask {

    private final List<StagingTask> tasks;
    private final String includes;
    private final String excludes;

    ArchiveTask(List<Map<String, String>> iterators,
                List<StagingTask> tasks,
                String target,
                String includes,
                String excludes) {

        super(iterators, target);
        this.tasks = tasks == null ? Collections.emptyList() : tasks;
        this.includes = includes;
        this.excludes = excludes;
    }

    @Override
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        Path stageDir = context.createTempDirectory("archive-task");
        for (StagingTask task : tasks) {
            task.execute(context, stageDir, variables);
        }
        String resolvedTarget = resolveVar(target(), variables);
        String resolvedIncludes = resolveVar(includes, variables);
        String resolvedExcludes = resolveVar(excludes, variables);
        context.logInfo("Archiving %s to %s", stageDir, resolvedTarget);
        context.archive(stageDir, dir.resolve(resolvedTarget), resolvedIncludes, resolvedExcludes);
    }
}
