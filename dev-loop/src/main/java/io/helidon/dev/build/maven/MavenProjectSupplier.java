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

package io.helidon.dev.build.maven;

import java.util.List;

import io.helidon.dev.build.BuildExecutor;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectSupplier;

/**
 * A {@code ProjectSupplier} for Maven projects.
 */
public class MavenProjectSupplier implements ProjectSupplier {
    private static final List<String> CLEAN_BUILD_COMMAND = List.of("clean", "prepare-package", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of("prepare-package", "-DskipTests");

    @Override
    public Project get(BuildExecutor executor, boolean clean, int cycleNumber) throws Exception {
        if (clean) {
            executor.execute(CLEAN_BUILD_COMMAND);
        }
        if (!configurationExists()) {
            executor.execute(BUILD_COMMAND);
        }

        // TODO 1. read configuration
        //      2. construct result from configuration using Project.builder();

        throw new Error("not implemented");
    }

    private boolean configurationExists() {
        return false; // TODO
    }
}
