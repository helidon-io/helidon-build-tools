/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.helidon.build.archetype.engine.v2.ScriptCompiler.ValidationException;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.Strings;
import io.helidon.build.common.VirtualFileSystem;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.ScriptCompiler.EXPR_EVAL_ERROR;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.EXPR_INCOMPATIBLE_OPERATOR;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.EXPR_UNRESOLVED_VARIABLE;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.Options.IGNORE_ERRORS;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.Options.VALIDATE_ONLY;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.INPUT_ALREADY_DECLARED;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.INPUT_NOT_IN_STEP;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.INPUT_OPTIONAL_NO_DEFAULT;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.INPUT_TYPE_MISMATCH;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.OPTION_VALUE_ALREADY_DECLARED;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.PRESET_TYPE_MISMATCH;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.PRESET_UNRESOLVED;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.STEP_DECLARED_OPTIONAL;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.STEP_NOT_DECLARED_OPTIONAL;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.STEP_NO_INPUT;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.Options.NO_TRANSIENT;
import static io.helidon.build.archetype.engine.v2.ScriptCompiler.Options.NO_OUTPUT;
import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link ScriptCompiler}.
 */
class ScriptCompilerTest {

    @Test
    void testEmptyScript() {
        Path outputDir = compile("compiler/empty-script", "main.xml", IGNORE_ERRORS, NO_OUTPUT);
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/empty-script.xml")));
    }

    @Test
    void testEmptyMethod() {
        Path outputDir = compile("compiler/empty-method", "main.xml");
        Node node = Script.load(outputDir.resolve("main.xml"));
        assertThat(node.script().methods(), is(Map.of()));
        assertThat(node.children(), is(List.of()));
    }

