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

package io.helidon.build.dev;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A build source and output directory.
 */
public class BuildComponent {
    private final AtomicReference<Project> project;
    private final BuildRoot sourceRoot;
    private final BuildRoot outputRoot;
    private final List<BuildStep> buildSteps;

    /**
     * Returns a new build component.
     *
     * @param sourceRoot The source root.
     * @param outputRoot The output root.
     * @param buildSteps The build steps.
     * @return The build component.
     */
    public static BuildComponent createBuildComponent(BuildRoot sourceRoot,
                                                      BuildRoot outputRoot,
                                                      BuildStep... buildSteps) {
        return new BuildComponent(sourceRoot, outputRoot, Arrays.asList(buildSteps));
    }

    private BuildComponent(BuildRoot sourceRoot, BuildRoot outputRoot, List<BuildStep> buildSteps) {
        this.project = new AtomicReference<>();
        this.sourceRoot = requireNonNull(sourceRoot).component(this);
        this.outputRoot = requireNonNull(outputRoot).component(this);
        this.buildSteps = requireNonNull(buildSteps);
    }

    /**
     * Returns the project containing this component..
     *
     * @return The project.
     */
    public Project project() {
        return requireNonNull(project.get());
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
     * Execute the build step for the given changed files only.
     *
     * @param changes The changes.
     * @param stdOut A consumer for stdout.
     * @param stdErr A consumer for stderr.
     * @throws Exception on error.
     */
    public void incrementalBuild(BuildRoot.Changes changes,
                                 Consumer<String> stdOut,
                                 Consumer<String> stdErr) throws Exception {
        if (changes.root().component() == this) {
            for (BuildStep step : buildSteps) {
                step.incrementalBuild(changes, stdOut, stdErr);
            }
        } else {
            throw new IllegalArgumentException("Changed component != this");
        }
    }

    /**
     * Updates the components.
     */
    public void update() {
        sourceRoot().update();
        outputRoot().update();
    }

    void project(Project project) {
        this.project.set(project);
    }
}
