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

package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.Context.ValueKind;
import io.helidon.build.archetype.engine.v2.InputResolver.BatchResolver;
import io.helidon.build.archetype.engine.v2.ScriptInvoker.InvocationException;
import io.helidon.build.common.Strings;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link Generator}.
 */
class GeneratorTest {

    @Test
    void testContextValues() throws IOException {
        Path outputDir = generate("generator/context-values");
        Path expected = outputDir.resolve("context-values.txt");
        assertThat(Files.exists(expected), is(true));
        List<String> lines = readFile(expected).lines().collect(Collectors.toList());
        Iterator<String> it = lines.iterator();
        assertThat(it.next(), is("bar"));
        assertThat(it.next(), is("se"));
        assertThat(it.next(), is("true"));
        assertThat(it.next(), is("test variable 1"));
        assertThat(it.next(), is("test variable 2"));
        assertThat(it.next(), is("    test variable 3"));
        assertThat(it.next(), is("    test var 5 section"));
        assertThat(it.next(), is("    test var 7 section"));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testFile() throws IOException {
        Path outputDir = generate("generator/file");
        Path expected = outputDir.resolve("file2.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is("foo\n"));
    }

    @Test
    void testTemplate() throws IOException {
        Path outputDir = generate("generator/template");
        Path expected = outputDir.resolve("template1.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is("bar\n"));
    }

    @Test
    void testFiles() throws IOException {
        Path outputDir = generate("generator/files");
        Path expected1 = outputDir.resolve("file1.xml");
        assertThat(Files.exists(expected1), is(true));
        assertThat(readFile(expected1), is("<foo/>\n"));
        Path expected2 = outputDir.resolve("file2.xml");
        assertThat(Files.exists(expected2), is(true));
        assertThat(readFile(expected2), is("<bar/>\n"));
    }

    @Test
    void testTemplates() throws IOException {
        Path outputDir = generate("generator/templates");
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
                () -> generate("generator/transformation"));
        assertThat(ex.getCause(), is(instanceOf(IllegalArgumentException.class)));
        assertThat(ex.getCause().getMessage(), is("Unresolved transformation: t1"));
    }

    @Test
    void testReplacement() throws IOException {
        Path outputDir = generate("generator/replacement",
                ctx -> ctx.scope().getOrCreate("package").value(Value.of("com.example"), ValueKind.EXTERNAL));
        Path expected = outputDir.resolve("com/example/file1.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is("foo\n"));
    }

    @Test
    void testProcessedValues() throws IOException {
        Path outputDir = generate("generator/processed-values");
        Path expected = outputDir.resolve("shapes.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is(""
                + "Here is a red circle\n"
                + "Here is a blue triangle\n"
                + "Here is a green square\n"
                + "Here is a yellow rectangle\n"
                + "\n"));
    }

    @Test
    void testTemplatesOverride() throws IOException {
        Path outputDir = generate("generator/templates-override");
        Path expected = outputDir.resolve("template1.txt");
        assertThat(Files.exists(expected), is(true));
        assertThat(readFile(expected), is("nested\n"));
    }

    static Path generate(String path) {
        return generate(path, scope -> {});
    }

    static Path generate(String path, Consumer<Context> consumer) {
        Path targetDir = targetDir(GeneratorTest.class);
        Path cwd = targetDir.resolve("test-classes/" + path);
        Node node = Script.load(cwd.resolve("main.xml"));
        Path outputDir = unique(targetDir.resolve("generator-ut"), fileName(cwd));
        Context context = new Context().pushCwd(node.script().path().getParent());
        consumer.accept(context);
        TemplateModel model = resolveModel(node, context);
        Generator generator = new Generator(model, context, outputDir);
        ScriptInvoker.invoke(node, context, new BatchResolver(context), generator);
        return outputDir;
    }

    static TemplateModel resolveModel(Node scope, Context context) {
        TemplateModel model = new TemplateModel(context);
        ScriptInvoker.invoke(scope, context, new BatchResolver(context), model);
        return model;
    }

    static String readFile(Path file) throws IOException {
        return Strings.normalizeNewLines(Files.readString(file));
    }
}
