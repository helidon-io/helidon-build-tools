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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.nio.file.Path;

import io.helidon.build.test.TestFiles;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.SubstitutionVariables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static io.helidon.build.util.SubstitutionVariables.systemPropertyOrEnvVarSource;

/**
 * Base class for init command tests
 */
class InitBaseTest extends MetadataCommandTest {

    private final Path targetDir = TestFiles.targetDir();
    private Path projectDir;
    private String projectName;
    private String packageName;

    @BeforeEach
    public void beforeEach() {
        startMetadataAccess(false, false);
    }

    @AfterEach
    public void afterEach() throws IOException {
        stopMetadataAccess();
        if (projectDir != null) {
            FileUtils.deleteDirectory(projectDir);
            System.out.println("Directory " + projectDir + " deleted");
        }
    }

    protected void init(String flavor, String archetypeName) throws Exception {
        SubstitutionVariables substitutions = SubstitutionVariables.of(systemPropertyOrEnvVarSource(), key -> {
            switch (key.toLowerCase()) {
                case "init_flavor":
                    return flavor;
                case "init_archetype":
                    return archetypeName;
                default:
                    return null;
            }
        });
        projectName = userConfig().defaultProjectName(substitutions);
        packageName = userConfig().defaultPackageName(substitutions);
        projectDir = targetDir.resolve(projectName);
    }

    protected Path targetDir() {
        return targetDir;
    }

    protected Path projectDir() {
        return projectDir;
    }

    protected String projectName() {
        return projectName;
    }

    protected String packageName() {
        return packageName;
    }
}
