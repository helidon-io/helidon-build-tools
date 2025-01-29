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

package io.helidon.build.archetype.v2.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

import io.helidon.build.archetype.engine.v2.Node;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.Nodes.condition;
import static io.helidon.build.archetype.engine.v2.Nodes.inputEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.inputOption;
import static io.helidon.build.archetype.engine.v2.Nodes.inputText;
import static io.helidon.build.archetype.engine.v2.Nodes.inputs;
import static io.helidon.build.archetype.engine.v2.Nodes.method;
import static io.helidon.build.archetype.engine.v2.Nodes.model;
import static io.helidon.build.archetype.engine.v2.Nodes.modelList;
import static io.helidon.build.archetype.engine.v2.Nodes.modelValue;
import static io.helidon.build.archetype.engine.v2.Nodes.output;
import static io.helidon.build.archetype.engine.v2.Nodes.presetBoolean;
import static io.helidon.build.archetype.engine.v2.Nodes.presetEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.presetList;
import static io.helidon.build.archetype.engine.v2.Nodes.presetText;
import static io.helidon.build.archetype.engine.v2.Nodes.presets;
import static io.helidon.build.archetype.engine.v2.Nodes.regex;
import static io.helidon.build.archetype.engine.v2.Nodes.script;
import static io.helidon.build.archetype.engine.v2.Nodes.step;
import static io.helidon.build.archetype.engine.v2.Nodes.validation;
import static io.helidon.build.archetype.engine.v2.Nodes.validations;
import static io.helidon.build.archetype.engine.v2.Nodes.variableBoolean;
import static io.helidon.build.archetype.engine.v2.Nodes.variableEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.variableList;
import static io.helidon.build.archetype.engine.v2.Nodes.variableText;
import static io.helidon.build.archetype.engine.v2.Nodes.variables;
import static io.helidon.build.archetype.v2.json.JsonFactory.PRETTY_OPTIONS;
import static io.helidon.build.archetype.v2.json.JsonFactory.PROVIDER;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link JsonScriptWriter}.
 */
class JsonScriptWriterTest {

    @Test
    void testExpressions() {
        int i = 1;
        Node node = script(
                condition("['', 'adc', 'def'] contains 'foo'", step("Step" + i++)),
                condition("!(['', 'adc', 'def'] contains 'foo' == false && false)", step("Step" + i++)),
                condition("!false", step("Step" + i++)),
                condition("['', 'adc', 'def'] contains 'foo' == false && true || !false", step("Step" + i++)),
                condition("['', 'adc', 'def'] contains 'foo' == false && true || !true", step("Step" + i++)),
                condition("['', 'adc', 'def'] contains 'def'", step("Step" + i++)),
                condition("['', 'adc', 'def'] contains 'foo' == true && false", step("Step" + i++)),
                condition("['', 'adc', 'def'] contains 'foo' == false && true", step("Step" + i++)),
                condition("'aaa' == 'aaa' && ['', 'adc', 'def'] contains ''", step("Step" + i++)),
                condition("true && \"bar\" == 'foo1' || true", step("Step" + i++)),
                condition("true && \"bar\" == 'foo1' || false", step("Step" + i++)),
                condition("('def' != 'def1') && false == true", step("Step" + i++)),
                condition("('def' != 'def1') && false", step("Step" + i++)),
                condition("('def' != 'def1') && true", step("Step" + i++)),
                condition("'def' != 'def1'", step("Step" + i++)),
                condition("'def' == 'def'", step("Step" + i++)),
                condition("'def' != 'def'", step("Step" + i++)),
                condition("true==((true|| false)&&true)", step("Step" + i++)),
                condition("false==((true|| false)&&true)", step("Step" + i++)),
                condition("false==((true|| false)&&false)", step("Step" + i++)),
                condition("true == 'def'", step("Step" + i++)),
                condition("'true' || 'def'", step("Step" + i++)),
                condition("['', 'adc', 'def'] contains ['', 'adc', 'def']", step("Step" + i++)),
                condition("true == ${def}", step("Step" + i)));

        assertThat(toJson(node), is(normalizeJson("writer/expressions.json")));
    }

    @Test
    void testValidations() {
        Node script = script(validations(validation("validation1", regex("^foo"))));
        assertThat(toJson(script), is(normalizeJson("writer/validations.json")));
    }

    @Test
    void testVariables() {
        Node script = script(
                variables(
                        variableBoolean("variable-boolean1", true),
                        variableEnum("variable-enum1", "value1"),
                        variableText("variable-text1", "value1"),
                        variableList("variable-list1", List.of("value1"))));
        assertThat(toJson(script), is(normalizeJson("writer/variables.json")));
    }

    @Test
    void testPresets() {
        Node script = script(
                presets(
                        presetBoolean("preset-boolean1", true),
                        presetEnum("preset-enum1", "value1"),
                        presetText("preset-text1", "value1"),
                        presetList("preset-list1", List.of("value1"))));
        assertThat(toJson(script), is(normalizeJson("writer/presets.json")));
    }

    @Test
    void testMethods() {
        Node block = script();
        block.script().methods().put("method1", method("method1",
                output(model(modelList("model-list1", modelValue("value1"))))));
        assertThat(toJson(block), is(normalizeJson("writer/methods.json")));
    }

    @Test
    void testInputs() {
        Node script = script(
                step("Step1", b -> b.attribute("help", "Help1"),
                        inputs(
                                condition("${foo}", inputEnum("enum1", b -> b.attribute("default", "option1"),
                                        inputOption("Option1", "option1"),
                                        inputOption("Option2", "option2"))),
                                inputText("text1")
                                        .attribute("default", "Default1"))));
        assertThat(toJson(script), is(normalizeJson("writer/inputs.json")));
    }

    static String toJson(Node node) {
        StringWriter sw = new StringWriter();
        try (JsonScriptWriter writer = new JsonScriptWriter(sw, true)) {
            writer.writeScript(node);
        }
        return sw.toString();
    }

    static final JsonReaderFactory JSON_READER_FACTORY = PROVIDER.createReaderFactory(Map.of());
    static final JsonWriterFactory JSON_WRITER_FACTORY = PROVIDER.createWriterFactory(PRETTY_OPTIONS);

    static String normalizeJson(String path) {
        try {
            return normalizeJson(Files.newBufferedReader(testResourcePath(JsonScriptWriterTest.class, path)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String normalizeJson(Reader reader) {
        StringWriter sw = new StringWriter();
        JsonReader jsonReader = JSON_READER_FACTORY.createReader(reader);
        JsonWriter jsonWriter = JSON_WRITER_FACTORY.createWriter(sw);
        jsonWriter.write(jsonReader.read());
        return sw.toString();
    }
}
