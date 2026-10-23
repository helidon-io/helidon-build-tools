/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.cli.impl.ProcessInvocation.MonitorException;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.Maps;
import io.helidon.build.common.SubstitutionVariables;
import io.helidon.build.common.test.utils.WorkDirExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.helidon.build.common.FileUtils.directory;
import static io.helidon.build.common.FileUtils.loadProperties;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.SubstitutionVariables.systemPropertyOrEnvVarSource;
import static io.helidon.build.common.test.utils.FileMatchers.fileExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link InitCommand}.
 */
@ExtendWith(WorkDirExtension.class)
class InitCommandTestIT {

    private final Path workDir;
    private final String url;

    InitCommandTestIT(Path workDir) {
        this.workDir = workDir;
        this.url = url(Map.of(
                "versions.xml", """
                        <data>
                            <archetypes>
                                <version>97.0.0</version>
                                <version>97.0.1</version>
                                <version>98.0.0</version>
                                <version>98.0.1</version>
                                <version>99.0.0</version>
                                <version default="true">99.0.1</version>
                            </archetypes>
                        </data>
                        """,
                "99.0.0/cli-data.zip", cliDataV2("99.0.0"),
                "99.0.1/cli-data.zip", cliDataV2("99.0.1")
        ));
    }

    @Test
    void testProjectOptionAndArgumentMatch() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--project", "project1",
                        "project1")
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("project1");
            assertThat(projectDir, fileExists());
            assertThat(monitor.output(), containsString("Switch directory to " + projectDir + " to use CLI"));

            var projectProps = projectProperties(projectDir);
            assertThat(projectProps, is(Map.of(
                    "archetype", "quickstart",
                    "flavor", "se",
                    "groupId", "com.example",
                    "artifactId", "test-project",
                    "package", "com.example.test",
                    "build", "maven"
            )));
        }
    }

    @Test
    void testProjectOptionAndArgumentMismatch() {
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--project", "project1",
                        "project2")
                .start()) {

            monitor.await();
            fail();
        } catch (MonitorException ex) {
            assertThat(ex.output(), containsString("Different project directories provided"));
            assertThat(ex.output(), containsString("'--project project1'"));
            assertThat(ex.output(), containsString("'project2'"));
        }
    }

    @Test
    void testImplicitProjectCollision() throws IOException {
        Files.createDirectories(workDir.resolve("quickstart-se"));
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url)
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("quickstart-se-2");
            assertThat(projectDir, fileExists());
            assertThat(projectProperties(projectDir), is(defaultProjectProperties()));
        }
    }

    @Test
    void testExplicitProjectCollision() throws IOException {
        var projectDir = Files.createDirectories(workDir.resolve("project1"));
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--project", "project1")
                .start()) {

            monitor.await();
            fail();
        } catch (MonitorException ex) {
            assertThat(ex.output(), containsString("Directory " + projectDir + " already exists"));
        }
    }

    @Test
    void testDefaults() {
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url)
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("quickstart-se");
            assertThat(projectDir, fileExists());

            var projectProps = projectProperties(projectDir);
            assertThat(projectProps, is(defaultProjectProperties()));
        }
    }

    @Test
    void testFlavor() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--flavor", "MP")
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("test-project");
            assertThat(projectDir, fileExists());

            var projectProps = projectProperties(projectDir);
            assertThat(projectProps.get("flavor"), is("mp"));
        }
    }

    @Test
    void testGroupId() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--groupId", "com.acme")
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("test-project");
            assertThat(projectDir, fileExists());

            var projectProps = projectProperties(projectDir);
            assertThat(projectProps.get("groupId"), is("com.acme"));
        }
    }

    @Test
    void testArtifactId() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--artifactId", "acme-project")
                .start()) {

            monitor.await();

            var props = projectProperties(workDir.resolve("acme-project"));
            assertThat(props.get("artifactId"), is("acme-project"));
        }
    }

    @Test
    void testPackage() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--package", "com.acme.project")
                .start()) {

            monitor.await();

            var props = projectProperties(workDir.resolve("test-project"));
            assertThat(props.get("package"), is("com.acme.project"));
        }
    }

    @Test
    void testName() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--name", "acme-project")
                .start()) {

            monitor.await();

            var props = projectProperties(workDir.resolve("acme-project"));
            assertThat(props.get("artifactId"), is("acme-project"));
        }
    }

    @Test
    void testArchetypeBuildAndOutputProperties() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url,
                        "--project", "options",
                        "--archetype", "custom",
                        "--build", "MAVEN",
                        "--output-props-file", "inputs.properties")
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("options");
            var projectProps = projectProperties(projectDir);
            assertThat(projectProps.get("archetype"), is("custom"));
            assertThat(projectProps.get("build"), is("maven"));

            var outputPropsFile = projectDir.resolve("inputs.properties");
            assertThat(outputPropsFile, fileExists());
            var outputProps = Maps.fromProperties(loadProperties(outputPropsFile));
            assertThat(outputProps.get("app-type"), is("custom"));
            assertThat(outputProps.get("build-system"), is("maven"));
        }
    }

    @Test
    void testMissingRequiredOption() {
        var url = url(Map.of(
                "versions.xml", """
                        <data>
                            <archetypes>
                                <version default="true">99.0.1</version>
                            </archetypes>
                        </data>
                        """,
                "99.0.1/cli-data.zip", zip(Map.of(
                        "metadata.properties", """
                                cli.version=0.0.1
                                """,
                        "helidon-99.0.1.jar", Map.of(
                                "main.xml", """
                                        <archetype-script>
                                            <step name="Project">
                                                <inputs>
                                                    <text id="custom"/>
                                                </inputs>
                                            </step>
                                        </archetype-script>
                                        """)))
        ));
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--batch",
                        "--url", url)
                .start()) {

            monitor.await();
            fail();
        } catch (MonitorException ex) {
            assertThat(ex.output(), containsString("Missing required option: -Dcustom=<value>"));
        }
    }

    @Test
    void testInteractive() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init", "--url", url)
                .stdIn("""
                        1
                        2
                        2
                        com.acme
                        acme-project
                        com.acme.project
                        n
                        """)
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("acme-project");
            assertThat(projectDir, fileExists());

            var projectProps = projectProperties(projectDir);
            assertThat(projectProps, is(Map.of(
                    "archetype", "custom",
                    "flavor", "mp",
                    "groupId", "com.acme",
                    "artifactId", "acme-project",
                    "package", "com.acme.project",
                    "build", "maven"
            )));
        }
    }

    @Test
    void testInteractiveFlavorOption() {
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--url", url,
                        "--flavor", "MP")
                .stdIn("""
                        1
                        1
                        com.acme
                        acme-project
                        com.acme.project
                        n
                        """)
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("acme-project");
            assertThat(projectProperties(projectDir), is(Map.of(
                    "archetype", "quickstart",
                    "flavor", "mp",
                    "groupId", "com.acme",
                    "artifactId", "acme-project",
                    "package", "com.acme.project",
                    "build", "maven"
            )));
        }
    }

    @Test
    void testInteractiveV1() {
        var v1Url = url(Map.of(
                "versions.xml", """
                        <data>
                            <archetypes>
                                <version default="true">2.0.0</version>
                            </archetypes>
                        </data>
                        """,
                "2.0.0/cli-data.zip", cliDataV1()
        ));
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init", "--url", v1Url)
                .stdIn("""
                        1
                        1
                        1
                        from-v1
                        n
                        """)
                .start()) {

            monitor.await();

            var projectDir = workDir.resolve("from-v1");
            assertThat(projectDir, fileExists());
            assertThat(projectProperties(projectDir), is(Map.of("engine", "v1")));
        }
    }

    @Test
    void testInteractiveAllHelidonVersions() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--url", url(Map.of(
                                "versions.xml", """
                                        <data>
                                            <archetypes>
                                                <version>97.0.0</version>
                                                <version>97.0.1</version>
                                                <version>98.0.0</version>
                                                <version>98.0.1</version>
                                                <version>99.0.0</version>
                                                <version default="true">99.0.1</version>
                                            </archetypes>
                                        </data>
                                        """,
                                "99.0.0/cli-data.zip", cliDataV2("99.0.0"),
                                "99.0.1/cli-data.zip", zip(Map.of(
                                        "metadata.properties", """
                                                cli.version=0.0.1
                                                """,
                                        "helidon-99.0.1.jar", new byte[0]))
                        )))
                .stdIn("4\n2\n1\n1\n\n\n\nn\n")
                .start()) {

            monitor.await();

            assertThat(monitor.output(), containsString("Helidon versions"));
            assertThat(monitor.output(), containsString("(1) 99.0.1"));
            assertThat(monitor.output(), containsString("(2) 98.0.1"));
            assertThat(monitor.output(), containsString("(3) 97.0.1"));
            assertThat(monitor.output(), containsString("(4) Show all versions"));
            assertThat(monitor.output(), containsString("(2) 99.0.0"));
            assertThat(monitor.output(), containsString("(4) 98.0.0"));
            assertThat(monitor.output(), containsString("(6) 97.0.0"));
            assertThat(monitor.output(), containsString("Enter selection (default: 1):"));

            var projectDir = workDir.resolve("test-project");
            assertThat(projectDir, fileExists());

            var projectProps = projectProperties(projectDir);
            assertThat(projectProps, is(Map.of(
                    "archetype", "quickstart",
                    "flavor", "se",
                    "groupId", "com.example",
                    "artifactId", "test-project",
                    "package", "com.example.test",
                    "build", "maven"
            )));
        }
    }

    @Test
    void testInteractiveLatestHelidonVersions() {
        writeConfig();
        try (var monitor = new ProcessInvocation()
                .cwd(workDir)
                .args("init",
                        "--url", url)
                .stdIn("1\n1\n1\n\n\n\nn\n")
                .start()) {

            monitor.await();

            assertThat(monitor.output(), containsString("Helidon versions"));
            assertThat(monitor.output(), containsString("(1) 99.0.1"));
            assertThat(monitor.output(), containsString("(2) 98.0.1"));
            assertThat(monitor.output(), containsString("(3) 97.0.1"));
            assertThat(monitor.output(), containsString("(4) Show all versions"));
            assertThat(monitor.output(), containsString("Enter selection (default: 1):"));

            var projectDir = workDir.resolve("test-project");
            assertThat(projectDir, fileExists());

            var projectProps = projectProperties(projectDir);
            assertThat(projectProps, is(Map.of(
                    "archetype", "quickstart",
                    "flavor", "se",
                    "groupId", "com.example",
                    "artifactId", "test-project",
                    "package", "com.example.test",
                    "build", "maven"
            )));
        }
    }

    void writeConfig() {
        try {
            Path configFile = workDir.resolve(".helidon/config");
            Files.createDirectories(configFile.getParent());
            FileUtils.writeProperties(Map.of(
                    "default.group.id", "com.example",
                    "default.artifact.id", "test-project",
                    "default.package.name", "com.example.test",
                    "default.project.name", "test"
            ), configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String url(Map<?, ?> entries) {
        return directory(workDir.resolve("cli-data"), entries).toUri().toString();
    }

    static byte[] cliDataV2(String version) {
        return zip(Map.of(
                "metadata.properties", """
                        cli.version=0.0.1
                        """,
                "helidon-" + version + ".jar", Map.of(
                        "main.xml", """
                                <archetype-script>
                                    <step name="Project">
                                        <inputs>
                                            <enum id="app-type" default="quickstart" optional="true">
                                                <option value="quickstart"/>
                                                <option value="custom"/>
                                            </enum>
                                            <enum id="flavor" default="se" optional="true">
                                                <option value="se"/>
                                                <option value="mp"/>
                                            </enum>
                                            <text id="groupId"/>
                                            <text id="artifactId"/>
                                            <text id="package"/>
                                            <enum id="build-system" default="maven" optional="true">
                                                <option value="maven"/>
                                            </enum>
                                        </inputs>
                                    </step>
                                    <output>
                                        <template engine="mustache"
                                                  source="project.properties.hbs"
                                                  target="project.properties">
                                              <model>
                                                  <value key="archetype">${app-type}</value>
                                                  <value key="groupId">${groupId}</value>
                                                  <value key="artifactId">${artifactId}</value>
                                                  <value key="package">${package}</value>
                                                  <value key="flavor">${flavor}</value>
                                                  <value key="build">${build-system}</value>
                                              </model>
                                        </template>
                                    </output>
                                </archetype-script>
                                """,
                        "project.properties.hbs", """
                                archetype={{archetype}}
                                flavor={{flavor}}
                                groupId={{groupId}}
                                artifactId={{artifactId}}
                                package={{package}}
                                build={{build}}
                                """)));
    }

    static byte[] cliDataV1() {
        return zip(Map.of(
                "metadata.properties", """
                        cli.version=0.0.1
                        """,
                "archetype-catalog.xml", """
                        <archetype-catalog modelVersion="1.0" name="test" groupId="com.example" version="2.0.0">
                            <archetype artifactId="minimal-se"
                                       name="quickstart"
                                       title="Minimal SE"
                                       summary="Minimal SE project"
                                       tags="se" />
                        </archetype-catalog>
                        """,
                "minimal-se-2.0.0.jar", Map.of(
                        "META-INF/helidon-archetype.xml", """
                                <archetype-descriptor modelVersion="1.0" name="minimal">
                                    <properties>
                                        <property id="name"/>
                                    </properties>
                                    <template-sets>
                                        <template-set>
                                            <directory>.</directory>
                                            <includes>
                                                <include>project.properties</include>
                                            </includes>
                                        </template-set>
                                    </template-sets>
                                    <input-flow>
                                        <input id="name" property="name" text="Enter a project name"/>
                                    </input-flow>
                                </archetype-descriptor>
                                """,
                        "META-INF/helidon-archetype-resources.txt", """
                                project.properties
                                """,
                        "project.properties", """
                                engine=v1
                                """)));
    }

    static Map<String, String> defaultProjectProperties() {
        var substitutions = SubstitutionVariables.of(key -> switch (key.toLowerCase()) {
            case "init_archetype" -> "quickstart";
            case "init_flavor" -> "se";
            case "init_build" -> "maven";
            default -> null;
        }, systemPropertyOrEnvVarSource());
        return Map.of(
                "archetype", "quickstart",
                "flavor", "se",
                "groupId", substitutions.resolve("me.${user.name}-helidon"),
                "artifactId", substitutions.resolve("${init_archetype}-${init_flavor}"),
                "package", substitutions.resolve("me.${user.name}.${init_flavor}.${init_archetype}"),
                "build", substitutions.resolve("${init_build}"));
    }

    static Map<String, String> projectProperties(Path projectDir) {
        return Maps.fromProperties(loadProperties(projectDir.resolve("project.properties")));
    }
}
