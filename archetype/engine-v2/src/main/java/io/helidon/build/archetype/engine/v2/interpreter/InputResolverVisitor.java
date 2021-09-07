package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
import java.util.stream.Collectors;

class InputResolverVisitor extends VisitorEmptyImpl<ASTNode> {

    @Override
    public void visit(InputEnumAST input, ASTNode arg) {
        if (arg instanceof ContextEnumAST) {
            LinkedList<Visitable> resolvedOptions = input.children().stream()
                    .filter(c -> c instanceof OptionAST)
                    .filter(o -> ((OptionAST) o).value().equals(((ContextEnumAST) arg).value()))
                    .collect(Collectors.toCollection(LinkedList::new));
            input.children().removeIf(c -> c instanceof OptionAST);
            input.children().addAll(resolvedOptions);
        }
    }

    @Override
    public void visit(InputListAST input, ASTNode arg) {
        if (arg instanceof ContextListAST) {
            LinkedList<Visitable> resolvedOptions = input.children().stream()
                    .filter(c -> c instanceof OptionAST)
                    .filter(o -> ((ContextListAST) arg).values().contains(((OptionAST) o).value()))
                    .collect(Collectors.toCollection(LinkedList::new));
            input.children().removeIf(c -> c instanceof OptionAST);
            input.children().addAll(resolvedOptions);
        }
    }
}
