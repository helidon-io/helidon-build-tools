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
    public void testIfStatement() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "if-output-test.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        flow.build(getUserInput("User boolean input label"));

        assertResult(flow, 2, 1);
    }

    @Test
    public void testOutput() {
        archetype = getArchetype("interpreter-test-resources");

        Flow flow = new Flow(archetype, "output-test.xml", List.of(DebugVisitor.builder().build()));
        flow.build(new ContextAST());
        flow.build(getUserInput("User boolean input label"));

        assertResult(flow, 1, 1);

        OutputAST output = (OutputAST) flow.result().get().children().stream()
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

        assertResult(flow, 5, 2);
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
        ASTNode result = flow.result().orElse(null);
        assertThat(result, notNullValue());

        List<Visitable> context = result.children().stream()
                .filter(child -> child instanceof ContextAST)
                .collect(Collectors.toList());
        assertThat(context.size(), is(1));
        assertThat(((ContextAST) context.get(0)).children().size(), is(expectedContextValuesCount));

        List<Visitable> outputs = result.children().stream()
                .filter(child -> child instanceof OutputAST)
                .collect(Collectors.toList());
        assertThat(outputs.size(), is(expectedOutputCount));
    }
}