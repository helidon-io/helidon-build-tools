/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.common.VirtualFileSystem;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.readFile;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link ArchetypeEngineV2}.
 */
class ArchetypeEngineV2Test {

    // TODO test template overriding
    //  i.e. a template with the same file path declared in two places
    //  the first one to render wins (upper stack can override lower stack)

    @Test
    void testExternalDefault() {
        Path outputDir = e2eDir("testExternalDefault",
                Map.of("theme", "colors",
                        "theme.base", "rainbow"),
                Map.of("artifactId", "foo"));
        assertThat(outputDir.getFileName().toString(), startsWith("foo"));
    }

    @Test
    void testProjectName() {
        Path outputDir = e2eDir("testProjectName", Map.of(
                "theme", "colors",
                "theme.base", "rainbow",
                "artifactId", "foo"));
        assertThat(outputDir.getFileName().toString(), startsWith("foo"));
    }

    @Test
    void testRainbowColors() throws IOException {
        Path outputDir = e2eDir("testRainbowColors", Map.of(
                "theme", "colors",
                "theme.base", "rainbow"));

        assertRainbowColorsReadme(outputDir);
        assertRainbowColors(outputDir);
        assertRainbowModernStyle(outputDir);
    }

    @Test
    void testRainbowColorsClassic() throws IOException {
        Path outputDir = e2eDir("testRainbowColorsClassic", Map.of(
                "theme", "colors",
                "theme.base", "rainbow",
                "theme.base.style", "classic"));

        assertRainbowColorsReadme(outputDir);
        assertRainbowColors(outputDir);
        assertThat(Files.exists(outputDir.resolve("modern.txt")), is(false));
    }

    @Test
    void testCustomDefaultColors() throws IOException {
        Path outputDir = e2eDir("testCustomDefaultColors", Map.of(
                "theme", "colors",
                "theme.base", "custom"));

        assertCustomReadme(outputDir);
        assertDefaultCustomColors(outputDir);
        assertCustomColorsModernStyle(outputDir);
    }

    @Test
    void testCustomColors() throws IOException {
        Path outputDir = e2eDir("testCustomColors", Map.of(
                "theme", "colors",
                "theme.base", "custom",
                "theme.base.colors", "cyan,khaki"));

        assertCustomReadme(outputDir);
        assertCustomColors(outputDir);
        assertCustomColorsModernStyle(outputDir);
    }

    @Test
    void test2dShapes() throws IOException {
        Path outputDir = e2eDir("test2dShapes", Map.of(
                "theme", "shapes",
                "theme.base", "2d"));

        assert2dShapesReadme(outputDir);
        assert2dShapes(outputDir);
        assert2dModernStyle(outputDir);
    }

    @Test
    void test2dShapesClassic() throws IOException {
        Path outputDir = e2eDir("test2dShapesClassic", Map.of(
                "theme", "shapes",
                "theme.base", "2d",
                "theme.base.style", "classic"));

        assert2dShapesReadme(outputDir);
        assert2dShapes(outputDir);
        assertThat(Files.exists(outputDir.resolve("modern.txt")), is(false));
    }

    @Test
    void testCustomDefaultShapes() throws IOException {
        Path outputDir = e2eDir("testCustomDefaultShapes", Map.of(
                "theme", "shapes",
                "theme.base", "custom"));

        assertCustomReadme(outputDir);
        assertCustomDefaultShapes(outputDir);
        asserCustomShapesModernStyle(outputDir);
    }

    @Test
    void testCustomShapes() throws IOException {
        Path outputDir = e2eDir("testCustomShapes", Map.of(
                "theme", "shapes",
                "theme.base", "custom",
                "theme.base.shapes", "arrow,donut"));

        assertCustomReadme(outputDir);
        assertCustomShapes(outputDir);
        asserCustomShapesModernStyle(outputDir);
    }

    @Test
    void testRainbowColorsZip() throws IOException {
        Path outputDir = e2eZip("testRainbowColorsZip", Map.of(
                "theme", "colors",
                "theme.base", "rainbow"));

        assertRainbowColorsReadme(outputDir);
        assertRainbowColors(outputDir);
        assertRainbowModernStyle(outputDir);
    }

