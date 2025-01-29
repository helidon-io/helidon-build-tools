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

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.helidon.build.archetype.engine.v2.Context.ValueKind;
import io.helidon.build.archetype.engine.v2.Context.Visibility;
import io.helidon.build.archetype.engine.v2.InputResolver.BatchResolver;
import io.helidon.build.archetype.engine.v2.InputResolver.InputValidationException;
import io.helidon.build.archetype.engine.v2.InputResolver.InteractiveResolver;
import io.helidon.build.archetype.engine.v2.InputResolver.InvalidInputException;
import io.helidon.build.archetype.engine.v2.Node.Visitor;
import io.helidon.build.archetype.engine.v2.ScriptInvoker.InvocationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.build.archetype.engine.v2.Nodes.inputBoolean;
import static io.helidon.build.archetype.engine.v2.Nodes.inputEnum;
import static io.helidon.build.archetype.engine.v2.Nodes.inputList;
import static io.helidon.build.archetype.engine.v2.Nodes.inputOption;
import static io.helidon.build.archetype.engine.v2.Nodes.inputText;
import static io.helidon.build.archetype.engine.v2.Nodes.model;
import static io.helidon.build.archetype.engine.v2.Nodes.modelList;
import static io.helidon.build.archetype.engine.v2.Nodes.modelValue;
import static io.helidon.build.archetype.engine.v2.Nodes.output;
import static io.helidon.build.archetype.engine.v2.Nodes.regex;
import static io.helidon.build.archetype.engine.v2.Nodes.step;
import static io.helidon.build.archetype.engine.v2.Nodes.validation;
import static io.helidon.build.archetype.engine.v2.Nodes.variableText;
import static io.helidon.build.archetype.engine.v2.Nodes.variables;
import static io.helidon.build.common.Strings.normalizeNewLines;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link InputResolver}.
 */
class InputResolverTest {

