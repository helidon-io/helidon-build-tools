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

    /**
     * Execute the task sequentially with iterations.
     *
     * @param context   staging context
     * @param dir       stage directory
     * @param variables variables for the current iteration
     * @throws IOException if an IO error occurs
     * @throws IOException if an IO error occurs
     */
    @Override
    public void execute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        if (iterators() == null || iterators().isEmpty()) {
            doExecute(context, dir, variables);
            return;
        }
        for (ActionIterator iterator : iterators()) {
            iterator.baseVariable(variables);
            while (iterator.hasNext()) {
                doExecute(context, dir, iterator.next());
            }
        }
    }

    @Override
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        Path stageDir = context.createTempDirectory("archive-task");
        for (StagingAction action : actions) {
            action.execute(context, stageDir, variables);
        }
        Container.submit(() -> archive(context, stageDir, dir, variables));
    }

    private CompletionStage<Void> archive(StagingContext context, Path source, Path targetDir, Map<String, String> variables) {
        String resolvedTarget = resolveVar(target(), variables);
        String resolvedIncludes = resolveVar(includes, variables);
        String resolvedExcludes = resolveVar(excludes, variables);
        context.logInfo("Archiving %s to %s", source, resolvedTarget);
        context.archive(source, targetDir.resolve(resolvedTarget), resolvedIncludes, resolvedExcludes);
        return CompletableFuture.completedFuture(null);
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