    @Test
    void testExternalDefaultZip() throws IOException {
        Path outputDir = e2eZip("testExternalDefaultZip",
                Map.of("theme", "colors",
                        "theme.base", "rainbow"),
                Map.of("artifactId", "not-bar"));
        assertThat(outputDir.getFileName().toString(), startsWith("not-bar"));
    }

    @Test
    void testProjectNameZip() throws IOException {
        Path outputDir = e2eZip("testProjectNameZip", Map.of(
                "theme", "colors",
                "theme.base", "rainbow",
                "artifactId", "bar"));
        assertThat(outputDir.getFileName().toString(), startsWith("bar"));
    }

    @Test
    void testCustomDefaultColorsZip() throws IOException {
        Path outputDir = e2eZip("testCustomDefaultColorsZip", Map.of(
                "theme", "colors",
                "theme.base", "custom"));

        assertCustomReadme(outputDir);
        assertDefaultCustomColors(outputDir);
        assertCustomColorsModernStyle(outputDir);
    }

    @Test
    void testRainbowColorsClassicZip() throws IOException {
        Path outputDir = e2eZip("testRainbowColorsClassicZip", Map.of(
                "theme", "colors",
                "theme.base", "rainbow",
                "theme.base.style", "classic"));

        assertRainbowColorsReadme(outputDir);
        assertRainbowColors(outputDir);
        assertThat(Files.exists(outputDir.resolve("modern.txt")), is(false));
    }

    @Test
    void testCustomColorsZip() throws IOException {
        Path outputDir = e2eZip("testCustomColorsZip", Map.of(
                "theme", "colors",
                "theme.base", "custom",
                "theme.base.colors", "cyan,khaki"));

        assertCustomReadme(outputDir);
        assertCustomColors(outputDir);
        assertCustomColorsModernStyle(outputDir);
    }

    @Test
    void test2dShapesZip() throws IOException {
        Path outputDir = e2eZip("test2dShapesZip", Map.of(
                "theme", "shapes",
                "theme.base", "2d"));

        assert2dShapesReadme(outputDir);
        assert2dShapes(outputDir);
        assert2dModernStyle(outputDir);
    }

    @Test
    void test2dShapesClassicZip() throws IOException {
        Path outputDir = e2eZip("test2dShapesClassicZip", Map.of(
                "theme", "shapes",
                "theme.base", "2d",
                "theme.base.style", "classic"));

        assert2dShapesReadme(outputDir);
        assert2dShapes(outputDir);
        assertThat(Files.exists(outputDir.resolve("modern.txt")), is(false));
    }

    @Test
    void testCustomDefaultShapesZip() throws IOException {
        Path outputDir = e2eZip("testCustomDefaultShapesZip", Map.of(
                "theme", "shapes",
                "theme.base", "custom"));

        assertCustomReadme(outputDir);
        assertCustomDefaultShapes(outputDir);
        asserCustomShapesModernStyle(outputDir);
    }

    @Test
    void testOutputPropsFile() throws IOException {
        Path outputDir = e2eZip("testOutputPropsFile", Map.of(
                "theme", "shapes",
                "theme.base", "custom"),
                "output.properties");

        assertOutputPropsFile(outputDir);
    }

    @Test
    void testCustomShapesZip() throws IOException {
        Path outputDir = e2eZip("testCustomShapesZip", Map.of(
                "theme", "shapes",
                "theme.base", "custom",
                "theme.base.shapes", "arrow,donut"));

        assertCustomReadme(outputDir);
        assertCustomShapes(outputDir);
        asserCustomShapesModernStyle(outputDir);
    }

