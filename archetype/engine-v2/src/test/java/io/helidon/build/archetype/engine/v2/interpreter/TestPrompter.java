package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test class for Prompter
 */
public class TestPrompter implements Prompter {

    private List<String> requestedLabels = new ArrayList<>();

    @Override
    public LinkedList<InputAST> resolve(LinkedList<InputAST> inputs) {
        if (requestedLabels.size() == 0) {
            throw new IllegalArgumentException("RequestedLabels does not contain labels.");
        }
        LinkedList<InputAST> result = new LinkedList<>();
        List<String> labels = new ArrayList<>(requestedLabels);
        for (InputAST inputAST : inputs) {
            InputAST resultInput = new InputAST(inputAST.parent(), inputAST.currentDirectory());
            resultInput.children().addAll(
                    inputAST.children().stream()
                            .filter(i -> !(i instanceof InputNodeAST))
                            .collect(Collectors.toCollection(LinkedList::new))
            );

            LinkedList<InputNodeAST> inputNodes = inputAST.children().stream()
                    .filter(i -> i instanceof InputNodeAST)
                    .map(i -> (InputNodeAST) i)
                    .collect(Collectors.toCollection(LinkedList::new));
            for (InputNodeAST node : inputNodes) {
                if (node instanceof InputListAST || node instanceof InputEnumAST) {
                    LinkedList<Visitable> selectedOptions = node.children().stream()
                            .filter(n -> n instanceof OptionAST && requestedLabels.contains(((OptionAST) n).label()))
                            .peek(n -> labels.remove(((OptionAST) n).label()))
                            .collect(Collectors.toCollection(LinkedList::new));
                    node.children().removeIf(n -> n instanceof OptionAST);
                    node.children().addAll(selectedOptions);
                    if (!selectedOptions.isEmpty()) {
                        resultInput.children().add(node);
                    }
                } else {
                    if (requestedLabels.contains(node.label())) {
                        labels.remove(node.label());
                        resultInput.children().add(node);
                    }
                }
            }
            result.add(resultInput);
        }

        return result;
    }

    public List<String> requestedLabels() {
        return requestedLabels;
    }

    public void requestedLabels(List<String> requestedLabels) {
        this.requestedLabels = requestedLabels;
    }
}
