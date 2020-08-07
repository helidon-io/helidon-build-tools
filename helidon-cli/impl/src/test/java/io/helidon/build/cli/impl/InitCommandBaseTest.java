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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.test.TestFiles;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.SubstitutionVariables;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static io.helidon.build.cli.impl.InitCommand.DEFAULT_ARCHETYPE_NAME;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_FLAVOR;
import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.PomUtils.readPomModel;
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.util.SubstitutionVariables.systemPropertyOrEnvVarSource;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Base class for init command tests and other tests that require init
 */
class InitCommandBaseTest extends MetadataCommandTest {

    private final Path targetDir = TestFiles.targetDir();
    private File input;

    private String flavor;
    private String archetypeName;
    private String groupId;
    private String artifactId;
    private String packageName;
    private String projectName;
    private boolean buildProject;

    private Path projectDir;
    private Path sourceRoot;


    @BeforeEach
    public void beforeEach() {
        flavor = DEFAULT_FLAVOR;
        archetypeName = DEFAULT_ARCHETYPE_NAME;
    }

    @AfterEach
    public void afterEach() throws IOException {
        if (projectDir != null) {
            FileUtils.deleteDirectory(projectDir);
            System.out.println("Directory " + projectDir + " deleted");
        }
    }

    protected void flavor(String flavor) {
        this.flavor = flavor.toUpperCase();
    }

    protected void archetypeName(String archetypeName) {
        this.archetypeName = archetypeName;
    }

    protected void input(String inputFileName) {
        URL url = requireNonNull(getClass().getResource(inputFileName), inputFileName + "not found");
        this.input = new File(url.getFile());
    }

    protected void buildProject(boolean buildProject) {
        this.buildProject = buildProject;
    }

    protected void groupId(String groupId) {
        this.groupId = groupId;
    }

    protected void artifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    protected void packageName(String packageName) {
        this.packageName = packageName;
    }

    protected void projectName(String projectName) {
        this.projectName = projectName;
    }

    protected Path targetDir() {
        return targetDir;
    }

    protected void initArguments() {
        SubstitutionVariables substitutions = SubstitutionVariables.of(key -> {
            switch (key.toLowerCase()) {
                case "init_flavor":
                    return flavor.toLowerCase();
                case "init_archetype":
                    return archetypeName;
                default:
                    return null;
            }
        }, systemPropertyOrEnvVarSource());
        String projectNameArg = projectName;
        projectName = userConfig().projectName(projectName, artifactId, substitutions);
        groupId = groupId == null ? userConfig().defaultGroupId(substitutions) : groupId;
        artifactId = userConfig().artifactId(artifactId, projectNameArg, substitutions);
        packageName = packageName == null ? userConfig().defaultPackageName(substitutions) : packageName;
        projectDir = targetDir.resolve(projectName);
    }

    protected Path projectDir() {
        return projectDir;
    }

    protected void generate() throws Exception {
        initArguments();
        List<String> args = new ArrayList<>();
        args.add("init");
        args.add("--url");
        args.add(metadataUrl());
        if (input == null) {
            args.add("--batch");
        }
        args.add("--version");
        args.add(helidonTestVersion());
        if (!flavor.equals(DEFAULT_FLAVOR)) {
            args.add("--flavor");
            args.add(flavor);
        }
        if (!archetypeName.equals(DEFAULT_ARCHETYPE_NAME)) {
            args.add("--apptype");
            args.add(archetypeName);
        }
        if (groupId != null) {
            args.add("--groupId");
            args.add(groupId);
        }
        if (artifactId != null) {
            args.add("--artifactId");
            args.add(artifactId);
        }
        if (packageName != null) {
            args.add("--package");
            args.add(packageName);
        }
        if (projectName != null) {
            args.add("--name");
            args.add(projectName);
        }
        String[] argsArray = args.toArray(new String[]{});
        System.out.print("Executing with args ");
        args.forEach(a -> System.out.print(a + " "));
        System.out.println();

        // Execute and verify process exit code
        TestUtils.execWithDirAndInput(targetDir.toFile(), input, argsArray);
    }

    protected void assertValid() throws Exception {
        assertProjectExists();
        assertExpectedPom();
        assertPackageExists();
        assertSourceFilesExist();
        if (buildProject) {
            exec("build", "--project ", projectDir.toString());
            assertJarExists();
            assertProjectConfig();
        }
    }

    protected void assertJarExists() {
        assertFile(projectDir.resolve("target").resolve(artifactId + ".jar"));
    }

    private void assertProjectExists() {
        assertDir(projectDir);
    }

    private void assertExpectedPom() {
        // Check pom and read model
        Path pomFile = assertFile(projectDir.resolve("pom.xml"));
        Model model = readPomModel(pomFile.toFile());

        // Flavor
        String parentArtifact = model.getParent().getArtifactId();
        assertThat(parentArtifact, containsString(flavor.toLowerCase()));

        // GroupId
        assertThat(model.getGroupId(), is(groupId));

        // ArtifactId
        assertThat(model.getArtifactId(), is(artifactId));

        // Name
        assertThat(model.getName(), is(projectName));
    }

    private void assertPackageExists() {
        sourceRoot = assertDir(projectDir.resolve("src/main/java"));
        for (String pkg : packageName.split("\\.")) {
            sourceRoot = assertDir(sourceRoot.resolve(pkg));
        }
    }

    private void assertSourceFilesExist() throws IOException {
        long sourceFiles = Files.list(sourceRoot)
                                .filter(file -> file.getFileName().toString().endsWith(".java"))
                                .count();
        assertThat(sourceFiles, is(greaterThan(0L)));
    }

    private void assertProjectConfig() {
        Path dotHelidon = projectDir.resolve(DOT_HELIDON);
        ProjectConfig config = new ProjectConfig(dotHelidon);
        assertThat(config.exists(), is(true));
        if (buildProject) {
            assertThat(config.lastSuccessfulBuildTime(), is(greaterThan(0L)));
        }
    }
}
