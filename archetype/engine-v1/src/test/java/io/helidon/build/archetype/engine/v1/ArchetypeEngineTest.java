/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v1;

import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.FileSet;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Property;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Replacement;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Transformation;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.PropertyEvaluator;
import io.helidon.build.common.Strings;

import io.helidon.build.common.test.utils.TestFiles;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v1.ArchetypeEngine.evaluateConditional;
import static io.helidon.build.archetype.engine.v1.ArchetypeEngine.transform;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test {@link ArchetypeEngine}.
 */
public class ArchetypeEngineTest extends ArchetypeBaseTest {

    @Test
    public void testResolveProperties() {
        Map<String, String> props = Map.of("foo", "bar", "bar", "foo");
        assertThat(PropertyEvaluator.evaluate("${foo}", props), is("bar"));
        assertThat(PropertyEvaluator.evaluate("${xxx}", props), is(""));
        assertThat(PropertyEvaluator.evaluate("-${foo}-", props), is("-bar-"));
        assertThat(PropertyEvaluator.evaluate("$${foo}}", props), is("$bar}"));
        assertThat(PropertyEvaluator.evaluate("${foo}-${bar}", props), is("bar-foo"));
        assertThat(PropertyEvaluator.evaluate("foo", props), is("foo"));
        assertThat(PropertyEvaluator.evaluate("$foo", props), is("$foo"));
        assertThat(PropertyEvaluator.evaluate("${foo", props), is("${foo"));
        assertThat(PropertyEvaluator.evaluate("${ foo}", props), is(""));
        assertThat(PropertyEvaluator.evaluate("${foo }", props), is(""));
    }

    @Test
    public void testTransformedProperties() {
        Map<String, String> props = Map.of("package", "com.example.myapp");
        assertThat(PropertyEvaluator.evaluate("${package/\\./\\/}", props), is("com/example/myapp"));
    }

    @Test
    public void testTransform() {
        Map<String, String> props = Map.of("package", "com.example.myapp");
        List<Transformation> tfs = List.of(
                new Transformation("mustache", List.of(new Replacement("\\.mustache$", ""))),
                new Transformation("packaged", List.of(new Replacement("__pkg__", "${package/\\./\\/}"))));

        String transformed = transform("src/main/java/__pkg__/Main.java.mustache", tfs, props);
        assertThat(transformed, is("src/main/java/com/example/myapp/Main.java"));
    }

    @Test
    public void testEvaluateConditional() {
        Map<String, String> props1 = Map.of("prop1", "true");
        Map<String, String> props2 = Map.of("prop1", "true", "prop2", "true");

        Property prop1 = new Property("prop1", null);
        FileSet f1 = new FileSet(null, List.of(), List.of(), List.of(), List.of(prop1), List.of());
        assertThat(evaluateConditional(f1, props1), is(true));
        assertThat(evaluateConditional(f1, Map.of()), is(false));

        FileSet f2 = new FileSet(null, List.of(), List.of(), List.of(), List.of(), List.of(prop1));
        assertThat(evaluateConditional(f2, props1), is(false));
        assertThat(evaluateConditional(f2, Map.of()), is(true));

        Property prop2 = new Property("prop2", null);
        FileSet f3 = new FileSet(null, List.of(), List.of(), List.of(), List.of(prop1, prop2), List.of());
        assertThat(evaluateConditional(f3, props2), is(true));
        assertThat(evaluateConditional(f3, props1), is(false));
        assertThat(evaluateConditional(f3, Map.of()), is(false));

        FileSet f4 = new FileSet(null, List.of(), List.of(), List.of(), List.of(prop1), List.of(prop2));
        assertThat(evaluateConditional(f4, props2), is(false));
        assertThat(evaluateConditional(f4, Map.of()), is(false));
    }

    @Test
    public void testGenerate() throws IOException {
        Map<String, String> properties = Map.of(
                "groupId", "com.example",
                "artifactId", "my-project",
                "version", "1.0-SNAPSHOT",
                "name", "my super project",
                "package", "com.example.myproject",
                "maven", "true");
        File targetDir = new File(new File("").getAbsolutePath(), "target");
        Path outputDirPath = targetDir.toPath().resolve("test-project");

        FileUtils.deleteDirectory(outputDirPath);
        assertThat(Files.exists(outputDirPath), is(false));

        try (ArchetypeEngine engine = new ArchetypeEngine(targetDir(), properties)) {
            engine.generate(outputDirPath);
        }
        assertThat(Files.exists(outputDirPath), is(true));

        try (Stream<Path> dirStream = Files.walk(outputDirPath)) {
            List<String> files = dirStream.filter(p -> !Files.isDirectory(p))
                    .map((p) -> TestFiles.pathOf(outputDirPath.relativize(p)))
                    .sorted()
                    .collect(Collectors.toList());
            assertThat(files, is(List.of("pom.xml", "src/main/java/com/example/myproject/Main.java")));
        }

        InputStream is = ArchetypeEngineTest.class.getClassLoader().getResourceAsStream("META-INF/test.properties");
        assertThat(is, is(not(nullValue())));
        Properties testProps = new Properties();
        testProps.load(is);

        String pomBase64 = testProps.getProperty("pom.xml");
        assertThat(pomBase64, is(not(nullValue())));
        assertThat(readFile(outputDirPath.resolve("pom.xml")),
                is (new String(Base64.getDecoder().decode(pomBase64), StandardCharsets.UTF_8)));

        String mainBase64 = testProps.getProperty("main.java");
        assertThat(mainBase64, is(not(nullValue())));
        assertThat(readFile(outputDirPath.resolve("src/main/java/com/example/myproject/Main.java")),
                is (new String(Base64.getDecoder().decode(mainBase64), StandardCharsets.UTF_8)));
    }

    private static String readFile(Path file) throws IOException {
        return Strings.normalizeNewLines(Files.readString(file));
    }
}
