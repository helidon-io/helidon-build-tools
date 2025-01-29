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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.Nodes.call;
import static io.helidon.build.archetype.engine.v2.Nodes.condition;
import static io.helidon.build.archetype.engine.v2.Nodes.exclude;
import static io.helidon.build.archetype.engine.v2.Nodes.excludes;
import static io.helidon.build.archetype.engine.v2.Nodes.exec;
import static io.helidon.build.archetype.engine.v2.Nodes.file;
import static io.helidon.build.archetype.engine.v2.Nodes.files;
import static io.helidon.build.archetype.engine.v2.Nodes.include;
import static io.helidon.build.archetype.engine.v2.Nodes.includes;
import static io.helidon.build.archetype.engine.v2.Nodes.inputBoolean;
import static io.helidon.build.archetype.engine.v2.Nodes.inputEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.inputList;
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
import static io.helidon.build.archetype.engine.v2.Nodes.source;
import static io.helidon.build.archetype.engine.v2.Nodes.step;
import static io.helidon.build.archetype.engine.v2.Nodes.template;
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
 * Tests {@link XMLScriptReader}.
 */
class XMLScriptReaderTest {

    @Test
    void testValidations() {
        Node node = read("reader/validations.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(validations()));
        assertThat(it.next(), is(validation("validation1", "description1")));
        assertThat(it.next(), is(regex("regex1")));
        assertThat(it.next(), is(validation("validation2", "description2")));
        assertThat(it.next(), is(regex("regex2")));
        assertThat(it.next(), is(regex("regex3")));
        assertThat(it.next(), is(regex("regex4")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testInputs() {
        Node node = read("reader/inputs.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputText("input1")
                .attribute("name", "Text input")
                .attribute("description", "A text input")
                .attribute("help", "Help 1")
                .attribute("optional", "true")
                .attribute("default", "default#1")
                .attribute("prompt", "Enter 1")));
        assertThat(it.next(), is(inputBoolean("input2")
                .attribute("name", "Boolean input")
                .attribute("description", "A boolean input")
                .attribute("help", "Help 2")
                .attribute("default", "true")
                .attribute("prompt", "Enter 2")));
        assertThat(it.next(), is(inputEnum("input3")
                .attribute("name", "Enum input")
                .attribute("description", "An enum input")
                .attribute("help", "Help 3")
                .attribute("default", "option3.1")
                .attribute("prompt", "Enter 3")));
        assertThat(it.next(), is(inputOption("Option 3.1", "option3.1")
                .attribute("description", "An option")));
        assertThat(it.next(), is(inputOption("Option 3.2", "option3.2")
                .attribute("description", "Another option")));
        assertThat(it.next(), is(inputList("input4")
                .attribute("name", "List input")
                .attribute("description", "A list input")
                .attribute("help", "Help 4")
                .attribute("default", "item4.1,item4.2")
                .attribute("prompt", "Enter 4")));
        assertThat(it.next(), is(inputOption("Item 4.1", "item4.1")
                .attribute("description", "An option")));
        assertThat(it.next(), is(inputOption("Item 4.2", "item4.2")
                .attribute("description", "Another option")));
        assertThat(it.next(), is(exec("more-inputs.xml")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testNestedInputs() {
        Node node = read("reader/nested-inputs.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputBoolean("input1")
                .attribute("name", "label1")));

        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputBoolean("input2")
                .attribute("name", "label2")));

        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputBoolean("input3")
                .attribute("name", "label3")));

        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputBoolean("input4")
                .attribute("name", "label4")));

        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputBoolean("input5")
                .attribute("name", "label5")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testInvocations() {
        Node node = read("reader/invocations.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(source("./dir1/script1.xml")));
        assertThat(it.next(), is(exec("./dir2/script2.xml")));
        assertThat(it.next(), is(call("method1")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testMethods() {
        Node node = read("reader/methods.xml");

        List<Node> methods = node.script().methods().values().stream()
                .flatMap(method -> method.collect().stream())
                .collect(Collectors.toList());

        Iterator<Node> it = methods.iterator();

        assertThat(it.next(), is(method("red")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(file("red.txt", "red.txt")));
        assertThat(it.next(), is(method("blue")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(file("blue.txt", "blue.txt")));

        List<Node> nodes = node.collect();
        it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputEnum("colors")
                .attribute("name", "Colors")));

        assertThat(it.next(), is(inputOption("Red", "red")));
        assertThat(it.next(), is(call("red")));

        assertThat(it.next(), is(inputOption("Blue", "blue")));
        assertThat(it.next(), is(call("blue")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testPresets() {
        Node node = read("reader/presets.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(presets()));
        assertThat(it.next(), is(presetBoolean("preset1", true)));
        assertThat(it.next(), is(presetText("preset2", "text1")));
        assertThat(it.next(), is(presetEnum("preset3", "enum1")));
        assertThat(it.next(), is(presetList("preset4", List.of("list1"))));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testVariables() {
        Node node = read("reader/variables.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(variables()));
        assertThat(it.next(), is(variableBoolean("var1", true)));
        assertThat(it.next(), is(variableText("var2", "text1")
                .attribute("transient", "true")));
        assertThat(it.next(), is(variableEnum("var3", "enum1")));
        assertThat(it.next(), is(variableList("var4", List.of("list1"))));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testOutput() {
        Node node = read("reader/output.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(transformation("t1")));
        assertThat(it.next(), is(replace("regex1", "token1")));

        assertThat(it.next(), is(templates("tpl-engine-1", "dir1", List.of("t1"))));
        assertThat(it.next(), is(includes()));
        assertThat(it.next(), is(include("**/*.tpl1")));

        assertThat(it.next(), is(files("dir2", List.of("t2"))));
        assertThat(it.next(), is(excludes()));
        assertThat(it.next(), is(exclude("**/*.txt")));

        assertThat(it.next(), is(template("tpl-engine-2", "file1.tpl", "file1.txt")));
        assertThat(it.next(), is(file("file1.txt", "file2.txt")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testModel() {
        Node node = read("reader/model.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(model()));

        assertThat(it.next(), is(modelMap("key1")));
        assertThat(it.next(), is(modelValue("key1.1", "value1.1")));

        assertThat(it.next(), is(modelList("key1.2")));
        assertThat(it.next(), is(modelValue("value1.2a")));
        assertThat(it.next(), is(modelValue("value1.2b")));

        assertThat(it.next(), is(modelValue("key2", "value2", 50)));

        assertThat(it.next(), is(modelList("key3")));
        assertThat(it.next(), is(modelValue("value3.1")));

        assertThat(it.next(), is(modelList()));
        assertThat(it.next(), is(modelValue("value3.2-a")));
        assertThat(it.next(), is(modelValue("value3.2-b")));

        assertThat(it.next(), is(modelMap()));
        assertThat(it.next(), is(modelValue("key3.3-a", "value3.3-a")));
        assertThat(it.next(), is(modelValue("key3.3-b", "value3.3-b")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testScopedModel() {
        Node node = read("reader/scoped-model.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(template("tpl-engine-1", "file1.tpl", "file1.txt")));
        assertThat(it.next(), is(model()));
        assertThat(it.next(), is(modelValue("key1", "value1")));
        assertThat(it.next(), is(model()));
        assertThat(it.next(), is(modelValue("key2", "value2")));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testConditional() {
        Node node = read("reader/conditional.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));

        assertThat(it.next(), is(presets()));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(presetBoolean("path1", true)));
        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(presetBoolean("path2", false)));

        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(step("Step 1")
                .attribute("help", "Help about step 1")));
        assertThat(it.next(), is(inputs()));
        assertThat(it.next(), is(inputBoolean("input1")
                .attribute("name", "Input 1")));
        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(file("file3.txt", "file4.txt")));

        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(file("file1.txt", "file2.txt")));
        assertThat(it.next(), is(condition("false")));

        assertThat(it.next(), is(file("file3.txt", "file4.txt")));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(template("foo", "file1.tpl", "file2.txt")));
        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(template("bar", "file3.tpl", "file4.txt")));

        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(files("colors")));
        assertThat(it.next(), is(includes()));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(include("red")));
        assertThat(it.next(), is(include("green")));
        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(include("blue")));
        assertThat(it.next(), is(excludes()));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(exclude("yellow")));
        assertThat(it.next(), is(exclude("pink")));
        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(exclude("purple")));

        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(files("colors2")));

        assertThat(it.next(), is(includes()));
        assertThat(it.next(), is(include("burgundy")));
        assertThat(it.next(), is(excludes()));
        assertThat(it.next(), is(exclude("beige")));

        assertThat(it.next(), is(model()));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(modelValue("red", "red")));
        assertThat(it.next(), is(modelValue("green", "green")));

        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(modelValue("blue", "blue")));

        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(modelList("colors1")));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(modelValue("yellow")));
        assertThat(it.next(), is(modelValue("pink")));
        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(modelValue("purple")));

        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(modelList("colors2")));
        assertThat(it.next(), is(modelValue("magenta")));

        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(modelMap("shapes1")));
        assertThat(it.next(), is(condition("true")));
        assertThat(it.next(), is(modelValue("rectangle", "orange")));
        assertThat(it.next(), is(modelValue("circle", "lavender")));
        assertThat(it.next(), is(condition("false")));
        assertThat(it.next(), is(modelValue("triangle", "black")));

        assertThat(it.next(), is(condition("false && !true")));
        assertThat(it.next(), is(modelMap("shapes2")));
        assertThat(it.next(), is(modelValue("circle", "white")));

        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testExec() {
        Node node = read("reader/exec.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(exec("include.xml")));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    void testNestedOutput() {
        Node node = read("reader/nested-output.xml");
        List<Node> nodes = node.collect();
        Iterator<Node> it = nodes.iterator();

        assertThat(it.next(), is(node));
        assertThat(it.next(), is(step("my step")));
        assertThat(it.next(), is(inputs()));

        assertThat(it.next(), is(inputBoolean("input1")
                .attribute("name", "label1")));
        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(model()));
        assertThat(it.next(), is(modelList("colors")));
        assertThat(it.next(), is(modelValue("yellow")));

        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(model()));
        assertThat(it.next(), is(modelList("colors")));
        assertThat(it.next(), is(modelValue("green")));

        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(model()));
        assertThat(it.next(), is(modelList("colors")));
        assertThat(it.next(), is(modelValue("red")));

        assertThat(it.next(), is(output()));
        assertThat(it.next(), is(model()));
        assertThat(it.next(), is(modelList("colors")));
        assertThat(it.next(), is(modelValue("blue")));

        assertThat(it.hasNext(), is(false));
    }

    static Node read(String path) {
        Path file = testResourcePath(XMLScriptReaderTest.class, path);
        try (XMLScriptReader reader = new XMLScriptReader(() -> file, true, Script.Loader.EMPTY)) {
            return reader.readScript();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
