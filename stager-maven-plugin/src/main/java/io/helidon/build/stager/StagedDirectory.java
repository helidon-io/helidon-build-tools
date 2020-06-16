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
import java.util.HashMap;
import java.util.List;

/**
 * Generate a directory using a set of tasks.
 */
final class StagedDirectory {

    private final List<StagingTask> tasks;
    private final String target;

    StagedDirectory(String target, List<StagingTask> tasks) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target is required");
        }
        this.target = target;
        this.tasks = tasks == null ? List.of() : tasks;
    }

    /**
     * Get the target.
     *
     * @return target, never {@code null}
     */
    String target() {
        return target;
    }

    /**
     * Nested tasks.
     *
     * @return tasks, never {@code null}
     */
    List<StagingTask> tasks() {
        return tasks;
    }

    /**
     * Execute the nested tasks to render the staged directory.
     *
     * @param context staging context
     * @throws IOException if an IO error occurs
     */
    void execute(StagingContext context, Path dir) throws IOException {
        Path targetDir = dir.resolve(target);
        Files.createDirectories(targetDir);
        for (StagingTask task : tasks) {
            task.execute(context, targetDir, new HashMap<>());
        }
    }
}
