package io.helidon.build.archetype.engine.v2.interpreter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
class FlowTest {

    @Test
    void testBuild() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("archetype").getFile());
        Archetype archetype = ArchetypeFactory.create(file);
        ArchetypeDescriptor descriptor = archetype.getDescriptor("flavor.xml");
        descriptor.archetypeAttributes();

        TestPrompter prompter = new TestPrompter();
        List<String> requestedLabels = new ArrayList<>();
        requestedLabels.add("Helidon MP");
        requestedLabels.add("Bare Helidon MP project suitable to start from scratch");
        requestedLabels.add("Apache Maven");
        requestedLabels.add("Docker support");
        requestedLabels.add("Do you want a native-image Dockerfile");
        requestedLabels.add("Kubernetes Support");
        prompter.requestedLabels(requestedLabels);
        Flow flow = new Flow(archetype, "flavor.xml", prompter);
        flow.build();
        LinkedList<StepAST> steps = flow.steps();
        String expectedLabel = "Select a flavor";
        Pair parentWithChild = getParentWithChild(expectedLabel, steps);
        ASTNode child = (ASTNode) parentWithChild.children.get(0);
        assertThat(((InputEnumAST) parentWithChild.parent).label(), is(expectedLabel));
        assertThat(((OptionAST) child).label(), is("Helidon MP"));
        assertThat(((SourceAST) child.children().get(0)).source(), is("mp/mp.xml"));

        expectedLabel = "Select archetype";
        parentWithChild = getParentWithChild(expectedLabel, steps);
        child = (ASTNode) parentWithChild.children.get(0);
        assertThat(((InputEnumAST) parentWithChild.parent).label(), is(expectedLabel));
        assertThat(((OptionAST) child).label(), is("Bare Helidon MP project suitable to start from scratch"));
        assertThat(((SourceAST) child.children().get(0)).source(), is("bare/bare-mp.xml"));

        expectedLabel = "Docker";
        parentWithChild = getParentWithChild(expectedLabel, steps);
        child = (ASTNode) ((InputAST) parentWithChild.children.get(0)).children().get(0);
        assertThat(((StepAST) parentWithChild.parent).label(), is(expectedLabel));
        assertThat(((InputBooleanAST) child).label(), is("Docker support"));

        expectedLabel = "Docker support";
        parentWithChild = getParentWithChild(expectedLabel, steps);
        assertThat(((InputBooleanAST) parentWithChild.parent).label(), is(expectedLabel));
        assertThat(parentWithChild.children.get(0) instanceof OutputAST, is(true));
        child = (ASTNode) ((InputAST) parentWithChild.children.get(1)).children().get(0);
        assertThat(((InputBooleanAST) child).label(), is("Do you want a native-image Dockerfile"));

        expectedLabel = "Kubernetes Support";
        parentWithChild = getParentWithChild(expectedLabel, steps);
        assertThat(((InputBooleanAST) parentWithChild.parent).label(), is(expectedLabel));
        assertThat(parentWithChild.children.get(0) instanceof OutputAST, is(true));
    }

    private Pair getParentWithChild(String parentLabel, LinkedList<StepAST> steps) {
        for (StepAST step : steps) {
            Pair result = getParentWithChildFromStep(parentLabel, step);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Pair getParentWithChildFromStep(String parentLabel, Visitable parent) {
        if (parent instanceof InputNodeAST) {
            if (Objects.equals(((InputNodeAST) parent).label(), parentLabel)) {
                return new Pair(parent, ((InputNodeAST) parent).children());
            }
        }
        if (parent instanceof StepAST) {
            if (Objects.equals(((StepAST) parent).label(), parentLabel)) {
                return new Pair(parent, ((StepAST) parent).children());
            }
        }
        if (parent instanceof ASTNode && !((ASTNode) parent).children().isEmpty()) {
            for (Visitable child : ((ASTNode) parent).children()) {
                Pair pair = getParentWithChildFromStep(parentLabel, child);
                if (pair != null) {
                    return pair;
                }
            }
        }
        return null;
    }

    private class Pair {
        Visitable parent;
        List<Visitable> children;

        Pair(Visitable parent, List<Visitable> children) {
            this.parent = parent;
            this.children = children;
        }
    }

}