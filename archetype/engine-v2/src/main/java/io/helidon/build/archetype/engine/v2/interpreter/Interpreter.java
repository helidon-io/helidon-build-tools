package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;

/**
 * Interpret user inputs and produce new steps.
 */
public class Interpreter implements Visitor<ASTNode> {

    private final Prompter prompter;
    private final Archetype archetype;

    Interpreter(Prompter prompter, Archetype archetype) {
        this.prompter = prompter;
        this.archetype = archetype;
    }

    @Override
    public void visit(Visitable v, ASTNode parent) {
        v.accept(this, parent);
    }

    @Override
    public void visit(StepAST step, ASTNode parent) {
        resolveInputs(step);
        acceptAll(step.children(), step);
    }

    @Override
    public void visit(InputAST input, ASTNode parent) {
        resolveInputs(input);
        acceptAll(input.children(), input);
    }

    @Override
    public void visit(ExecAST exec, ASTNode parent) {
        ArchetypeDescriptor descriptor = archetype.getDescriptor(exec.src());

        LinkedList<StepAST> steps = descriptor.steps().stream()
                .map(StepAST::from).collect(Collectors.toCollection(LinkedList::new));
        parent.children().addAll(steps);
        acceptAll(steps, parent);

        LinkedList<InputAST> inputs = descriptor.inputs().stream()
                .map(InputAST::from).collect(Collectors.toCollection(LinkedList::new));
        parent.children().addAll(inputs);
        resolveInputs(parent);
        acceptAll(inputs, parent);

        LinkedList<SourceAST> sources = descriptor.sources().stream()
                .map(SourceAST::from).collect(Collectors.toCollection(LinkedList::new));
        parent.children().addAll(sources);
        acceptAll(sources, parent);

        LinkedList<ExecAST> execs = descriptor.execs().stream()
                .map(ExecAST::from).collect(Collectors.toCollection(LinkedList::new));
        parent.children().addAll(execs);
        acceptAll(execs, parent);
        //todo process other content of the descriptor

    }

    @Override
    public void visit(SourceAST source, ASTNode parent) {
        //todo need to be implemented (maybe the same as the method visit(exec ...))
    }

    private void acceptAll(LinkedList<? extends ASTNode> nodes, ASTNode parent) {
        LinkedList<ASTNode> list = new LinkedList<>(nodes);
        while (!list.isEmpty()) {
            list.pop().accept(this, parent);
        }
    }

    private LinkedList<InputAST> resolveInputs(ASTNode parent) {
        LinkedList<InputAST> result = new LinkedList<>();
        LinkedList<InputAST> unresolvedInputs = parent.children().stream()
                .filter(node -> node instanceof InputAST)
                .map(node -> (InputAST) node)
                .collect(Collectors.toCollection(LinkedList::new));
        if (!unresolvedInputs.isEmpty()) {
            LinkedList<InputAST> resolvedInputs = prompter.resolve(unresolvedInputs);
            result.addAll(resolvedInputs);
            //replace inputs in the parent node
            parent.children().removeIf(node -> node instanceof InputAST);
            parent.children().addAll(resolvedInputs);
        }
        return result;
    }
}
