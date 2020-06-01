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

package io.helidon.build.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.test.TestFiles;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.helidon.build.test.HelidonTestVersions.currentHelidonReleaseVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link ApplicationGenerator}.
 */
class ApplicationGeneratorTest {
    private static final Path TARGET_DIR = TestFiles.targetDir(ApplicationGeneratorTest.class);
    private Path generated;

    @AfterEach
    void cleanup() {
        if (generated != null) {
            try {
                FileUtils.deleteDirectory(generated);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Test
    void testSeGenerationNonExistentVersion() {
        try {
            testGeneration(HelidonVariant.SE, "0.0.0");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("desired archetype does not exist"));
        }
    }

    @Test
    void testSeGeneration() {
        testGeneration(HelidonVariant.SE, currentHelidonReleaseVersion());
    }

    @Test
    void testMpGeneration() {
        testGeneration(HelidonVariant.MP, currentHelidonReleaseVersion());
    }

    private void testGeneration(HelidonVariant variant, String version) {
        generated = ApplicationGenerator.generator()
                                        .parentDirectory(TARGET_DIR)
                                        .helidonVariant(variant)
                                        .helidonVersion(version)
                                        .generate();

        assertThat(generated, is(not(nullValue())));
        assertThat(Files.isDirectory(generated), is(true));
        assertThat(Files.exists(generated.resolve("pom.xml")), is(true));
        assertThat(Files.exists(generated.resolve("src/main/java")), is(true));
        assertThat(Files.exists(generated.resolve("src/main/resources")), is(true));
        assertThat(FileUtils.listFiles(generated, name -> name.endsWith(".java"), 64), is(not(empty())));
    }
}