    private void assert2dShapesReadme(Path outputDir) throws IOException {
        Path readmeFile = outputDir.resolve("README.md");
        assertThat(Files.exists(readmeFile), is(true));
        String readme = readFile(readmeFile);
        assertThat(readme, is(""
                + "# my-project\n"
                + "\n"
                + "This the README\n"
                + "\n"
                + "## A hard-coded section\n"
                + "\n"
                + "Some text.\n"
                + "\n"
                + "## About\n"
                + "\n"
                + "About 2D shapes...\n"
                + "\n"
                + "## What is 2D Shapes\n"
                + "\n"
                + "A shape library composed of 2D shapes.\n"
                + "\n"));
    }

    private void assert2dShapes(Path outputDir) throws IOException {
        Path shapesFiles = outputDir.resolve("shapes.txt");
        assertThat(Files.exists(shapesFiles), is(true));
        assertThat(readFile(shapesFiles), is(""
                + "Circle\n"
                + "Triangle\n"
                + "Rectangle\n"));
    }

    private void assertCustomDefaultShapes(Path outputDir) throws IOException {
        Path shapesFile = outputDir.resolve("shapes.txt");
        assertThat(Files.exists(shapesFile), is(true));
        assertThat(readFile(shapesFile), is(""
                + "Circle\n"
                + "Triangle\n"));
    }

    private void assertCustomShapes(Path outputDir) throws IOException {
        Path shapesFile = outputDir.resolve("shapes.txt");
        assertThat(Files.exists(shapesFile), is(true));
        assertThat(readFile(shapesFile), is(""
                + "Arrow\n"
                + "Donut\n"));
    }

    private void assert2dModernStyle(Path outputDir) throws IOException {
        Path modernFile = outputDir.resolve("modern.txt");
        assertThat(Files.exists(modernFile), is(true));
        assertThat(readFile(modernFile), is(""
                + "Modern style.\n"
                + "\n"
                + "Notes:\n"
                + "- Shapes can have many styles\n"
                + "- 2D shapes can be used for a retro style!\n"));
    }

    private void asserCustomShapesModernStyle(Path outputDir) throws IOException {
        Path modernFile = outputDir.resolve("modern.txt");
        assertThat(Files.exists(modernFile), is(true));
        assertThat(readFile(modernFile), is(""
                + "Modern style.\n"
                + "\n"
                + "Notes:\n"
                + "- Shapes can have many styles\n"));
    }

    private void assertOutputPropsFile(Path outputDir) throws IOException {
        Path outputPropsFile = outputDir.resolve("output.properties");
        assertThat(Files.exists(outputPropsFile), is(true));
        String props = readFile(outputPropsFile);
        assertThat(props, containsString("theme=shapes\n"));
        assertThat(props, containsString("theme.base=custom\n"));
    }

    private void assertCustomReadme(Path outputDir) throws IOException {
        Path readmeFile = outputDir.resolve("README.md");
        assertThat(Files.exists(readmeFile), is(true));
        String readme = readFile(readmeFile);
        assertThat(readme, is(""
                + "# my-project\n"
                + "\n"
                + "This the README\n"
                + "\n"
                + "## A hard-coded section\n"
                + "\n"
                + "Some text.\n"
                + "\n"));
    }

    private void assertRainbowColorsReadme(Path outputDir) throws IOException {
        Path readmeFile = outputDir.resolve("README.md");
        assertThat(Files.exists(readmeFile), is(true));
        String readme = readFile(readmeFile);
        assertThat(readme, is(""
                + "# my-project\n"
                + "\n"
                + "This the README\n"
                + "\n"
                + "## A hard-coded section\n"
                + "\n"
                + "Some text.\n"
                + "\n"
                + "## About\n"
                + "\n"
                + "About rainbows...\n"
                + "\n"
                + "## What is Rainbow\n"
                + "\n"
                + "A color palette composed of the rainbow colors.\n"
                + "\n"));
    }

    private void assertCustomColors(Path outputDir) throws IOException {
        Path colorsFile = outputDir.resolve("colors.txt");
        assertThat(Files.exists(colorsFile), is(true));
        assertThat(readFile(colorsFile), is(""
                + "Cyan\n"
                + "Khaki\n"));
    }

