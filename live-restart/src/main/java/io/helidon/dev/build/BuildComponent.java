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
import java.util.List;

/**
 * A build source and output directory.
 */
public class BuildComponent {
    private final Project project;
    private final BuildRoot sourceRoot;
    private final BuildRoot outputRoot;
    private final List<BuildStep> buildSteps;

    /**
     * Returns a new build component.
     *
     * @param project The project to which this component belongs.
     * @param sourceRoot The source root.
     * @param outputRoot The output root.
     * @param buildSteps The build steps.
     */
    public static BuildComponent createBuildComponent(Project project,
                                                      BuildRoot sourceRoot,
                                                      BuildRoot outputRoot,
                                                      BuildStep... buildSteps) {
        return new BuildComponent(project, sourceRoot, outputRoot, Arrays.asList(buildSteps));
    }

    private BuildComponent(Project project, BuildRoot sourceRoot, BuildRoot outputRoot, List<BuildStep> buildSteps) {
        this.project = project;
        this.sourceRoot = sourceRoot.component(this);
        this.outputRoot = outputRoot.component(this);
        this.buildSteps = buildSteps;
    }

    /**
     * Returns the project containing this component..
     *
     * @return The project.
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the source root.
     *
     * @return The root.
     */
    public BuildRoot sourceRoot() {
        return sourceRoot;
    }

    /**
     * Returns the output root.
     *
     * @return The root.
     */
    public BuildRoot outputRoot() {
        return outputRoot;
    }

    /**
     * Returns the list of build steps.
     *
     * @return The steps.
     */
    public List<BuildStep> buildSteps() {
        return buildSteps;
    }

    /**
     * Updates the components.
     */
    public void update() {
        sourceRoot().update();
        outputRoot().update();
    }
}
