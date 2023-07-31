/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.maven.sitegen.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.maven.sitegen.maven.MavenPluginHelper.mojo;
import static io.helidon.build.maven.sitegen.maven.AbstractAsciiDocMojo.inputs;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link PreprocessAsciiDocMojo}.
 */
public class PreprocessAsciiDocMojoTest {

    private static final Path TARGET_DIR = targetDir(PreprocessAsciiDocMojoTest.class);
    private static final Path TEST_ROOT = TARGET_DIR.resolve("test-classes/preprocess");

    @Test
    public void testSimpleIncludes() throws IOException {
        Collection<Path> matched = inputs(TEST_ROOT,
                new String[]{"a/*.adoc"},
                new String[0]);
        assertThat(matched, hasItems(TEST_ROOT.resolve("a/a.adoc")));
    }

    @Test
    public void testDoubleStarInclude() throws IOException {
        Collection<Path> matched = inputs(TEST_ROOT,
                new String[]{"**/*.adoc"},
                new String[0]);
        assertThat(matched, hasItems(
                TEST_ROOT.resolve("a/a.adoc"),
                TEST_ROOT.resolve("b/b.adoc"),
                TEST_ROOT.resolve("b/b1/b1.adoc"),
                TEST_ROOT.resolve("b/b2/b2.adoc")));
    }

    @Test
    public void testEmbeddedDoubleStarIncludes() throws IOException {
        Collection<Path> matched = inputs(TEST_ROOT,
                new String[]{"b/**/*.adoc"},
                new String[0]);
        assertThat(matched, hasItems(
                TEST_ROOT.resolve("b/b1/b1.adoc"),
                TEST_ROOT.resolve("b/b2/b2.adoc")));
    }

    @Test
    public void testSimpleExcludes() throws IOException {
        Collection<Path> matched = inputs(TEST_ROOT,
                new String[]{"**/*.adoc"},
                new String[]{"b/b2/b2.adoc", "b/b.adoc"});
        assertThat(matched, hasItems(
                TEST_ROOT.resolve("a/a.adoc"),
                TEST_ROOT.resolve("b/b1/b1.adoc")));
    }

    @Test
    public void testDoubleStarExclude() throws IOException {
        Collection<Path> matched = inputs(TEST_ROOT,
                new String[]{"**/*.adoc"},
                new String[]{"b/**"});
        assertThat(matched, hasItems(TEST_ROOT.resolve("a/a.adoc")));
    }

    @Test
    public void testWithRealIncludes() throws Exception {
        runMojo("preprocess-mojo/pom-test-includes.xml",
                "variousIncludes-afterFullPreprocessing.adoc",
                "preprocess-adoc",
                PreprocessAsciiDocMojo.class);
    }

    @Test
    public void testWithRealIncludesAndNaturalOutput() throws Exception {
        runMojo("preprocess-mojo/pom-test-includes-natural-output.xml",
                "variousIncludes-naturalForm.adoc",
                "naturalize-adoc",
                NaturalizeAsciiDocMojo.class);
    }

    private void runMojo(String pomFile,
                         String expectedFile,
                         String goal,
                         Class<? extends AbstractAsciiDocMojo> mojoClass) throws Exception {

        AbstractAsciiDocMojo mojo = mojo(pomFile, TARGET_DIR.resolve("test-classes"), goal, mojoClass);

        mojo.execute();

        String baseDir = mojo.project().getBasedir().toPath().toString();
        Path mojoOutputPath = Paths.get(baseDir, "../../../../target/docs",
                "variousIncludes.adoc").normalize();
        List<String> mojoOutput = Files.readAllLines(mojoOutputPath);

        Path expectedOutputPath = Paths.get(baseDir, "../preprocess-adoc", expectedFile);
        List<String> expectedOutput = Files.readAllLines(expectedOutputPath);

        try {
            String reason = DiffUtils.diff(expectedOutput, mojoOutput).toString();
            assertThat(reason, expectedOutput, is(mojoOutput));
        } catch (DiffException ex) {
            throw new RuntimeException(ex);
        }
    }
}
