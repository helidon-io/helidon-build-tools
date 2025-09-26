/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype.postgenerate;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.common.VirtualFileSystem;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.Strings.normalizeNewLines;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static java.nio.file.Files.readString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link ReflectedEngine}.
 */
class ReflectedEngineTest {

    @Test
    void testGenerate() throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path projectDir = generate(targetDir.resolve("test-classes/simple"), targetDir.resolve("reflected-engine-ut"));
        assertThat(Files.exists(projectDir.resolve("color.txt")), is(true));
        assertThat(normalizeNewLines(readString(projectDir.resolve("color.txt"))), is("red\n"));
    }

    static Path generate(Path sourceDir, Path outputDir) throws IOException {
        try (FileSystem fs = VirtualFileSystem.create(sourceDir)) {
            Path root = fs.getRootDirectories().iterator().next();
            Map<String, String> externalValues = Map.of("color", "red", "artifactId", "testGenerate");
            ReflectedEngine engine = new ReflectedEngine(
                    ReflectedEngineTest.class.getClassLoader(), root, false,
                    externalValues, Map.of(), () -> unique(outputDir, "testGenerate"));

            return engine.generate();
        }
    }
}
