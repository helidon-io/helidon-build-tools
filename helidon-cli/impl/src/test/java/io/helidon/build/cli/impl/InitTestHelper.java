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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.test.TestFiles;

import org.apache.maven.model.Model;

import static io.helidon.build.cli.impl.InitCommand.DEFAULT_ARCHETYPE_NAME;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_FLAVOR;
import static io.helidon.build.cli.impl.TestUtils.assertPackageExist;
import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static io.helidon.build.util.PomUtils.readPomModel;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class InitTestHelper.
 */
class InitTestHelper {

    private final String metadataUrl;
    private final Path targetDir;
    private String flavor = DEFAULT_FLAVOR;
    private String apptype = DEFAULT_ARCHETYPE_NAME;
    private String groupId;
    private String artifactId;
    private String packageName;
    private String projectName;
    private String projectDir;


    InitTestHelper(String metadataUrl) {
        this.metadataUrl = metadataUrl;
        this.targetDir =  TestFiles.targetDir();
    }

    private void init() {

    }

    public void flavor(String flavor) {
        this.flavor = flavor.toUpperCase();
    }

    public void apptype(String apptype) {
        this.apptype = apptype;
    }

    public void groupId(String groupId) {
        this.groupId = groupId;
    }

    public void artifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void packageName(String packageName) {
        this.packageName = packageName;
    }

    public void name(String name) {
        this.projectName = name;
    }

    public void execute() throws Exception {
        try {
            init();
            generate();
            build();
        } finally {
            clean();
        }
    }

    private void generate() throws Exception {
        List<String> args = new ArrayList<>();
        args.add("init");
        args.add("--url");
        args.add(metadataUrl);
        args.add("--batch");
        args.add("--version");
        args.add(helidonTestVersion());
        args.add("--project");
        args.add(targetDir.toString());
        if (!flavor.equals(DEFAULT_FLAVOR)) {
            args.add("--flavor");
            args.add(flavor);
        }
        if (!apptype.equals(DEFAULT_ARCHETYPE_NAME)) {
            args.add("--apptype");
            args.add(apptype);
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
        TestUtils.exec(argsArray);
        verify();
    }

    private void verify() {
        // Project directory
        Path projectDir = targetDir.resolve(Path.of(projectName));
        assertTrue(Files.exists(projectDir));

        // Check pom and read model
        Path pomFile = projectDir.resolve("pom.xml");
        assertTrue(pomFile.toFile().exists());
        Model model = readPomModel(pomFile.toFile());

        // Flavor
        String parentArtifact = model.getParent().getArtifactId();
        assertThat(parentArtifact, containsString(flavor.toLowerCase()));

        // GroupId
        assertThat(model.getGroupId(), is(groupId));

        // ArtifactId
        assertThat(model.getArtifactId(), is(artifactId));

        // Package
        assertPackageExist(projectDir, packageName);

        // Name
        assertThat(model.getName(), is(projectName));
    }

    private void build() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(projectName));
        exec("build", "--project ", projectDir.toString());
        assertTrue(Files.exists(projectDir.resolve("target/" + artifactId + ".jar")));
    }

    private void clean() {
        Path projectDir = targetDir.resolve(Path.of(projectName));
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
