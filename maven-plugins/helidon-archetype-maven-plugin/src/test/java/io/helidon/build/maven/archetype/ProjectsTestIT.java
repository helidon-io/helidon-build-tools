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
package io.helidon.build.maven.archetype;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import org.junit.jupiter.params.ParameterizedTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Integration test that verifies the projects under {@code src/it/projects}.
 */
final class ProjectsTestIT {

    private static final String TEST_PKG = "io.helidon.build.maven.archetype.tests";
    private static final String TEST_PKG_DIR = TEST_PKG.replaceAll("\\.", "/");

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        Path outputDir = Path.of(basedir).resolve("target/test-classes/projects/it1/project");
        assertThat(Files.exists(outputDir), is(true));
        Path shape = outputDir.resolve("src/main/java")
                              .resolve(TEST_PKG_DIR)
                              .resolve("Shape.java");
        assertThat(Files.exists(shape), is(true));
        assertThat(Files.lines(shape)
                        .filter(line -> line.contains("System.out.println(\"circle\");"))
                        .count(), is(1L));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test2(String basedir) throws IOException {
        Path outputDir = Path.of(basedir).resolve("target/test-classes/projects/it1/project");
        assertThat(Files.exists(outputDir), is(true));
        Path shape = outputDir.resolve("src/main/java")
                              .resolve(TEST_PKG_DIR)
                              .resolve("Shape.java");
        assertThat(Files.exists(shape), is(true));
        assertThat(Files.lines(shape)
                        .filter(line -> line.contains("System.out.println(\"triangle\");"))
                        .count(), is(1L));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test3(String basedir) throws IOException {
        Path outputDir = Path.of(basedir).resolve("target/test-classes/projects/it1/project");
        assertThat(Files.exists(outputDir), is(true));
        Path shape = outputDir.resolve("src/main/java")
                              .resolve(TEST_PKG_DIR)
                              .resolve("Shape.java");
        assertThat(Files.exists(shape), is(true));
        assertThat(Files.lines(shape)
                        .filter(line -> line.contains("System.out.println(\"square\");"))
                        .count(), is(1L));
    }
}
