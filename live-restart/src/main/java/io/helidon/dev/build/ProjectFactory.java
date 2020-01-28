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

import java.nio.file.Path;

import io.helidon.dev.build.maven.MavenProject;

import static io.helidon.build.util.FileUtils.assertDir;

/**
 * A factory for {@link Project} instances.
 */
public class ProjectFactory {

    /**
     * Returns a new project instance for the given root directory.
     *
     * @param projectRootDir The root directory.
     * @return The project.
     */
    public static Project createProject(Path projectRootDir) {
        assertDir(projectRootDir);
        if (MavenProject.isMavenProject(projectRootDir)) {
            return new MavenProject(projectRootDir);
        }
        throw new IllegalArgumentException("Unknown project type at " + projectRootDir);
    }
}