    @Test
    void testExecUrl() {
        Path outputDir = compile("compiler/exec-url", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/exec-url.xml")));
    }

    @Test
    void testFiltering1() {
        Path outputDir = compile("compiler/filtering1", "main.xml", IGNORE_ERRORS, NO_OUTPUT, NO_TRANSIENT);
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/filtering1.xml")));
    }

    @Test
    void testFiltering2() {
        Path outputDir = compile("compiler/filtering2", "main.xml", NO_OUTPUT, NO_TRANSIENT);
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/filtering2.xml")));
    }

    @Test
    void testFiltering3() {
        Path outputDir = compile("compiler/filtering3", "main.xml", NO_OUTPUT);
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/filtering3.xml")));
    }

    @Test
    void testInlined1() {
        Path outputDir = compile("compiler/inlined1", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/inlined1.xml")));
    }

    @Test
    void testInlined2() {
        Path outputDir = compile("compiler/inlined2", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/inlined2.xml")));
    }

    @Test
    void testInlined3() {
        Path outputDir = compile("compiler/inlined3", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/inlined3.xml")));
    }

    @Test
    void testInlined4() {
        Path outputDir = compile("compiler/inlined4", "main.xml", IGNORE_ERRORS);
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/inlined4.xml")));
    }

    @Test
    void testInlined5() {
        Path outputDir = compile("compiler/inlined5", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/inlined5.xml")));
    }

    @Test
    void testOutput1() {
        Path outputDir = compile("compiler/output1", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/output1.xml")));
    }

    @Test
    void testOutput2() {
        Path outputDir = compile("compiler/output2", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/output2.xml")));
    }

    @Test
    void testOutput3() {
        Path outputDir = compile("compiler/output3", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/output3.xml")));
    }

    @Test
    void testOutput4() {
        Path outputDir = compile("compiler/output4", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/output4.xml")));
    }

    @Test
    void testInput1() {
        Path outputDir = compile("compiler/input1", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input1.xml")));
    }

    @Test
    void testInput2() {
        Path outputDir = compile("compiler/input2", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input2.xml")));
    }

    @Test
    void testInput3() {
        Path outputDir = compile("compiler/input3", "main.xml", IGNORE_ERRORS);
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input3.xml")));
    }

    @Test
    void testInput4() {
        Path outputDir = compile("compiler/input4", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input4.xml")));
    }

    @Test
    void testInput5() {
        Path outputDir = compile("compiler/input5", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input5.xml")));
    }

    @Test
    void testInput6() {
        Path outputDir = compile("compiler/input6", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input6.xml")));
    }

    @Test
    void testInput7() {
        Path outputDir = compile("compiler/input7", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input7.xml")));
    }

    @Test
    void testInput8() {
        Path outputDir = compile("compiler/input8", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/input8.xml")));
    }

    @Test
    void testVariables1() {
        Path outputDir = compile("compiler/variables1", "main.xml", IGNORE_ERRORS);
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/variables1.xml")));
    }

    @Test
    void testVariables2() {
        Path outputDir = compile("compiler/variables2", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/variables2.xml")));
    }

    @Test
    void testVariables3() {
        Path outputDir = compile("compiler/variables3", "main.xml");
        assertThat(normalizeXml(outputDir.resolve("main.xml")), is(normalizeXml("compiler/expected/variables3.xml")));
    }

    // TODO test normalized variables

    @Test
    void testOptionalInputWithoutDefault() {
        try {
            compile("compiler/validate", "optional-input-no-default.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(INPUT_OPTIONAL_NO_DEFAULT))));
        }
    }

    @Test
    void testOptionalStepWithNonOptionalInputs() {
        try {
            compile("compiler/validate", "optional-step-required-input.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(STEP_DECLARED_OPTIONAL))));
        }
    }

    @Test
    void testRequiredStepWithOnlyOptionalInputs() {
        try {
            compile("compiler/validate", "required-step-optional-input.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(STEP_NOT_DECLARED_OPTIONAL))));
        }
    }

    @Test
    void testRequiredStepWithinOptionalStep() {
        try {
            compile("compiler/validate", "required-step-within-optional-step.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(STEP_DECLARED_OPTIONAL))));
        }
    }

    @Test
    void testStepWithNoInputs() {
        try {
            compile("compiler/validate", "step-with-no-inputs.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(STEP_NO_INPUT))));
        }
    }

    @Test
    void testOptionalStepWithinRequiredStep() {
        compile("compiler/validate", "optional-step-within-required-step.xml", VALIDATE_ONLY);
    }

    @Test
    void testUnresolvedPreset() {
        try {
            compile("compiler/validate", "unresolved-preset.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(PRESET_UNRESOLVED))));
        }
    }

    @Test
    void testPresetTypeMismatch() {
        try {
            compile("compiler/validate", "preset-type-mismatch.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(EXPR_EVAL_ERROR),
                    containsString(PRESET_TYPE_MISMATCH))));
        }
    }

    @Test
    void testExpressionWithIncompatibleOperators() {
        try {
            compile("compiler/validate", "expression-incompatible-operators.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(EXPR_INCOMPATIBLE_OPERATOR),
                    containsString(EXPR_INCOMPATIBLE_OPERATOR),
                    containsString(EXPR_INCOMPATIBLE_OPERATOR))));
        }
    }

    @Test
    void testExpressionWithUnresolvedVariable1() {
        try {
            compile("compiler/validate", "expression-unresolved-variable1.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(EXPR_UNRESOLVED_VARIABLE))));
        }
    }

    @Test
    void testExpressionWithUnresolvedVariable2() {
        try {
            compile("compiler/validate", "expression-unresolved-variable2.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(EXPR_UNRESOLVED_VARIABLE))));
        }
    }

    @Test
    void testExpressionWithUnresolvedVariable3() {
        compile("compiler/validate", "expression-unresolved-variable3.xml", VALIDATE_ONLY);
    }

    @Test
    void testExpressionWithUnresolvedVariable4() {
        try {
            compile("compiler/validate", "expression-unresolved-variable4.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(EXPR_UNRESOLVED_VARIABLE),
                    containsString(EXPR_UNRESOLVED_VARIABLE))));
        }
    }

    @Test
    void testExpressionWithTypeMismatch1() {
        try {
            compile("compiler/validate", "expression-type-mismatch1.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(EXPR_EVAL_ERROR))));
        }
    }

    @Test
    void testExpressionWithTypeMismatch2() {
        try {
            compile("compiler/validate", "expression-type-mismatch2.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(EXPR_EVAL_ERROR))));
        }
    }

    @Test
    void testInputAlreadyDeclared1() {
        try {
            compile("compiler/validate", "input-already-declared1.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(INPUT_ALREADY_DECLARED))));
        }
    }

    @Test
    void testInputAlreadyDeclared2() {
        try {
            compile("compiler/validate", "input-already-declared2.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(INPUT_ALREADY_DECLARED))));
        }
    }

    @Test
    void testInputTypeMismatch() {
        try {
            compile("compiler/validate", "input-type-mismatch.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(INPUT_TYPE_MISMATCH))));
        }
    }

    @Test
    void testInputNotInStep() {
        try {
            compile("compiler/validate", "input-not-in-step.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(INPUT_NOT_IN_STEP))));
        }
    }

    @Test
    void testOptionValueAlreadyDeclared() {
        try {
            compile("compiler/validate", "option-value-already-declared.xml", VALIDATE_ONLY);
            fail("An exception should have been thrown");
        } catch (ValidationException ex) {
            assertThat(ex.errors(), contains(List.of(
                    containsString(OPTION_VALUE_ALREADY_DECLARED))));
        }
    }

    @Test
    void testVariationsList1() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/list1.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "list1.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsList2() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/list2.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "list2.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsList3() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/list3.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "list3.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsEnum1() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/enum1.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "enum1.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsEnum2() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/enum2.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "enum2.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsEnum3() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/enum3.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "enum3.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsBoolean1() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/boolean1.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "boolean1.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsBoolean2() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/boolean2.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "boolean2.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsBoolean3() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/boolean3.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "boolean3.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsText1() {
        Set<Map<String, String>> expected = new LinkedHashSet<>();
        expected.add(Map.of("name", "Foo"));

        Set<Map<String, String>> actual = variations("compiler/variations", "text1.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsText2() {
        Set<Map<String, String>> expected = new LinkedHashSet<>();
        expected.add(Maps.of("name", "<?>"));

        Set<Map<String, String>> actual = variations("compiler/variations", "text2.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsSubstitutions() {
        Set<Map<String, String>> expected = new LinkedHashSet<>();
        expected.add(Maps.of("text", "a-foo-a-bar", "list-things", "a-bar"));
        expected.add(Maps.of("text", "a-foo-a-bar", "list-things", "none"));

        Set<Map<String, String>> actual = variations("compiler/variations", "substitutions.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsConditionals() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/conditionals.xml");
        Set<Map<String, String>> actual = variations("compiler/variations", "conditionals.xml", List.of());
        assertThat(toString(actual), is(toString(expected)));
    }

    @Test
    void testVariationsE2e() {
        Set<Map<String, String>> actual = variations("e2e", "main.xml", List.of());
        assertThat(actual.size(), is(65604));
    }

    @Test
    void testVariationsFilters() {
        Set<Map<String, String>> expected = loadVariations("compiler/variations/expected/filtered.xml");
        List<Expression> filters = filters("compiler/variations/filters.xml");
        Set<Map<String, String>> actual = variations("e2e", "main.xml", filters);
        assertThat(toString(actual), is(toString(expected)));
    }

    static Set<Map<String, String>> variations(String path, String entrypoint, List<Expression> filters) {
        Path targetDir = targetDir(ScriptCompilerTest.class);
        try (FileSystem fs = VirtualFileSystem.create(targetDir.resolve("test-classes"))) {
            Path cwd = fs.getPath(path);
            Path source = cwd.resolve(entrypoint).toAbsolutePath().normalize();
            ScriptCompiler compiler = new ScriptCompiler(() -> source, cwd);
            return compiler.variations(filters);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex.getMessage(), ex);
        }
    }

    static Path compile(String path, String entrypoint, ScriptCompiler.Options... features) {
        Path targetDir = targetDir(ScriptCompilerTest.class);
        try (FileSystem fs = VirtualFileSystem.create(targetDir.resolve("test-classes"))) {
            Path cwd = fs.getPath(path);
            Path source = cwd.resolve(entrypoint).toAbsolutePath().normalize();
            ScriptCompiler compiler = new ScriptCompiler(() -> source, cwd);
            Path outputDir = unique(targetDir.resolve("compiler-ut"), fileName(cwd));
            compiler.compile(List.of(features)).write(outputDir);
            return outputDir;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("SameParameterValue")
    static List<Expression> filters(String path) {
        List<Expression> excludes = new ArrayList<>();
        XMLElement root = loadXml(path);
        for (XMLElement elt : root.traverse(it -> it.name().equals("exclude"))) {
            Expression exclude = Expression.TRUE;
            for (XMLElement n = elt; n.parent() != null; n = n.parent()) {
                exclude = exclude.and(Expression.create(n.attribute("if")));
            }
            excludes.add(exclude);
        }
        return excludes;
    }

    static Set<Map<String, String>> loadVariations(String path) {
        Set<Map<String, String>> result = new LinkedHashSet<>();
        for (XMLElement e : loadXml(path).children("variation")) {
            Map<String, String> map = new LinkedHashMap<>();
            for (XMLElement entry : e.children()) {
                map.put(entry.name(), entry.value());
            }
            result.add(map);
        }
        return result;
    }

    static XMLElement loadXml(String path) {
        Path targetDir = targetDir(ScriptCompilerTest.class);
        Path testClasses = targetDir.resolve("test-classes");
        try (InputStream is = Files.newInputStream(testClasses.resolve(path))) {
            return XMLElement.parse(is);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex.getMessage(), ex);
        }
    }

    static final Pattern XML_COMMENT = Pattern.compile("[^\\S\\r\\n]*<!--[^>]*-->\n", Pattern.DOTALL);
    static final Pattern XML_NAMESPACE = Pattern.compile("\\s+((xmlns|xsi)(:\\w+)?=\"[^\"]+)");

    static String toString(Set<Map<String, String>> map) {
        return String.join(System.lineSeparator(), Lists.map(map, Map::toString));
    }

    static String normalizeXml(String path) {
        return normalizeXml(testResourcePath(XMLScriptWriterTest.class, path));
    }

    static String normalizeXml(Path file) {
        try {
            String raw = Strings.normalizeNewLines(Files.readString(file));
            return normalizeXmlString(raw);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String normalizeXmlString(String xml) {
        String str = XML_COMMENT.matcher(xml).replaceAll("").trim();
        return XML_NAMESPACE.matcher(str).replaceAll("\n        $1");
    }
}
