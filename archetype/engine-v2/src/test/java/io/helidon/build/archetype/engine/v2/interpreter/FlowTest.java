/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.interpreter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class FlowTest {

    private Archetype archetype;

    @Test
    public void testInnerOutputsElements() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "inner-output-elements-test.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        assertResult(flow, 5, 3);

        OutputAST output = (OutputAST) flow.result().get().outputs().stream()
                .filter(child -> child instanceof OutputAST)
                .filter(o -> ((OutputAST) o).children().size() == 1)
                .findFirst().orElse(null);
        assertThat(output, notNullValue());
        TemplatesAST templatesAST = (TemplatesAST) output.children().get(0);
        assertThat(templatesAST.children().size(), is(0));

        output = (OutputAST) flow.result().get().outputs().stream()
                .filter(child -> child instanceof OutputAST)
                .filter(o -> o.children().size() == 2)
                .findFirst().orElse(null);
        assertThat(output, notNullValue());
        templatesAST = (TemplatesAST) output.children().stream()
                .filter(ch -> ch instanceof TemplatesAST)
                .findFirst()
                .orElse(null);
        assertThat(templatesAST, notNullValue());
        assertThat(templatesAST.children().size(), is(1));
        assertThat(templatesAST.children().get(0) instanceof ModelAST, is(true));

        ModelAST modelAST = (ModelAST) templatesAST.children().get(0);
        assertThat(modelAST.children().size(), is(3));
        List<ModelKeyListAST> list = modelAST.children().stream()
                .filter(c -> c instanceof ModelKeyListAST)
                .map(c -> (ModelKeyListAST) c)
                .collect(Collectors.toList());
        assertThat(list.size(), is(1));
        assertThat(list.get(0).key(), is("dependencies"));
        assertThat(list.get(0).children().size(), is(3));
        List<ValueTypeAST> values = list.get(0).children().stream()
                .filter(c -> c instanceof ValueTypeAST)
                .map(c -> (ValueTypeAST) c).
                collect(Collectors.toList());
        assertThat(values.size(), is(1));
        assertThat(values.get(0).value(), is("you depend on ME"));
        List<ListTypeAST> innerList = list.get(0).children().stream()
                .filter(c -> c instanceof ListTypeAST)
                .map(c -> (ListTypeAST) c).
                collect(Collectors.toList());
        assertThat(innerList.size(), is(1));
        assertThat(innerList.get(0).order(), is(101));
        assertThat(innerList.get(0).children().size(), is(2));
        List<MapTypeAST> innerMap = list.get(0).children().stream()
                .filter(c -> c instanceof MapTypeAST)
                .map(c -> (MapTypeAST) c).
                collect(Collectors.toList());
        assertThat(innerMap.size(), is(1));
        assertThat(innerMap.get(0).order(), is(10));
        assertThat(innerMap.get(0).children().size(), is(2));
        List<ModelKeyMapAST> map = modelAST.children().stream()
                .filter(c -> c instanceof ModelKeyMapAST)
                .map(c -> (ModelKeyMapAST) c)
                .collect(Collectors.toList());
        assertThat(map.size(), is(1));
        assertThat(map.get(0).key(), is("foo"));
        List<ModelKeyValueAST> valueList = modelAST.children().stream()
                .filter(c -> c instanceof ModelKeyValueAST)
                .map(c -> (ModelKeyValueAST) c)
                .collect(Collectors.toList());
        assertThat(valueList.size(), is(1));
        assertThat(valueList.get(0).key(), is("key value"));

        output = (OutputAST) flow.result().get().outputs().stream()
                .filter(child -> child instanceof OutputAST)
                .filter(o -> ((OutputAST) o).children().size() == 3)
                .findFirst().orElse(null);
        assertThat(output, notNullValue());
        templatesAST = (TemplatesAST) output.children().stream()
                .filter(ch -> ch instanceof TemplatesAST)
                .findFirst()
                .orElse(null);
        assertThat(templatesAST, notNullValue());
        assertThat(templatesAST.children().size(), is(0));
    }

    @Test
    public void testIfStatement() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "if-output-test.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        assertResult(flow, 4, 1);

        OutputAST output = (OutputAST) flow.result().get().outputs().stream()
                .filter(child -> child instanceof OutputAST)
                .filter(o -> o.children().size() > 0)
                .findFirst().orElse(null);
        assertThat(output, notNullValue());
        assertThat(((TransformationAST) output.children().get(0)).id(), is("t1"));
    }

    @Test
    public void testOutput() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "output-test.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        flow.build(getUserInput("User boolean input label"));

        assertResult(flow, 1, 1);

        OutputAST output = (OutputAST) flow.result().get().outputs().stream()
                .filter(child -> child instanceof OutputAST)
                .findFirst().get();
        assertThat(output.children().size(), is(4));
    }

    @Test
    public void testSource() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "source_script_test.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());

        flow.build(getUserInput("User boolean input label"));
        ContextBooleanAST contextNode = (ContextBooleanAST) flow.pathToContextNodeMap().get("bool-input");
        assertThat(contextNode.bool(), is(true));

        SourceAST sourceAST = (SourceAST) flow.tree().get(0).children().get(0);
        InputAST inputAST = (InputAST) sourceAST.children().get(0);
        assertThat(inputAST.location().currentDirectory(), is(""));
        assertThat(inputAST.location().scriptDirectory(), is("inner"));

        assertResult(flow, 1, 1);
    }

    @Test
    public void testExec() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "exec_script_test.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());

        flow.build(getUserInput("User boolean input label"));
        ContextBooleanAST contextNode = (ContextBooleanAST) flow.pathToContextNodeMap().get("bool-input");
        assertThat(contextNode.bool(), is(true));

        ExecAST execAST = (ExecAST) flow.tree().get(0).children().get(0);
        InputAST inputAST = (InputAST) execAST.children().get(0);
        assertThat(inputAST.location().currentDirectory(), is("inner"));
        assertThat(inputAST.location().scriptDirectory(), is("inner"));

        assertResult(flow, 1, 1);
    }

    @Test
    public void testInputList() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "input-list.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        flow.build(getUserInput("Select the list1"));
        ContextListAST contextNode = (ContextListAST) flow.pathToContextNodeMap().get("list1");
        assertThat(contextNode.values(), containsInAnyOrder("option1", "option2"));

        assertResult(flow, 5, 4);
    }

    @Test
    public void testInputEnum() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "input-enum.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        flow.build(getUserInput("Enum option 1"));
        ContextEnumAST contextNode = (ContextEnumAST) flow.pathToContextNodeMap().get("enum1");
        assertThat(contextNode.value(), is("option1"));

        contextNode = (ContextEnumAST) flow.pathToContextNodeMap().get("enum_input_context");
        assertThat(contextNode.value(), is("enum context value"));

        assertResult(flow, 5, 2);
    }

    @Test
    public void testInputBoolean() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "input-boolean.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        flow.build(getUserInput("User boolean input label"));
        ContextBooleanAST contextNode = (ContextBooleanAST) flow.pathToContextNodeMap().get("bool-input");
        assertThat(contextNode.bool(), is(true));

        contextNode = (ContextBooleanAST) flow.pathToContextNodeMap().get("bool_input_context");
        assertThat(contextNode.bool(), is(true));

        assertResult(flow, 6, 2);
    }

    @Test
    public void testInputText() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "input-text.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        FlowState state = flow.build(getUserInput("User text input"));
        ContextTextAST contextNode = (ContextTextAST) flow.pathToContextNodeMap().get("user_text_input");
        assertThat(contextNode.text(), is("user text input"));

        contextNode = (ContextTextAST) flow.pathToContextNodeMap().get("user_text_input_context");
        assertThat(contextNode.text(), is("text input from context"));

        assertResult(flow, 5, 0);
    }

    @Test
    void testBuildFlowMp() {
        archetype = getArchetype("archetype");
        List<String> labels = new ArrayList<>();
        labels.add("Helidon MP");
        labels.add("Bare Helidon MP project suitable to start from scratch");
        labels.add("Apache Maven");
        labels.add("Docker support");
        labels.add("Do you want a native-image Dockerfile");
        labels.add("Do you want a jlink Dockerfile");
        labels.add("Kubernetes Support");

        Flow flow = new Flow(archetype, "flavor.xml", List.of(DebugVisitor.builder().build()));
        FlowState state = flow.build(new ContextAST());
        for (String label : labels) {
            state = flow.build(getUserInput(label));
        }
        assertThat(state.type(), is(FlowStateEnum.READY));
    }

    @AfterEach
    public void clean() throws IOException {
        if (archetype != null) {
            archetype.close();
        }
        archetype = null;
    }

    private Archetype getArchetype(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(name).getFile());
        archetype = ArchetypeFactory.create(file);
        return archetype;
    }

    private static ContextAST getUserInput(String label) {
        Map<String, ContextAST> userInputs = new HashMap<>();

        ContextAST context = new ContextAST();
        ContextTextAST contextText = new ContextTextAST("user_text_input");
        contextText.text("user text input");
        context.children().add(contextText);
        userInputs.put("User text input", context);

        context = new ContextAST();
        ContextEnumAST contextEnum = new ContextEnumAST("enum1");
        contextEnum.value("option1");
        context.children().add(contextEnum);
        userInputs.put("Enum option 1", context);

        context = new ContextAST();
        ContextListAST contextList = new ContextListAST("list1");
        contextList.values().addAll(List.of("option1", "option2"));
        context.children().add(contextList);
        userInputs.put("Select the list1", context);

        context = new ContextAST();
        ContextBooleanAST contextBool = new ContextBooleanAST("bool-input");
        contextBool.bool(true);
        context.children().add(contextBool);
        userInputs.put("User boolean input label", context);

        context = new ContextAST();
        contextEnum = new ContextEnumAST("flavor");
        contextEnum.value("mp");
        context.children().add(contextEnum);
        userInputs.put("Helidon MP", context);

        context = new ContextAST();
        contextEnum = new ContextEnumAST("base");
        contextEnum.value("bare");
        context.children().add(contextEnum);
        userInputs.put("Bare Helidon MP project suitable to start from scratch", context);

        context = new ContextAST();
        contextList = new ContextListAST("build-system");
        contextList.values().add("maven");
        context.children().add(contextList);
        userInputs.put("Apache Maven", context);

        context = new ContextAST();
        contextBool = new ContextBooleanAST("docker");
        contextBool.bool(true);
        context.children().add(contextBool);
        userInputs.put("Docker support", context);

        context = new ContextAST();
        contextBool = new ContextBooleanAST("docker.native-image");
        contextBool.bool(true);
        context.children().add(contextBool);
        userInputs.put("Do you want a native-image Dockerfile", context);

        context = new ContextAST();
        contextBool = new ContextBooleanAST("docker.jlink");
        contextBool.bool(false);
        context.children().add(contextBool);
        userInputs.put("Do you want a jlink Dockerfile", context);

        context = new ContextAST();
        contextBool = new ContextBooleanAST("kubernetes");
        contextBool.bool(true);
        context.children().add(contextBool);
        userInputs.put("Kubernetes Support", context);

        return userInputs.get(label);
    }

    private void assertResult(Flow flow, int expectedContextValuesCount, int expectedOutputCount) {
        FlowState state = flow.state();

        assertThat(state.type(), is(FlowStateEnum.READY));
        state = flow.build(new ContextAST());

        assertThat(state.type(), is(FlowStateEnum.DONE));
        Flow.Result result = flow.result().orElse(null);
        assertThat(result, notNullValue());

        assertThat(result.context().size(), is(expectedContextValuesCount));

        assertThat(result.outputs().size(), is(expectedOutputCount));
    }
}