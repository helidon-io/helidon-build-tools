/*
 * Copyright (c) 2018-2019 Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package io.helidon.sitegen.maven;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import io.helidon.sitegen.MavenPluginHelper;
import static io.helidon.sitegen.TestHelper.getFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the include preprocessing maven plug-in mojo.
 */
public class PreprocessAsciiDocMojoTest {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final File BASIC2_OUTPUT_DIR = getFile("target/basic-backend-test");

    private static final Path TEST_ROOT = Paths.get("src/test/resources/testpreprocess");
    private static final Path INCLUDES_TEST_ROOT = Paths.get("src/test/resources/preprocess-adoc");

    @Test
    public void testSimpleIncludes() throws IOException {

        Collection<Path> expected = new HashSet<>(Arrays.asList(new Path[]{
            TEST_ROOT.resolve("a/a.adoc")
        }));

        Collection<Path> matched = PreprocessAsciiDocMojo.inputs(TEST_ROOT,
                new String[]{"a/*.adoc"},
                EMPTY_STRING_ARRAY);

        assertEquals(expected, matched);
    }

    @Test
    public void testDoubleStarInclude() throws IOException {
        Set<Path> expected = new HashSet<>(Arrays.asList(new Path[]{
            TEST_ROOT.resolve("a/a.adoc"),
            TEST_ROOT.resolve("b/b.adoc"),
            TEST_ROOT.resolve("b/b1/b1.adoc"),
            TEST_ROOT.resolve("b/b2/b2.adoc")

        }));

        Collection<Path> matched = PreprocessAsciiDocMojo.inputs(TEST_ROOT,
                new String[]{"**/*.adoc"},
                EMPTY_STRING_ARRAY);

        assertEquals(expected, matched);
    }

    @Test
    public void testEmbeddedDoubleStarIncludes() throws IOException {
        Set<Path> expected = new HashSet<>(Arrays.asList(new Path[]{
            TEST_ROOT.resolve("b/b1/b1.adoc"),
            TEST_ROOT.resolve("b/b2/b2.adoc"),}));

        Collection<Path> matched = PreprocessAsciiDocMojo.inputs(TEST_ROOT,
                new String[]{"b/**/*.adoc"},
                EMPTY_STRING_ARRAY);

        assertEquals(expected, matched);
    }

    @Test
    public void testSimpleExcludes() throws IOException {
        Set<Path> expected = new HashSet<>(Arrays.asList(new Path[]{
            TEST_ROOT.resolve("a/a.adoc"),
            TEST_ROOT.resolve("b/b1/b1.adoc")
        }));

        Collection<Path> matched = PreprocessAsciiDocMojo.inputs(TEST_ROOT,
                new String[]{"**/*.adoc"},
                new String[]{"b/b2/b2.adoc", "b/b.adoc"}
        );

        assertEquals(expected, matched);
    }

    @Test
    public void testDoubleStarExclude() throws IOException {
        Set<Path> expected = new HashSet<>(Arrays.asList(new Path[]{
            TEST_ROOT.resolve("a/a.adoc"),}));

        Collection<Path> matched = PreprocessAsciiDocMojo.inputs(TEST_ROOT,
                new String[]{"**/*.adoc"},
                new String[]{"b/**"}
        );

        assertEquals(expected, matched);
    }

    @Test
    public void testWithRealIncludes() throws Exception {
        runMojo("preprocess-mojo/pom-test-includes.xml",
                "variousIncludes-afterFullPreprocessing.adoc");
    }

    @Test
    public void testWithRealIncludesAndNaturalOutput() throws Exception {
        runMojo("preprocess-mojo/pom-test-includes-natural-output.xml",
                "variousIncludes-naturalForm.adoc");
    }

    private void runMojo(String pomFile, String expectedFile) throws Exception {
        PreprocessAsciiDocMojo mojo = MavenPluginHelper.getInstance().getMojo(
                pomFile,
                INCLUDES_TEST_ROOT.toFile(),
                "preprocess-adoc",
                PreprocessAsciiDocMojo.class);
        mojo.execute();

        String baseDir = mojo.project().getBasedir().toPath().toString();
        Path mojoOutputPath = Paths.get(baseDir, "../../../../target/docs",
                "variousIncludes.adoc").normalize();
        List<String> mojoOutput = Files.readAllLines(mojoOutputPath);

        Path expectedOutputPath = Paths.get(
                baseDir,
                "../preprocess-adoc",
                expectedFile);
        List<String> expectedOutput = Files.readAllLines(expectedOutputPath);

        assertEquals(expectedOutput, mojoOutput, () -> {
            try {
                return DiffUtils.diff(expectedOutput, mojoOutput).toString();
            } catch (DiffException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
