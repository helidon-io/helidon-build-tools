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

import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_APPTYPE;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_ARTIFACT_ID;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_FLAVOR;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_GROUP_ID;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_NAME;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_PACKAGE;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.helidon.build.cli.impl.PomReader.readPomModel;

/**
 * Class InitTestHelper.
 */
class InitTestHelper {

    private static final String HELIDON_VERSION_TEST = "2.0.0-SNAPSHOT";

    private String flavor = DEFAULT_FLAVOR;
    private String apptype = DEFAULT_APPTYPE;
    private String groupId = DEFAULT_GROUP_ID;
    private String artifactId = DEFAULT_ARTIFACT_ID;
    private String packageName = DEFAULT_PACKAGE;
    private String name = DEFAULT_NAME;

    private Path targetDir = TestFiles.targetDir();

    InitTestHelper() {
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
        this.name = name;
    }

    public void execute() throws Exception {
        try {
            generate();
            build();
        } finally {
            clean();
        }
    }

    private void generate() throws Exception {
        List<String> args = new ArrayList<>();
        args.add("init");
        args.add("--batch");
        args.add("--version");
        args.add(HELIDON_VERSION_TEST);
        args.add("--project");
        args.add(targetDir.toString());
        if (!flavor.equals(DEFAULT_FLAVOR)) {
            args.add("--flavor");
            args.add(flavor);
        }
        if (!apptype.equals(DEFAULT_APPTYPE)) {
            args.add("--apptype");
            args.add(apptype);
        }
        if (!groupId.equals(DEFAULT_GROUP_ID)) {
            args.add("--groupId");
            args.add(groupId);
        }
        if (!artifactId.equals(DEFAULT_ARTIFACT_ID)) {
            args.add("--artifactId");
            args.add(artifactId);
        }
        if (!packageName.equals(DEFAULT_PACKAGE)) {
            args.add("--package");
            args.add(packageName);
        }
        if (!name.equals(DEFAULT_NAME)) {
            args.add("--name");
            args.add(name);
        }
        String[] argsArray = args.toArray(new String[]{});
        System.out.print("Executing with args ");
        args.forEach(a -> System.out.print(a + " "));
        System.out.println();

        // Execute and verify process exit code
        TestUtils.ExecResult res = TestUtils.exec(argsArray);
        assertThat(res.code, is(equalTo(0)));
        verify();
    }

    private void verify() {
        // Project directory
        Path projectDir = targetDir.resolve(Path.of(name));
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
        Path packageDir = projectDir.resolve("src/main/java")
                .resolve(packageName.replace('.', '/'));
        assertTrue(Files.exists(packageDir));

        // Name
        assertThat(model.getName(), is(name));
    }

    private void build() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(name));
        TestUtils.ExecResult res = exec("build", "--project ", projectDir.toString());
        System.out.println(res.output);
        assertThat(res.code, is(equalTo(0)));
        assertTrue(Files.exists(projectDir.resolve("target/" + artifactId + ".jar")));
    }

    private void clean() {
        Path projectDir = targetDir.resolve(Path.of(name));
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
