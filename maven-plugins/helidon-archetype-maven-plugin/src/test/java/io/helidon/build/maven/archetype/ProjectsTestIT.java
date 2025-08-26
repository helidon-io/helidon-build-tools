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
package io.helidon.build.maven.archetype;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.common.test.utils.BuildLog;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.test.utils.BuildLog.assertDiffs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test that verifies the projects under {@code src/it/projects}.
 */
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
class ProjectsTestIT {

    private static final String TEST_PKG = "io.helidon.build.maven.archetype.tests";
    private static final String TEST_PKG_DIR = TEST_PKG.replaceAll("\\.", "/");

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        Path projectDir = projectsDir(basedir);
        assertProjectCount(projectDir, 3);
        assertProjectShape(projectDir, "circle");
        assertProjectShape(projectDir, "triangle");
        assertProjectShape(projectDir, "square");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test2(String basedir) throws IOException {
        Path projectDir = projectsDir(basedir);
        assertProjectCount(projectDir, 2);
        assertProjectShape(projectDir, "circle");
        assertProjectShape(projectDir, "square");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test3(String basedir) throws IOException {
        Path projectDir = projectsDir(basedir);
        assertProjectCount(projectDir, 1);
        assertProjectShape(projectDir, "rectangle");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test4(String basedir) throws IOException {
        Path projectDir = projectsDir(basedir, "module1");
        assertProjectCount(projectDir, 2);
        assertProjectShape(projectDir, "triangle");
        assertProjectShape(projectDir, "rectangle");

        projectDir = projectsDir(basedir, "module2");
        assertProjectCount(projectDir, 1);
        assertProjectShape(projectDir, "triangle");

        projectDir = projectsDir(basedir, "module3");
        assertProjectCount(projectDir, 1);
        assertProjectShape(projectDir, "rectangle");
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test5(String basedir) throws IOException {
        BuildLog log = new BuildLog(new File(basedir, "build.log"));
        List<String> diffs = log.containsLines(
                List.of("Regular expression 'foo' at validation 'dummyregex' is not JavaScript compatible"),
                String::contains,
                0);
        assertDiffs(diffs);
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test6(String basedir) throws IOException {
        BuildLog log = new BuildLog(new File(basedir, "build.log"));
        log.skipInvocations(5);
        List<String> diffs = log.containsLines(new File(basedir, "expected1.log"));
        assertDiffs(diffs);
        diffs = log.containsLines(new File(basedir, "expected2.log"));
        assertDiffs(diffs);
        diffs = log.containsLines(new File(basedir, "expected3.log"));
        assertDiffs(diffs);
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test7(String basedir) throws IOException {
        Path projectDir = projectsDir(basedir);
        assertProjectCount(projectDir, 3);
        assertProjectShape(projectDir, "circle");
        assertProjectShape(projectDir, "triangle");
        assertProjectShape(projectDir, "square");
    }

    private static Path projectsDir(String baseDir) {
        return projectsDir(baseDir, null);
    }

    private static Path projectsDir(String baseDir, String prefix) {
        Path projectsDir = Path.of(baseDir);
        if (prefix != null) {
            projectsDir = projectsDir.resolve(prefix);
        }
        projectsDir = projectsDir.resolve("target/tests");
        assertThat(Files.exists(projectsDir), is(true));
        return projectsDir;
    }

    private static List<Path> testProjects(Path projectsDir) throws IOException {
        List<Path> projects = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(projectsDir)) {
            for (Path path : paths) {
                if (fileName(path).matches(".*-project(-[0-9]+)?$")) {
                    projects.add(path);
                }
            }
        }
        return projects;
    }

    private static void assertProjectCount(Path projectsDir, int expectedCount) throws IOException {
        int projectCount = 0;
        for (Path path : testProjects(projectsDir)) {
            assertThat(Files.isDirectory(path), is(true));
            projectCount++;
        }
        assertThat(projectCount, is(expectedCount));
    }

    private static void assertProjectShape(Path projectsDir, String shape) throws IOException {
        // Check project directory
        Path outputDir = testProjects(projectsDir).stream()
                .filter(p -> fileName(p).contains(shape + "-project"))
                .findFirst()
                .orElse(null);
        assertThat(outputDir, is(not(nullValue())));
        assertThat(Files.exists(outputDir), is(true));

        // Check pom file
        Path pomFile = outputDir.resolve("pom.xml");
        String fileName = fileName(outputDir);
        assertContains(pomFile, List.of(
                "<groupId>io.helidon.build.maven.archetype.tests</groupId>",
                "<artifactId>" + fileName + "</artifactId>",
                "<version>0.1-SNAPSHOT</version>",
                "<name>" + fileName + "</name>"
        ));

        // Check source file
        Path shapeSourceFile = outputDir.resolve("src/main/java")
                                        .resolve(TEST_PKG_DIR)
                                        .resolve("Shape.java");
        assertContains(shapeSourceFile, List.of(
                "System.out.println(\"" + shape + "\");"
        ));
    }

    private static void assertContains(Path file, List<String> expected) throws IOException {
        assertThat(Files.exists(file), is(true));
        List<String> lines = Files.readAllLines(file);
        for (String expectedLine : expected) {
            assertThat(lines.stream()
                            .filter(line -> line.contains(expectedLine))
                            .count(), is(1L));
        }
    }
}
