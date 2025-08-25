/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.cli.impl.ProcessInvocation.Monitor;
import io.helidon.build.cli.impl.ProcessInvocation.MonitorException;
import io.helidon.build.common.Strings;
import io.helidon.build.common.maven.MavenModel;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.common.FileUtils.fileExt;
import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.FileUtils.urlOf;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests {@link InitCommand}.
 */
class InitCommandTest extends MetadataAccess {

    static final Path TARGET_DIR = targetDir(InitCommandTest.class);
    static final Path CWD = TARGET_DIR.resolve("init-ut");

    @Test
    void testProjectOptionAndArgumentMatch() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "project-option-and-argument-match"))
                .args("init",
                        "--batch", "--reset",
                        "--url", metadataUrl(),
                        "--project", "project1",
                        "project1")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("project1");
            assertThat(monitor.output(), containsString("Switch directory to " + projectDir + " to use CLI"));
        }
    }

    @Test
    void testProjectOptionAndArgumentMismatch() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "project-option-and-argument-mismatch"))
                .args("init",
                        "--batch", "--reset",
                        "--url", metadataUrl(),
                        "--project", "project1",
                        "project2")
                .start()) {

            monitor.await();
            throw new AssertionError("Expected exception");
        } catch (MonitorException ex) {
            assertThat(ex.output(), containsString("Different project directories provided"));
            assertThat(ex.output(), containsString("'--project project1'"));
            assertThat(ex.output(), containsString("'project2'"));
        }
    }

    @Test
    void testDefaults() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "defaults"))
                .args("init",
                        "--batch",
                        "--reset", "--url", metadataUrl(),
                        "--project", "defaults")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("defaults");
            validateProject(projectDir);
        }
    }

    @Test
    void testFlavor() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "flavor"))
                .args("init",
                        "--batch",
                        "--reset",
                        "--url", metadataUrl(),
                        "--flavor", "MP")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("quickstart-mp");
            validateProject(projectDir);
        }
    }

    @Test
    void testGroupId() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "groupId"))
                .args("init",
                        "--batch",
                        "--reset", "--url", metadataUrl(),
                        "--groupId", "com.acme")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("quickstart-se");
            validateProject(projectDir);

            Path pomFile = projectDir.resolve("pom.xml");
            assertThat(Files.exists(pomFile), is(true));
            assertThat(Files.isRegularFile(pomFile), is(true));

            MavenModel mavenModel = MavenModel.read(pomFile);
            assertThat(mavenModel.groupId(), is("com.acme"));
        }
    }

    @Test
    void testArtifactId() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "artifactId"))
                .args("init",
                        "--batch",
                        "--reset",
                        "--url", metadataUrl(),
                        "--artifactId", "acme-project")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("acme-project");
            validateProject(projectDir);

            Path pomFile = projectDir.resolve("pom.xml");
            assertThat(Files.exists(pomFile), is(true));
            assertThat(Files.isRegularFile(pomFile), is(true));

            MavenModel mavenModel = MavenModel.read(pomFile);
            assertThat(mavenModel.artifactId(), is("acme-project"));
        }
    }

    @Test
    void testPackage() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "package"))
                .args("init",
                        "--batch",
                        "--reset", "--url", metadataUrl(),
                        "--package", "com.acme")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("quickstart-se");
            validateProject(projectDir);

            Path sourceRoot = projectDir.resolve("src/main/java");
            List<String> javaPackages = javaPackages(sourceRoot);
            assertThat(javaPackages, is(List.of("com.acme")));
        }
    }

    @Test
    void testName() {
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "name"))
                .args("init",
                        "--batch",
                        "--reset",
                        "--url", metadataUrl(),
                        "--name", "acme-project")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("acme-project");
            validateProject(projectDir);
        }
    }

    @Test
    void testInteractiveSe() {
        Path stdIn = testResourcePath(InitCommandTest.class, "init/input.txt");
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "interactive-se"))
                .stdIn(stdIn)
                .args("init",
                        "--reset",
                        "--url", metadataUrl())
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("quickstart-se");
            validateProject(projectDir);
        }
    }

    @Test
    void testInteractiveMp() {
        Path stdIn = testResourcePath(InitCommandTest.class, "init/input.txt");
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "interactive-mp"))
                .stdIn(stdIn)
                .args("init",
                        "--plain",
                        "--reset",
                        "--url", metadataUrl(),
                        "--flavor", "MP")
                .start()) {

            monitor.await();

            Path projectDir = monitor.cwd().resolve("quickstart-mp");
            validateProject(projectDir);
        }
    }

    @Test
    void testInteractiveAllHelidonVersions() {
        String helidonVersion = System.getProperty(HELIDON_VERSION_PROPERTY);
        System.clearProperty(HELIDON_VERSION_PROPERTY);

        Path stdIn = testResourcePath(InitCommandTest.class, "init/input-full-version-list.txt");
        URL cliDataUrl = urlOf(testResourcePath(getClass(), "versions").resolve("cli-data"));
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "interactive-all-versions"))
                .stdIn(stdIn)
                .args("init",
                        "--plain",
                        "--reset",
                        "--url", cliDataUrl.toString())
                .start()) {

            monitor.await();

            assertThat(monitor.output(), containsString("Helidon versions"));
            assertThat(monitor.output(), containsString("(28) 2.0.0"));
            assertThat(monitor.output(), containsString("(29) 4.0.0-SNAPSHOT"));
            assertThat(monitor.output(), containsString("Enter selection (default: 29):"));

            Path projectDir = monitor.cwd().resolve("quickstart-se");
            validateProject(projectDir);
        } finally {
            if (helidonVersion != null) {
                System.setProperty(HELIDON_VERSION_PROPERTY, helidonVersion);
            }
        }
    }

    @Test
    void testInteractiveLatestHelidonVersions() {
        String helidonVersion = System.getProperty(HELIDON_VERSION_PROPERTY);
        System.clearProperty(HELIDON_VERSION_PROPERTY);

        Path stdIn = testResourcePath(InitCommandTest.class, "init/input-latest-version-list.txt");
        URL cliDataUrl = urlOf(testResourcePath(getClass(), "versions").resolve("cli-data"));
        try (Monitor monitor = new ProcessInvocation()
                .cwd(unique(CWD, "interactive-latest-versions"))
                .stdIn(stdIn)
                .args("init",
                        "--plain",
                        "--reset",
                        "--url", cliDataUrl.toString())
                .start()) {

            monitor.await();

            assertThat(monitor.output(), containsString("Helidon versions"));
            assertThat(monitor.output(), containsString("(1) 3.1.2"));
            assertThat(monitor.output(), containsString("(2) 2.6.0"));
            assertThat(monitor.output(), containsString("(3) 4.0.0-SNAPSHOT"));
            assertThat(monitor.output(), containsString("(4) Show all versions"));
            assertThat(monitor.output(), containsString("Enter selection (default: 3):"));

            Path projectDir = monitor.cwd().resolve("quickstart-se");
            validateProject(projectDir);
        } finally {
            if (helidonVersion != null) {
                System.setProperty(HELIDON_VERSION_PROPERTY, helidonVersion);
            }
        }
    }

    static void validateProject(Path projectDir) {
        assertThat(Files.exists(projectDir), is(true));
        assertThat(Files.isDirectory(projectDir), is(true));

        Path pomFile = projectDir.resolve("pom.xml");
        assertThat(Files.exists(pomFile), is(true));
        assertThat(Files.isRegularFile(pomFile), is(true));

        Path sourceRoot = projectDir.resolve("src/main/java");
        assertThat(Files.exists(sourceRoot), is(true));
        assertThat(Files.isDirectory(sourceRoot), is(true));

        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            long count = stream.count();
            assertThat(count, greaterThan(0L));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static List<String> javaPackages(Path sourceRoot) {
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> fileExt(p).equals("java"))
                    .filter(p -> !fileName(p).equals("module-info.java"))
                    .map(p -> Strings.normalizePath(sourceRoot.relativize(p).getParent().toString()))
                    .map(s -> s.replace("/", "."))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
