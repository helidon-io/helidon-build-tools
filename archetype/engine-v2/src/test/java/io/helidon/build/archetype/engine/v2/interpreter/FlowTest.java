package io.helidon.build.archetype.engine.v2.interpreter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class FlowTest {

    @Test
    void testBuildFlowMp() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("archetype").getFile());
        Archetype archetype = ArchetypeFactory.create(file);
        ArchetypeDescriptor descriptor = archetype.getDescriptor("flavor.xml");
        descriptor.archetypeAttributes();
        List<String> labels = new ArrayList<>();
        labels.add("Helidon MP");
        labels.add("Bare Helidon MP project suitable to start from scratch");
        labels.add("Apache Maven");
        labels.add("Docker support");
        labels.add("Do you want a native-image Dockerfile");
        labels.add("Do you want a jlink Dockerfile");
        labels.add("Kubernetes Support");

        Flow flow = new Flow(archetype, "flavor.xml");
        FlowState state = flow.build(new ContextAST());
        for (String label : labels) {
            state = flow.build(getUserInput(label));
        }
        assertThat(state.state(), is(FlowStateEnum.READY));
    }

    private static ContextAST getUserInput(String label) {
        Map<String, ContextAST> userInputs = new HashMap<>();

        ContextAST context = new ContextAST();
        ContextEnumAST contextEnum = new ContextEnumAST("flavor");
        contextEnum.value("mp");
        context.children().add(contextEnum);
        userInputs.put("Helidon MP", context);

        context = new ContextAST();
        contextEnum = new ContextEnumAST("flavor.base");
        contextEnum.value("bare");
        context.children().add(contextEnum);
        userInputs.put("Bare Helidon MP project suitable to start from scratch", context);

        context = new ContextAST();
        ContextListAST contextList = new ContextListAST("flavor.base.build-system");
        contextList.values().add("maven");
        context.children().add(contextList);
        userInputs.put("Apache Maven", context);

        context = new ContextAST();
        ContextBooleanAST contextBool = new ContextBooleanAST("flavor.base.docker");
        contextBool.bool(true);
        context.children().add(contextBool);
        userInputs.put("Docker support", context);

        context = new ContextAST();
        contextBool = new ContextBooleanAST("flavor.base.docker.native-image");
        contextBool.bool(true);
        context.children().add(contextBool);
        userInputs.put("Do you want a native-image Dockerfile", context);

        context = new ContextAST();
        contextBool = new ContextBooleanAST("flavor.base.docker.jlink");
        contextBool.bool(false);
        context.children().add(contextBool);
        userInputs.put("Do you want a jlink Dockerfile", context);

        context = new ContextAST();
        contextBool = new ContextBooleanAST("flavor.base.kubernetes");
        contextBool.bool(true);
        context.children().add(contextBool);
        userInputs.put("Kubernetes Support", context);

        return userInputs.get(label);
    }
}