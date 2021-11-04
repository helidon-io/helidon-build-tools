/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.prompter.DefaultPrompterImpl;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.Strings;
import io.helidon.build.common.test.utils.TestFiles;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ArchetypeEngineV2Test extends ArchetypeBaseTest {

    static Path projectNameToDirectory(String projectName) {
        File targetDir = new File(new File("").getAbsolutePath(), "target");
        File projectDir = new File(targetDir, projectName);
        return projectDir.toPath();
    }

    static File archetypeDir() {
        // NOTE: this is not right or IDE friendly as it assumes that the working directory is set to the project directory
        //       and that we resolve into the sources rather than target, e.g. this resolves to
        //       ${HOME}/dev/helidon-build-tools/archetype/engine-v2/src/main/resources/archetype
        return Paths.get("src/main/resources/archetype").toFile();
    }

    Archetype getArchetype() {
        return getArchetype(archetypeDir());
    }

    @Test
    void generateSkipOptional() throws IOException {
        String projectName = "skip-optional";
        Path outputDirPath = projectNameToDirectory(projectName);
        FileUtils.deleteDirectory(outputDirPath);
        assertThat(Files.exists(outputDirPath), is(false));

        Map<String, String> params = Map.of("flavor", "se",
                                             "base", "bare",
                                             "build-system", "maven",
                                             "project.name", projectName);
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(getArchetype(),
                                                                    "flavor.xml",
                                                                    new DefaultPrompterImpl(true),
                                                                    params,
                                                                    Map.of(),
                                                                    true,
                                                                    true,
                                                                    List.of());

        Path outputDir = archetypeEngineV2.generate(ArchetypeEngineV2Test::projectNameToDirectory);
        assertThat(outputDir.getFileName().toString(), is(projectName));
        assertThat(outputDir, is(outputDirPath));
        assertThat(Files.exists(outputDir), is(true));

        assertThat(Files.walk(outputDir)
                        .filter(p -> !Files.isDirectory(p))
                        .map((p) -> TestFiles.pathOf(outputDirPath.relativize(p)))
                        .sorted()
                        .collect(Collectors.toList()),
                is(List.of(
                        ".helidon",
                        "README.md",
                        "pom.xml",
                        "src/main/java/io/helidon/examples/bare/se/GreetService.java",
                        "src/main/java/io/helidon/examples/bare/se/Main.java",
                        "src/main/java/io/helidon/examples/bare/se/package-info.java",
                        "src/main/resources/META-INF/native-image/reflect-config.json",
                        "src/main/resources/application.yaml",
                        "src/main/resources/logging.properties",
                        "src/test/java/io/helidon/examples/bare/se/MainTest.java"
                )));

        String mainTest = readFile(outputDir.resolve("src/test/java/io/helidon/examples/bare/se/MainTest.java"));
        assertThat(mainTest, containsString("package io.helidon.examples.bare.se;"));
        assertThat(mainTest, containsString("public class MainTest {"));

        String loggingProperties = readFile(outputDir.resolve("src/main/resources/logging.properties"));
        assertThat(loggingProperties, containsString("handlers=io.helidon.common.HelidonConsoleHandler"));

        String applicationYaml = readFile(outputDir.resolve("src/main/resources/application.yaml"));
        assertThat(applicationYaml, containsString("greeting: \"Hello\""));

        String packageInfo = readFile(outputDir.resolve("src/main/java/io/helidon/examples/bare/se/package-info.java"));
        assertThat(packageInfo, containsString("package io.helidon.examples.bare.se;"));

        String mainClass = readFile(outputDir.resolve("src/main/java/io/helidon/examples/bare/se/Main.java"));
        assertThat(mainClass, containsString("package io.helidon.examples.bare.se;"));

        String greetService = readFile(outputDir.resolve("src/main/java/io/helidon/examples/bare/se/GreetService.java"));
        assertThat(greetService, containsString("package io.helidon.examples.bare.se;"));

        String pom = readFile(outputDir.resolve("pom.xml"));
        assertThat(pom, containsString("<groupId>io.helidon.applications</groupId>"));
        assertThat(pom, containsString("<artifactId>helidon-bare-se</artifactId>"));
        assertThat(pom, containsString("<mainClass>io.helidon.examples.bare.se.Main</mainClass>"));
        assertThat(pom, containsString("<groupId>org.junit.jupiter</groupId>"));
        assertThat(pom, containsString("<scope>test</scope>"));
        assertThat(pom, containsString("<artifactId>maven-dependency-plugin</artifactId>"));
        assertThat(pom, containsString("<id>copy-libs</id>"));

        String readme = readFile(outputDir.resolve("README.md"));
        assertThat(readme, containsString("Helidon SE Bare"));
        assertThat(readme, containsString("java -jar target/helidon-bare-se.jar"));
        assertThat(readme, containsString("## Exercise the application"));

        String helidonFile = readFile(outputDir.resolve(".helidon"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMM dd");
        ZonedDateTime now = ZonedDateTime.now();
        assertThat(helidonFile, containsString(dtf.format(now)));
        assertThat(helidonFile, containsString("project.directory=" + outputDir));
        assertThat(helidonFile, containsString(projectName));
    }

    @Test
    void generateWithFailOnUnresolvedInputFails() {
        Map<String, String> params = Map.of("flavor", "se",
                                            "build-system", "maven");

        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(getArchetype(),
                                                                    "flavor.xml",
                                                                    new DefaultPrompterImpl(true),
                                                                    params,
                                                                    Map.of(),
                                                                    true,
                                                                    true,
                                                                    List.of());
        try {
            archetypeEngineV2.generate(ArchetypeEngineV2Test::projectNameToDirectory);
            fail("should have failed");
        } catch (UnresolvedInputException e) {
            assertThat(e.inputPath(), is("base"));
        }
    }

    @Test
    void generateWithIncludeCycleFails() {
        Map<String, String> params = new HashMap<>();
        params.put("flavor", "se");
        params.put("base", "bare");
        params.put("build-system", "maven");
        Archetype archetype = getArchetype("include-cycle");
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(archetype,
                                                                    "flavor.xml",
                                                                    new DefaultPrompterImpl(true),
                                                                    params,
                                                                    Map.of(),
                                                                    true,
                                                                    false,
                                                                    List.of());
        try {
            archetypeEngineV2.generate(ArchetypeEngineV2Test::projectNameToDirectory);
            fail("should have failed");
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            assertThat("Got " + msg, msg.contains(fixPaths("Include cycle: 'se/se.xml'")), is(true));
            assertThat("Got " + msg, msg.contains("included via <source> in 'flavor.xml'"), is(true));
            assertThat("Got " + msg, msg.contains(fixPaths("and again via <exec> in 'se/bare/bare-se.xml'")), is(true));
        }
    }

    @Test
    void generateWithDuplicateIncludeFails() {
        Map<String, String> params = new HashMap<>();
        params.put("flavor", "se");
        params.put("base", "bare");
        params.put("build-system", "maven");
        Archetype archetype = getArchetype("duplicate-include");
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(archetype,
                                                                    "flavor.xml",
                                                                    new DefaultPrompterImpl(true),
                                                                    params,
                                                                    Map.of(),
                                                                    true,
                                                                    false, List.of());
        try {
            archetypeEngineV2.generate(ArchetypeEngineV2Test::projectNameToDirectory);
            fail("should have failed");
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            assertThat("Got " + msg, msg.contains(fixPaths("Duplicate include: 'common/java-files.xml'")), is(true));
            assertThat("Got " + msg, msg.contains(fixPaths("included via <source> in 'se/bare/bare-se.xml'")), is(true));
            assertThat("Got " + msg, msg.contains(fixPaths("and again via <source> in 'common/common.xml'")), is(true));
        }
    }

    private static String fixPaths(String msg) {
        return msg.replace("/", File.separator);
    }

    private static String readFile(Path file) throws IOException {
        return Strings.normalizeNewLines(Files.readString(file));
    }
}
