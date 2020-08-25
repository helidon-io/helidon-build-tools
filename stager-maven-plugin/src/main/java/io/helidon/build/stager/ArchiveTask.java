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
import java.util.List;
import java.util.Map;

/**
 * Generate an archive with a set of tasks.
 */
class ArchiveTask extends StagingTask {

    static final String ELEMENT_NAME = "archive";

    private final List<StagingAction> actions;
    private final String includes;
    private final String excludes;

    ArchiveTask(ActionIterators iterators, List<StagingAction> actions, String target, String includes, String excludes) {
        super(iterators, target);
        this.actions = actions == null ? List.of() : actions;
        this.includes = includes;
        this.excludes = excludes;
    }

    @Override
    public String elementName() {
        return ELEMENT_NAME;
    }

    /**
     * Get the nested tasks.
     *
     * @return tasks, never {@code null}
     */
    List<StagingAction> tasks() {
        return actions;
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
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        Path stageDir = context.createTempDirectory("archive-task");
        for (StagingAction actions : actions) {
            actions.execute(context, stageDir, variables);
        }
        String resolvedTarget = resolveVar(target(), variables);
        String resolvedIncludes = resolveVar(includes, variables);
        String resolvedExcludes = resolveVar(excludes, variables);
        context.logInfo("Archiving %s to %s", stageDir, resolvedTarget);
        context.archive(stageDir, dir.resolve(resolvedTarget), resolvedIncludes, resolvedExcludes);
    }

    @Override
    public String describe(Path dir, Map<String, String> variables) {
        return ELEMENT_NAME + "{"
                + "target=" + resolveVar(target(), variables)
                + ", includes='" + resolveVar(includes, variables) + '\''
                + ", excludes='" + resolveVar(excludes, variables) + '\''
                + '}';
    }
}
