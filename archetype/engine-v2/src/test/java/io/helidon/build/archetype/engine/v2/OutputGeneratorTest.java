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

package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static io.helidon.build.archetype.engine.v2.TestHelper.readFile;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link OutputGenerator}.
 */
class OutputGeneratorTest {

    @Test
    void testFile() throws IOException {
        Path outputDir = generate("generator/file.xml");
        Path expected = outputDir.resolve("file2.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is("foo\n"));
    }

    @Test
    void testTemplate() throws IOException {
        Path outputDir = generate("generator/template.xml");
        Path expected = outputDir.resolve("template1.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is("bar\n"));
    }

    @Test
    void testFiles() throws IOException {
        Path outputDir = generate("generator/files.xml");
        Path expected1 = outputDir.resolve("file1.xml");
        assertThat(Files.exists(expected1), is(true));
        assertThat(readFile(expected1), is("<foo/>\n"));
        Path expected2 = outputDir.resolve("file2.xml");
        assertThat(Files.exists(expected2), is(true));
        assertThat(readFile(expected2), is("<bar/>\n"));
    }

    @Test
    void testTemplates() throws IOException {
        Path outputDir = generate("generator/templates.xml");
        Path expected1 = outputDir.resolve("file1.txt");
        assertThat(Files.exists(expected1), is(true));
        assertThat(readFile(expected1), is("red\n"));
        Path expected2 = outputDir.resolve("file2.txt");
        assertThat(Files.exists(expected2), is(true));
        assertThat(readFile(expected2), is("circle\n"));
    }

    @Test
    void testTransformation() {
        InvocationException ex = assertThrows(InvocationException.class,
                () -> generate("generator/transformation.xml"));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
        assertThat(ex.getCause().getMessage(), is("Unresolved transformation: t1"));
    }

    @Test
    void testReplacement() throws IOException {
        Path outputDir = generate("generator/replacement.xml",
                scope -> scope.putValue("package", Value.create("com.example"), ContextValue.ValueKind.EXTERNAL));
        Path expected = outputDir.resolve("com/example/file1.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is("foo\n"));
    }

    @Test
    void testProcessedValues() throws IOException {
        Path outputDir = generate("generator/processed-values.xml");
        Path expected = outputDir.resolve("shapes.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is(""
                + "Here is a red circle\n"
                + "Here is a blue triangle\n"
                + "Here is a green square\n"
                + "Here is a yellow rectangle\n"
                + "\n"));
    }

    private static Path generate(String path) {
        return generate(path, scope -> {});
    }

    private static Path generate(String path, Consumer<ContextScope> initializer) {
        Script script = load(path);
        Path scriptPath = script.scriptPath();
        String dirname = scriptPath.getFileName().toString().replaceAll(".xml", "");
        Path target = targetDir(OutputGeneratorTest.class);
        Path outputDir = unique(target.resolve("generator-ut/"), dirname);
        Context context = Context.create(script.scriptPath().getParent());
        initializer.accept(context.scope());
        MergedModel mergedModel = MergedModel.resolveModel(script, context);
        OutputGenerator outputGenerator = new OutputGenerator(mergedModel, outputDir);
        Controller.walk(outputGenerator, script, context);
        return outputDir;
    }
}