    @Test
    void testEnumOption() {
        Node step = step("step",
                inputEnum("enum-input", b -> b.attribute("default", "value3"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option3", "value3", output(model(modelList("colors", modelValue("blue")))))));

        Context context = new Context()
                .externalValues(Map.of("enum-input", "value2"));

        List<String> values = new ArrayList<>();
        invoke(step, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of(("green"))));
    }

    @Test
    void testListOptions() {
        Node step = step("step",
                inputList("list-input", b -> b.attribute("default", ""),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option3", "value3", output(model(modelList("colors", modelValue("blue")))))));

        Context context = new Context()
                .externalValues(Map.of("list-input", "value1,value3"));
        List<String> values = new ArrayList<>();
        invoke(step, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of("red", "blue")));
    }

    @Test
    void testDefaultValueSubstitutions1() {
        Node step = step("step", inputText("text-input4")
                .attribute("default", "${foo}")
                .attribute("optional", "true"));
        Context context = new Context()
                .externalValues(Map.of("foo", "bar"));
        invoke(step, context);

        Value<?> value = context.scope().get("text-input4").value();
        assertThat(value.asString().orElse(null), is("bar"));
    }

    @Test
    void testDefaultValueSubstitutions2() {
        Node step = step("step",
                variables(variableText("input1.default-input2", "value2")),
                inputBoolean("input1", inputEnum("input2", b -> b
                                .attribute("default", "${default-input2}")
                                .attribute("optional", "true"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("green"))))))));

        Context context = new Context()
                .externalValues(Map.of("input1", "true"));

        List<String> values = new ArrayList<>();
        invoke(step, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of(("green"))));
    }

    @Test
    void testExternalDefaultValueSubstitutions() {
        Context context = new Context()
                .externalDefaults(Map.of("text-input5", "${foo}"))
                .externalValues(Map.of("foo", "bar"));

        Node step = step("step", inputText("text-input5")
                .attribute("default", "foo")
                .attribute("optional", "true"));
        invoke(step, context);

        Value<?> value = context.scope().get("text-input5").value();
        assertThat(value.asString().orElse(null), is("bar"));
    }

    @Test
    void testInvalidEnumExternalValue() {
        Node step = step("step", inputEnum("enum-input", inputOption("option1", "value1")));
        Context context = new Context().externalValues(Map.of("enum-input", ""));
        InvocationException ex = assertThrows(InvocationException.class, () -> invoke(step, context));
        assertThat(ex.getCause(), is(instanceOf(InvalidInputException.class)));
        assertThat(ex.getCause().getMessage(), is("Invalid input: enum-input=''"));
    }

    @Test
    void testEnumIgnoreCase() {
        Node step = step("step",
                inputEnum("enum-input2", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue")))))));

        Context context = new Context()
                .externalValues(Map.of("enum-input2", "VALUE2"));

        List<String> values = new ArrayList<>();
        invoke(step, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of(("blue"))));
    }

    @Test
    void testEnumDefaultIgnoreCase() {
        Node step = step("step",
                inputEnum("enum-input3", b -> b
                                .attribute("default", "VALUE2")
                                .attribute("optional", "true"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue")))))));

        Context context = new Context();

        List<String> values = new ArrayList<>();
        invoke(step, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of(("blue"))));
    }

    @Test
    void testListIgnoreCase() {
        Node step = step("step",
                inputList("list-input", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option1", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option2", "value3", output(model(modelList("colors", modelValue("blue")))))));

        Context context = new Context()
                .externalValues(Map.of("list-input", "VALUE2,VALUE3"));

        List<String> values = new ArrayList<>();
        invoke(step, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of("green", "blue")));
    }

    @Test
    void testListDefaultIgnoreCase() {
        Node step = step("step",
                inputList("list-input", b -> b
                                .attribute("default", "VALUE2,VALUE3")
                                .attribute("optional", "true"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option1", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option2", "value3", output(model(modelList("colors", modelValue("blue")))))));

        Context context = new Context();

        List<String> values = new ArrayList<>();
        invoke(step, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of("green", "blue")));
    }

    @Test
    void testGlobalInputs() {
        Node nested2 = inputEnum("nested", b -> b.attribute("default", "value1"),
                inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue"))))));

        Node nested1 = inputEnum("nested-local", b -> b.attribute("default", "value1"),
                inputOption("option1", "value1", output(model(modelList("style", modelValue("plain"))))),
                inputOption("option2", "value2", nested2));

        Node nestedGlobal = inputEnum("nested-global", b -> b
                        .attribute("default", "value1")
                        .attribute("global", "true"),
                inputOption("option1", "value1", nested1));

        Node global = step("step",
                inputEnum("global", b -> b
                                .attribute("default", "value1")
                                .attribute("global", "true"),
                        inputOption("option1", "value1", nestedGlobal)));

        Context context = new Context();

        context.scope()
                .getOrCreate("global", false, Visibility.GLOBAL)
                .value(Value.of("value1"), ValueKind.EXTERNAL)
                .scope()
                .getOrCreate("nested-global", false, Visibility.GLOBAL)
                .value(Value.of("value1"), ValueKind.EXTERNAL)
                .scope()
                .getOrCreate("nested-local", false, Visibility.LOCAL)
                .value(Value.of("value2"), ValueKind.EXTERNAL)
                .scope()
                .getOrCreate("nested", false, Visibility.LOCAL)
                .value(Value.of("value2"), ValueKind.EXTERNAL);

        List<String> values = new ArrayList<>();
        invoke(global, context, visitor(n -> values.add(n.value().getString())));
        assertThat(values, is(List.of("blue")));
    }

    @Test
    void testInvalidGlobalInputs() {
        Node invalidGlobal = inputEnum("invalid-global", b -> b
                        .attribute("default", "value1")
                        .attribute("optional", "true")
                        .attribute("global", "true"),
                inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue"))))));

        Node nested = inputEnum("nested", b -> b.attribute("default", "value1"),
                inputOption("option1", "value1", invalidGlobal));

        Node global = step("step",
                inputEnum("global", b -> b
                                .attribute("default", "value1")
                                .attribute("global", "true"),
                        inputOption("option1", "value1", nested)));

        Context context = new Context();
        context.scope()
                .getOrCreate("global")
                .value(Value.of("value1"), ValueKind.EXTERNAL)
                .scope()
                .getOrCreate("global", false, Visibility.GLOBAL)
                .getOrCreate("nested", false, Visibility.LOCAL)
                .value(Value.of("value1"), ValueKind.EXTERNAL);

        InvocationException ex = assertThrows(InvocationException.class, () -> invoke(global, context));
        assertThat(ex.getCause(), is(instanceOf(IllegalStateException.class)));
        assertThat(ex.getCause().getMessage(), endsWith("parent input is not global"));
    }

    @Test
    void testSingleValidation() {
        Node node = step("step",
                validation("lower-case", "rule for lower case",
                        regex("^[a-z]+$")),
                inputText("text")
                        .attribute("default", "")
                        .attribute("validations", List.of("lower-case")));

        Context context = new Context()
                .externalValues(Map.of("text", "FOO"));

        InvocationException ex = assertThrows(InvocationException.class, () -> invoke(node, context));
        assertThat(ex.getCause(), is(instanceOf(InputValidationException.class)));
        assertThat(normalizeNewLines(ex.getCause().getMessage()), is("Invalid input: text='FOO', rules:\n\t^[a-z]+$"));
    }

    @Test
    void testMultiValidation() {
        String lowerCaseRule = "^[a-z]+$";
        String pkgRule = "^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$";
        String dummyRule = ".";

        Node step = step("step",
                validation("lower-case", "lower case rule", regex(lowerCaseRule)),
                validation("package", "package rule", regex(lowerCaseRule), regex(pkgRule), regex(dummyRule)),
                validation("dummy", "dummy", regex(dummyRule)),
                inputText("text")
                        .attribute("default", "")
                        .attribute("validations", List.of("package")));

        Context context = new Context()
                .externalValues(Map.of("text", "my.package.name"));

        InvocationException ex = assertThrows(InvocationException.class, () -> invoke(step, context));
        assertThat(ex.getCause(), is(instanceOf(InputValidationException.class)));
        assertThat(normalizeNewLines(ex.getCause().getMessage()),
                is("Invalid input: text='my.package.name', rules:\n\t^[a-z]+$\n\t."));
    }

    @Test
    void testOverride() {
        Node step = step("step",
                output(model(modelValue("foo", "foo1"), modelValue("foo", "foo2")
                        .attribute("override", "true"))));

        TemplateModel model = resolveModel(step, new Context());
        TemplateModel.ModelNode root = model.root();
        TemplateModel.ModelNode foo = root.get("foo");

        assertThat(foo, is(not(nullValue())));
        assertThat(foo, is(instanceOf(TemplateModel.Value.class)));
        assertThat(((TemplateModel.Value) foo).value(), is("foo2"));
    }

    @Test
    void testBatchInputEnumWithSingleOptionAndDefault() {
        Node step = step("step",
                inputEnum("enum-input1", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1")));

        Context context = new Context();
        ScriptInvoker.invoke(step, context, new BatchResolver(context));
        Context.ScopeValue<?> value = context.scope().get("enum-input1").value();
        assertThat(value.asString().orElse(null), is("value1"));
    }

    @Test
    void testBooleanWithEmptyResponse() {
        Node step = step("step", inputBoolean("boolean-input1")
                .attribute("default", "true"));

        Context context = prompt(step, "");

        Value<?> value = context.scope().get("boolean-input1").value();
        assertThat(value.asBoolean().orElse(false), is(true));
    }

    @Test
    void testBooleanWithEmptyResponse2() {
        Node step = step("step", inputBoolean("boolean-input2")
                .attribute("default", "false"));

        Context context = prompt(step, "");

        Value<?> value = context.scope().get("boolean-input2").value();
        assertThat(value.asBoolean().orElse(false), is(false));
    }

    @Test
    void testInputBoolean() {
        Node step = step("step", inputBoolean("boolean-input3")
                .attribute("default", "false"));

        Context context = prompt(step, "NO");

        Value<?> value = context.scope().get("boolean-input3").value();

        assertThat(value.isEmpty(), is(false));
        assertThat(value.getBoolean(), is(false));
    }

    @Test
    void testInputListWithEmptyResponse() {
        Node step = step("step",
                inputList("list-input1", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2")));

        Context context = prompt(step, "");

        Value<?> value = context.scope().get("list-input1").value();
        assertThat(value.asList().orElse(List.of()), is(List.of("value1")));
    }

    @Test
    void testInputListWithEmptyResponseMultipleDefault() {
        Node step = step("step",
                inputList("list-input2", b -> b.attribute("default", "value1,value2"),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2")));

        Context context = prompt(step, "");

        Value<?> value = context.scope().get("list-input2").value();
        assertThat(value.asList().orElse(List.of()), is(List.of("value1", "value2")));
    }

    @Test
    void testInputList() {
        Node step = step("step",
                inputList("list-input3", b -> b.attribute("default", ""),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"),
                        inputOption("option3", "value3")));

        Context context = prompt(step, "1 3");

        Value<?> value = context.scope().get("list-input3").value();
        assertThat(value.asList().orElse(List.of()), is(List.of("value1", "value3")));
    }

    @Test
    void testInputListResponseDuplicate() {
        Node step = step("step",
                inputList("list-input4", b -> b.attribute("default", ""),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"),
                        inputOption("option3", "value3")));

        Context context = prompt(step, "1 3 3 1");

        Value<?> value = context.scope().get("list-input4").value();
        assertThat(value.asList().orElse(List.of()), is(List.of("value1", "value3")));
    }

    @Test
    void testInputEnumWithEmptyResponse() {
        Node step = step("step",
                inputEnum("enum-input1", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2")));

        Context context = prompt(step, "");

        Value<?> value = context.scope().get("enum-input1").value();
        assertThat(value.asString().orElse(null), is("value1"));
    }

    @Test
    void testInputEnumWithSingleOptionAndDefault() {
        Node step = step("step",
                inputEnum("enum-input1", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1")));

        Context context = prompt(step, "2");

        Value<?> value = context.scope().get("enum-input1").value();
        assertThat(value.asString().orElse(null), is("value1"));
    }

    @Test
    void testInputEnum() {
        Node step = step("step",
                inputEnum("enum-input2", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"),
                        inputOption("option3", "value3")));

        Context context = prompt(step, "2");

        Value<?> value = context.scope().get("enum-input2").value();
        assertThat(value.asString().orElse(null), is("value2"));
    }

    @Test
    void testInputTextWithEmptyResponseNoDefault() {
        Node step = step("step", inputText("text-input1"));

        Context context = prompt(step, "");

        Value<?> value = context.scope().get("text-input1").value();
        assertThat(value.isEmpty(), is(true));
    }

    @Test
    void testInputTextWithEmptyResult() {
        Node step = step("step", inputText("text-input2")
                .attribute("default", "value1"));

        Context context = prompt(step, "");

        Value<?> value = context.scope().get("text-input2").value();
        assertThat(value.asString().orElse(null), is("value1"));
    }

    @Test
    void testInputText() {
        Node step = step("step", inputText("text-input3")
                .attribute("default", "value1"));

        Context context = prompt(step, "not-value1");

        Value<?> value = context.scope().get("text-input3").value();
        assertThat(value.asString().orElse(null), is("not-value1"));
    }

    @Test
    void testExternalDefault() {
        Node step = step("step", inputText("text-input3")
                .attribute("default", "value1"));

        Context context = new Context();
        context.externalDefaults(Map.of("text-input3", "value2"));

        prompt(step, "", context);

        Value<?> value = context.scope().get("text-input3").value();
        assertThat(value.asString().orElse(null), is("value2"));
    }

    @Test
    void testInputEnumWithSingleOptionAndDefault2() {
        Node step = step("step",
                inputEnum("enum-input1", b -> b.attribute("default", "value1"),
                        inputOption("option1", "value1")));

        Context context = new Context();
        context.externalDefaults(Map.of("text-input3", "value2"));

        List<Node> steps = new ArrayList<>();
        ScriptInvoker.invoke(step, context, new InteractiveResolver(context, new ByteArrayInputStream(new byte[0])) {
            @Override
            protected void onVisitStep(Node step) {
                steps.add(step);
                super.onVisitStep(step);
            }
        });

        assertThat(steps, is(empty()));
    }

    private final String VALIDATION_ALL_IN_ONE = "resolver/validations-all-in-one.xml";
    private final String VALIDATION_EXEC = "resolver/archetype-base.xml";

    @ParameterizedTest
    @ValueSource(strings = {VALIDATION_ALL_IN_ONE, VALIDATION_EXEC})
    void testSuccessfulValidation(String path) {
        Context context = createContext()
                .externalValues(Map.of(
                        "input1", "foo",
                        "input2", "FOO"));

        invoke(path, context);
    }

    @ParameterizedTest
    @CsvSource({
            "foo,dummy,^[A-Z]+$",
            "foo,A,[^A]+",
            "foo,B,[^B]+",
            "foo,C,[^C]+"})
    void testRegexException(String input1, String input2, String regex) {
        Context context = createContext()
                .externalValues(Map.of(
                        "input1", input1,
                        "input2", input2));

        InvocationException ex = assertThrows(InvocationException.class, () -> invoke(VALIDATION_EXEC, context));
        assertThat(ex.getCause(), is(instanceOf(InputValidationException.class)));
        assertThat(ex.getCause().getMessage(), containsString(input2));
        assertThat(ex.getCause().getMessage(), containsString(regex));
    }

    static Node load(String path) {
        return Script.load(testResourcePath(InputResolverTest.class, path));
    }

    Context createContext() {
        Path target = targetDir(InputResolverTest.class);
        Context context = new Context();
        context.pushCwd(target.resolve("test-classes/resolver"));
        return context;
    }

    static Context prompt(Node node, String userInput) {
        return prompt(node, userInput, new Context());
    }

    static Context prompt(Node node, String userInput, Context context) {
        byte[] bytes = userInput != null ? userInput.getBytes() : new byte[0];
        ScriptInvoker.invoke(node, context, new InteractiveResolver(context, new ByteArrayInputStream(bytes)));
        return context;
    }

    static void invoke(String path, Context context) {
        ScriptInvoker.invoke(load(path), context, new BatchResolver(context));
    }

    static void invoke(Node node, Context context) {
        ScriptInvoker.invoke(node, context, new BatchResolver(context));
    }

    static void invoke(Node node, Context context, Visitor visitor) {
        ScriptInvoker.invoke(node, context, new BatchResolver(context), visitor);
    }

    static TemplateModel resolveModel(Node scope, Context context) {
        TemplateModel model = new TemplateModel(context);
        ScriptInvoker.invoke(scope, context, new BatchResolver(context), model);
        return model;
    }

    static Visitor visitor(Consumer<Node> consumer) {
        return (node) -> {
            if (node.kind() == Node.Kind.MODEL_VALUE) {
                consumer.accept(node);
            }
            return true;
        };
    }
}
