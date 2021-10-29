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

    @Test
    void generateSkipOptional() throws IOException {
        File targetDir = new File(new File("").getAbsolutePath(), "target");
        File outputDir = new File(targetDir, "test-project");
        Path outputDirPath = outputDir.toPath();
        FileUtils.deleteDirectory(outputDirPath);
        assertThat(Files.exists(outputDirPath), is(false));

        Map<String, String> presets = new HashMap<>();
        presets.put("flavor", "se");
        presets.put("base", "bare");
        presets.put("build-system", "maven");
        Map<String, String> defaults = Map.of("project.name", "test-project");
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(getArchetype(
                Paths.get("src/main/resources/archetype").toFile()),
                                                                    "flavor.xml",
                                                                    new DefaultPrompterImpl(true),
                                                                    presets,
                                                                    defaults,
                                                                    true,
                                                                    List.of());

        archetypeEngineV2.generate(outputDir);
        assertThat(Files.exists(outputDirPath), is(true));

        assertThat(Files.walk(outputDirPath)
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

        String mainTest = readFile(outputDirPath.resolve("src/test/java/io/helidon/examples/bare/se/MainTest.java"));
        assertThat(mainTest, containsString("package io.helidon.examples.bare.se;"));
        assertThat(mainTest, containsString("public class MainTest {"));

        String loggingProperties = readFile(outputDirPath.resolve("src/main/resources/logging.properties"));
        assertThat(loggingProperties, containsString("handlers=io.helidon.common.HelidonConsoleHandler"));

        String applicationYaml = readFile(outputDirPath.resolve("src/main/resources/application.yaml"));
        assertThat(applicationYaml, containsString("greeting: \"Hello\""));

        String packageInfo = readFile(outputDirPath.resolve("src/main/java/io/helidon/examples/bare/se/package-info.java"));
        assertThat(packageInfo, containsString("package io.helidon.examples.bare.se;"));

        String mainClass = readFile(outputDirPath.resolve("src/main/java/io/helidon/examples/bare/se/Main.java"));
        assertThat(mainClass, containsString("package io.helidon.examples.bare.se;"));

        String greetService = readFile(outputDirPath.resolve("src/main/java/io/helidon/examples/bare/se/GreetService.java"));
        assertThat(greetService, containsString("package io.helidon.examples.bare.se;"));

        String pom = readFile(outputDirPath.resolve("pom.xml"));
        assertThat(pom, containsString("<groupId>io.helidon.applications</groupId>"));
        assertThat(pom, containsString("<artifactId>helidon-bare-se</artifactId>"));
        assertThat(pom, containsString("<mainClass>io.helidon.examples.bare.se.Main</mainClass>"));
        assertThat(pom, containsString("<groupId>org.junit.jupiter</groupId>"));
        assertThat(pom, containsString("<scope>test</scope>"));
        assertThat(pom, containsString("<artifactId>maven-dependency-plugin</artifactId>"));
        assertThat(pom, containsString("<id>copy-libs</id>"));

        String readme = readFile(outputDirPath.resolve("README.md"));
        assertThat(readme, containsString("Helidon SE Bare"));
        assertThat(readme, containsString("java -jar target/helidon-bare-se.jar"));
        assertThat(readme, containsString("## Exercise the application"));

        String helidonFile = readFile(outputDirPath.resolve(".helidon"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMM dd");
        ZonedDateTime now = ZonedDateTime.now();
        assertThat(helidonFile, containsString(dtf.format(now)));
        assertThat(helidonFile, containsString("project.directory="+outputDirPath));
    }

    @Test
    void generateWithIncludeCycleFails() throws IOException {
        File outputDir = Files.createTempDirectory("include-cycle").toFile();

        Map<String, String> presets = new HashMap<>();
        presets.put("flavor", "se");
        presets.put("base", "bare");
        presets.put("build-system", "maven");
        Archetype archetype = getArchetype("include-cycle");
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(archetype,
                                                                    "flavor.xml",
                                                                    new DefaultPrompterImpl(true),
                                                                    presets,
                                                                    Map.of(),
                                                                    true, List.of());
        try {
            archetypeEngineV2.generate(outputDir);
            fail("should have failed");
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            assertThat("Got " + msg, msg.contains(fixPaths("Include cycle: 'se/se.xml'")), is(true));
            assertThat("Got " + msg, msg.contains("included via <source> in 'flavor.xml'"), is(true));
            assertThat("Got " + msg, msg.contains(fixPaths("and again via <exec> in 'se/bare/bare-se.xml'")), is(true));
        }
    }

    @Test
    void generateWithDuplicateIncludeFails() throws IOException {
        File outputDir = Files.createTempDirectory("duplicate-include").toFile();

        Map<String, String> presets = new HashMap<>();
        presets.put("flavor", "se");
        presets.put("base", "bare");
        presets.put("build-system", "maven");
        Archetype archetype = getArchetype("duplicate-include");
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(archetype,
                                                                    "flavor.xml",
                                                                    new DefaultPrompterImpl(true),
                                                                    presets,
                                                                    Map.of(),
                                                                    true, List.of());
        try {
            archetypeEngineV2.generate(outputDir);
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
