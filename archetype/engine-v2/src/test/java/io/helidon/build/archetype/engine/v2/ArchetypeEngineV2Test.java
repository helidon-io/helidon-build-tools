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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.prompter.DefaultPrompterImpl;
import io.helidon.build.common.Strings;
import io.helidon.build.common.test.utils.TestFiles;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ArchetypeEngineV2Test extends ArchetypeBaseTest {

    @Test
    void generateSkipOptional() throws IOException {
        File targetDir = new File(new File("").getAbsolutePath(), "target");
        File outputDir = new File(targetDir, "test-project");
        Path outputDirPath = outputDir.toPath();
        if (Files.exists(outputDirPath)) {
            Files.walk(outputDirPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        assertThat(Files.exists(outputDirPath), is(false));

        Map<String, String> initContextValues = new HashMap<>();
        initContextValues.put("flavor", "se");
        initContextValues.put("base", "bare");
        initContextValues.put("build-system", "maven");
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(getArchetype(
                Paths.get("src/main/resources/archetype").toFile()),
                "flavor.xml",
                new DefaultPrompterImpl(true),
                initContextValues,
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
        assertThat(helidonFile, containsString("project.directory="+outputDirPath.toString()));
    }

    private static String readFile(Path file) throws IOException {
        return Strings.normalizeNewLines(Files.readString(file));
    }
}
