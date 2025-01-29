/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import io.helidon.build.common.Strings;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.Nodes.condition;
import static io.helidon.build.archetype.engine.v2.Nodes.exclude;
import static io.helidon.build.archetype.engine.v2.Nodes.excludes;
import static io.helidon.build.archetype.engine.v2.Nodes.files;
import static io.helidon.build.archetype.engine.v2.Nodes.include;
import static io.helidon.build.archetype.engine.v2.Nodes.includes;
import static io.helidon.build.archetype.engine.v2.Nodes.inputEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.inputOption;
import static io.helidon.build.archetype.engine.v2.Nodes.inputText;
import static io.helidon.build.archetype.engine.v2.Nodes.inputs;
import static io.helidon.build.archetype.engine.v2.Nodes.method;
import static io.helidon.build.archetype.engine.v2.Nodes.model;
import static io.helidon.build.archetype.engine.v2.Nodes.modelList;
import static io.helidon.build.archetype.engine.v2.Nodes.modelMap;
import static io.helidon.build.archetype.engine.v2.Nodes.modelValue;
import static io.helidon.build.archetype.engine.v2.Nodes.output;
import static io.helidon.build.archetype.engine.v2.Nodes.presetBoolean;
import static io.helidon.build.archetype.engine.v2.Nodes.presetEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.presetList;
import static io.helidon.build.archetype.engine.v2.Nodes.presetText;
import static io.helidon.build.archetype.engine.v2.Nodes.presets;
import static io.helidon.build.archetype.engine.v2.Nodes.regex;
import static io.helidon.build.archetype.engine.v2.Nodes.replace;
import static io.helidon.build.archetype.engine.v2.Nodes.script;
import static io.helidon.build.archetype.engine.v2.Nodes.step;
import static io.helidon.build.archetype.engine.v2.Nodes.templates;
import static io.helidon.build.archetype.engine.v2.Nodes.transformation;
import static io.helidon.build.archetype.engine.v2.Nodes.validation;
import static io.helidon.build.archetype.engine.v2.Nodes.validations;
import static io.helidon.build.archetype.engine.v2.Nodes.variableBoolean;
import static io.helidon.build.archetype.engine.v2.Nodes.variableEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.variableList;
import static io.helidon.build.archetype.engine.v2.Nodes.variableText;
import static io.helidon.build.archetype.engine.v2.Nodes.variables;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link XMLScriptWriter}.
 */
class XMLScriptWriterTest {

    @Test
    void testValidations() {
        Node script = script(validations(validation("validation1", regex("^foo"))));
        assertThat(toXml(script), is(normalizeXml("writer/validations.xml")));
    }

    @Test
    void testVariables() {
        Node script = script(
                variables(
                        variableBoolean("variable-boolean1", true),
                        variableEnum("variable-enum1", "value1"),
                        variableText("variable-text1", "value1"),
                        variableText("variable-text2", ""),
                        variableList("variable-list1", List.of("value1"))));
        assertThat(toXml(script), is(normalizeXml("writer/variables.xml")));
    }

    @Test
    void testPresets() {
        Node script = script(
                presets(
                        presetBoolean("preset-boolean1", true),
                        presetEnum("preset-enum1", "value1"),
                        presetText("preset-text1", "value1"),
                        presetList("preset-list1", List.of("value1"))));
        assertThat(toXml(script), is(normalizeXml("writer/presets.xml")));
    }

    @Test
    void testMethods() {
        Node block = script();
        block.script().methods().put("method1", method("method1",
                output(model(modelList("model-list1", modelValue("value1"))))));
        assertThat(toXml(block), is(normalizeXml("writer/methods.xml")));
    }

    @Test
    void testResources() {
        Node script = script(
                output(
                        transformation("t1", replace("^t1", "r1")),
                        transformation("t2", replace("^t2", "r2")),
                        templates("mustache", "files", List.of("t1", "t2"),
                                includes(include("foo/**")),
                                excludes(exclude("bar/**"))),
                        files("files",
                                includes(include("foo/**")),
                                excludes(exclude("bar/**")))));
        assertThat(toXml(script), is(normalizeXml("writer/resources.xml")));
    }

    @Test
    void testInputs() {
        Node script = script(
                step("Step1", b -> b.attribute("help", "Help1"),
                        inputs(
                                condition("${foo}", inputEnum("enum1", b -> b
                                                .attribute("name", "Enum1")
                                                .attribute("default", "option1"),
                                        inputOption("Option1", "option1"),
                                        inputOption("Option2", "option2"))),
                                inputText("text1")
                                        .attribute("name", "Text1")
                                        .attribute("default", "Default1"))));
        assertThat(toXml(script), is(normalizeXml("writer/inputs.xml")));
    }

    @Test
    void testModel() {
        Node script = script(
                output(model(
                        modelList("list1", modelValue("value1")),
                        modelMap("map1", modelValue("key2", "value2")),
                        modelValue("key3", "value3"))));
        assertThat(toXml(script), is(normalizeXml("writer/model.xml")));
    }

    static String toXml(Node node) {
        StringWriter buf = new StringWriter();
        try (XMLScriptWriter writer = new XMLScriptWriter(buf, true)) {
            writer.writeScript(node);
        }
        return normalizeXmlString(buf.toString());
    }

    static final Pattern XML_COMMENT = Pattern.compile("[^\\S\\r\\n]*<!--[^>]*-->\n", Pattern.DOTALL);
    static final Pattern XML_NAMESPACE = Pattern.compile("\\s+((xmlns|xsi)(:\\w+)?=\"[^\"]+)");

    static String normalizeXml(String path) {
        try {
            Path file = testResourcePath(XMLScriptWriterTest.class, path);
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
