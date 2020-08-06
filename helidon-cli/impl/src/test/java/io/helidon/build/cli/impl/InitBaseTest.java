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
class InitBaseTest extends MetadataCommandTest {

    private final Path targetDir = TestFiles.targetDir();
    private Path projectDir;
    private String defaultProjectName;
    private String defaultGroupId;
    private String defaultArtifactId;
    private String defaultPackageName;
    private Path sourceRoot;

    private File input;
    private String flavor;
    private String archetypeName;
    private String groupId;
    private String artifactId;
    private String packageName;
    private String projectName;
    private boolean build;

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

    protected void build(boolean build) {
        this.build = build;
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

    protected void initDefaults() {
        SubstitutionVariables substitutions = SubstitutionVariables.of(systemPropertyOrEnvVarSource(), key -> {
            switch (key.toLowerCase()) {
                case "init_flavor":
                    return flavor.toLowerCase();
                case "init_archetype":
                    return archetypeName;
                default:
                    return null;
            }
        });
        defaultProjectName = userConfig().defaultProjectName(substitutions);
        defaultGroupId = userConfig().defaultGroupId(substitutions);
        defaultArtifactId = userConfig().defaultArtifactId(substitutions);
        defaultPackageName = userConfig().defaultPackageName(substitutions);
        projectDir = targetDir.resolve(projectName == null ? defaultProjectName : projectName);
    }

    protected Path projectDir() {
        return projectDir;
    }

    protected String defaultProjectName() {
        return defaultProjectName;
    }

    protected String defaultGroupId() {
        return defaultGroupId;
    }

    protected String defaultArtifactId() {
        return defaultArtifactId;
    }

    protected String defaultPackageName() {
        return defaultPackageName;
    }

    protected void generate() throws Exception {
        initDefaults();
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
        if (build) {
            exec("build", "--project ", projectDir.toString());
            assertJarExists();
            assertProjectConfig();
        }
    }

    protected void assertJarExists() {
        assertFile(projectDir.resolve("target").resolve((artifactId == null ? defaultArtifactId : artifactId) + ".jar"));
// TODO assertFile(projectDir.resolve("target").resolve((projectName == null ? defaultProjectName : projectName) + ".jar"));
// if user specifies --name and NOT --artifactid, use name as artifactid?
// if yes, them we must do the reverse as well/
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
        assertThat(model.getGroupId(), is(groupId == null ? defaultGroupId : groupId));

        // ArtifactId
        assertThat(model.getArtifactId(), is(artifactId == null ? defaultArtifactId : artifactId));

        // Name
        assertThat(model.getName(), is(projectName == null ? defaultProjectName : projectName));
    }

    private void assertPackageExists() {
        sourceRoot = assertDir(projectDir.resolve("src/main/java"));
        String pkgName = packageName == null ? defaultPackageName : packageName;
        for (String pkg : pkgName.split("\\.")) {
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
        if (build) {
            assertThat(config.lastSuccessfulBuildTime(), is(greaterThan(0L)));
        }
    }
}
