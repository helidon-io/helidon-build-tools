/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
        runTest(basedir, "circle");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test2(String basedir) throws IOException {
        runTest(basedir, "triangle");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test3(String basedir) throws IOException {
        runTest(basedir, "square");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test4(String basedir) throws IOException {
        runTest(basedir, "module1", "square");
        runTest(basedir, "module2", "rectangle");
        runTest(basedir, "module3", "triangle");
    }

    private static void runTest(String basedir, String expected) throws IOException {
        runTest(basedir, null, expected);
    }

    private static void runTest(String basedir, String prefix, String expected) throws IOException {
        Path outputDir = Path.of(basedir);
        if (prefix != null) {
            outputDir = outputDir.resolve(prefix);
        }
        outputDir = outputDir.resolve("target/test-classes/projects/it1/project");
        assertThat(Files.exists(outputDir), is(true));
        Path shape = outputDir.resolve("src/main/java")
                              .resolve(TEST_PKG_DIR)
                              .resolve("Shape.java");
        assertThat(Files.exists(shape), is(true));
        assertThat(Files.lines(shape)
                        .filter(line -> line.contains("System.out.println(\"" + expected + "\");"))
                        .count(), is(1L));
    }
}
