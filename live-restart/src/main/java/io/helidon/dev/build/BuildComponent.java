/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A build source and output directory.
 */
public class BuildComponent {
    private final ProjectDirectory sourceDir;
    private final ProjectDirectory outputDir;
    private final List<BuildStep> buildSteps;

    /**
     * Returns a new build component.
     *
     * @param sourceDir The source directory.
     * @param outputDir The output directory.
     * @param buildSteps The build steps.
     */
    public static BuildComponent createBuildComponent(ProjectDirectory sourceDir,
                                                      ProjectDirectory outputDir,
                                                      BuildStep... buildSteps) {
        return new BuildComponent(sourceDir, outputDir, Arrays.asList(buildSteps));
    }

    private BuildComponent(ProjectDirectory sourceDir, ProjectDirectory outputDir, List<BuildStep> buildSteps) {
        this.sourceDir = sourceDir;
        this.outputDir = outputDir;
        this.buildSteps = buildSteps;
    }

    /**
     * Returns the source directory.
     *
     * @return The directory.
     */
    public ProjectDirectory sourceDirectory() {
        return sourceDir;
    }

    /**
     * Returns the output directory.
     *
     * @return The directory.
     */
    public ProjectDirectory outputDirectory() {
        return outputDir;
    }

    /**
     * Execute the build.
     *
     * @return A list of build errors, empty on success.
     */
    public Collection<String> build() {
        for (final BuildStep step : buildSteps) {
            final List<String> errors = step.execute(this);
            if (!errors.isEmpty()) {
                return errors;
            }
        }
        return emptyList();
    }
}
