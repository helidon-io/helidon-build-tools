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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.test.utils.TestFiles;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.engine;
import static io.helidon.build.archetype.engine.v2.TestHelper.uniqueDir;
import static io.helidon.build.common.test.utils.TestFiles.pathOf;
import static java.nio.file.Files.isDirectory;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Tests {@link ArchetypeEngineV2}.
 */
class ArchetypeEngineV2Test {

    @Test
    void e2e() throws IOException {
        Map<String, String> externalValues = Map.of(
                "flavor", "se",
                "flavor.base", "bare");
        Path directory = TestFiles.targetDir(ArchetypeEngineV2Test.class).resolve("e2e");

        ArchetypeEngineV2 engine = engine("e2e");
        Path outputDir = engine.generate(new BatchInputResolver(), externalValues, Map.of(), n -> uniqueDir(directory, n));
        assertThat(Files.exists(outputDir), is(true));

        List<String> files = Files.walk(outputDir)
                                  .filter(p -> !isDirectory(p))
                                  .map((p) -> pathOf(outputDir.relativize(p)))
                                  .sorted()
                                  .collect(toList());
        assertThat(files, contains(
                ".dockerignore",
                "README.md",
                "pom.xml",
                "src/main/java/com/example/myproject/Main.java",
                "src/main/java/com/example/myproject/package-info.java",
                "src/main/resources/application.yaml"
        ));
    }
}