    private void assertDefaultCustomColors(Path outputDir) throws IOException {
        Path colorsFile = outputDir.resolve("colors.txt");
        assertThat(Files.exists(colorsFile), is(true));
        assertThat(readFile(colorsFile), is(""
                + "Red\n"
                + "Green\n"
                + "Blue\n"));
    }

    private void assertRainbowColors(Path outputDir) throws IOException {
        Path colorsFile = outputDir.resolve("colors.txt");
        assertThat(Files.exists(colorsFile), is(true));
        assertThat(readFile(colorsFile), is(""
                + "Rainbow colors:\n"
                + "- Red\n"
                + "- Orange\n"
                + "- Yellow\n"
                + "- Green\n"
                + "- Blue\n"
                + "- Indigo\n"
                + "- Violet\n"));
    }

    private void assertCustomColorsModernStyle(Path outputDir) throws IOException {
        Path modernFile = outputDir.resolve("modern.txt");
        assertThat(Files.exists(modernFile), is(true));
        assertThat(readFile(modernFile), is(""
                + "Modern style.\n"
                + "\n"
                + "Notes:\n"
                + "- Colors can have many styles\n"));
    }

    private void assertRainbowModernStyle(Path outputDir) throws IOException {
        Path modernFile = outputDir.resolve("modern.txt");
        assertThat(Files.exists(modernFile), is(true));
        assertThat(readFile(modernFile), is(""
                + "Modern style.\n"
                + "\n"
                + "Notes:\n"
                + "- Colors can have many styles\n"
                + "- Rainbow has a unique style\n"));
    }

    private Path e2eZip(String name, Map<String, String> externalValues) throws IOException {
        return e2eZip(name, externalValues, Map.of());
    }

    private Path e2eZip(String name, Map<String, String> externalValues, String outputPropsFile) throws IOException {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/e2e");
        Path testOutputDir = targetDir.resolve("engine-ut");
        Path zipFile = unique(testOutputDir, "archetype", ".zip");
        zip(zipFile, sourceDir);
        FileSystem fs = FileSystems.newFileSystem(zipFile, this.getClass().getClassLoader());
        Path outputDir = unique(testOutputDir, name);
        return e2e(fs, outputDir, externalValues, Map.of(), Path.of(outputPropsFile));
    }

    private Path e2eZip(String name,
                        Map<String, String> externalValues,
                        Map<String, String> externalDefaults) throws IOException {

        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/e2e");
        Path testOutputDir = targetDir.resolve("engine-ut");
        Path zipFile = unique(testOutputDir, "archetype", ".zip");
        zip(zipFile, sourceDir);
        FileSystem fs = FileSystems.newFileSystem(zipFile, this.getClass().getClassLoader());
        Path outputDir = unique(testOutputDir, name);
        return e2e(fs, outputDir, externalValues, externalDefaults, null);
    }

    private Path e2eDir(String name, Map<String, String> externalValues) {
        return e2eDir(name, externalValues, Map.of());
    }

    private Path e2eDir(String name, Map<String, String> externalValues, Map<String, String> externalDefaults) {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/e2e");
        Path testOutputDir = targetDir.resolve("engine-ut");
        FileSystem fs = VirtualFileSystem.create(sourceDir);
        Path outputDir = unique(testOutputDir, name);
        return e2e(fs, outputDir, externalValues, externalDefaults, null);
    }

    private Path e2e(FileSystem archetype,
                     Path directory,
                     Map<String, String> externalValues,
                     Map<String, String> externalDefaults,
                     Path outputPropsFile) {
        ArchetypeEngineV2 engine = ArchetypeEngineV2.builder()
                                                    .fileSystem(archetype)
                                                    .inputResolver(new BatchInputResolver())
                                                    .directorySupplier(n -> unique(directory, n))
                                                    .externalDefaults(externalDefaults)
                                                    .externalValues(externalValues)
                                                    .outputPropsFile(outputPropsFile)
                                                    .build();
        Path outputDir = engine.generate();
        assertThat(Files.exists(outputDir), is(true));
        return outputDir;
    }
}
