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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.eclipse.aether.version.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.build.util.Maven.LATEST_RELEASE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link QuickstartGenerator}.
 */
class QuickstartGeneratorTest {
    private static Path TARGET_DIR = targetDir(QuickstartGeneratorTest.class);
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
            testGeneration(HelidonVariant.SE, version -> version.toString().equals("never published"));
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("no matching version"));
        }
    }

    @Test
    void testSeGenerationSpecificVersion() {
        testGeneration(HelidonVariant.SE, version -> version.toString().equals("1.4.1"));
    }

    @Test
    void testSeGenerationLatestVersion() {
        testGeneration(HelidonVariant.SE, LATEST_RELEASE);
    }

    @Test
    void testMpGenerationLatestVersion() {
        testGeneration(HelidonVariant.MP, LATEST_RELEASE);
    }

    private void testGeneration(HelidonVariant variant, Predicate<Version> version) {
        generated = QuickstartGenerator.generator()
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

    private static Path targetDir(Class<?> testClass) {
        try {
            final Path codeSource = Paths.get(testClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            return ensureDirectory(codeSource.getParent());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
