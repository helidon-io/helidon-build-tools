package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
import java.util.stream.Collectors;

//todo implement for other inputnode types
//todo maybe need to create separate types for the user inputs and do not use children of the InputNodeAST
public class UserInputVisitor implements GenericVisitor<InputNodeAST, ASTNode> {

    @Override
    public InputNodeAST visit(InputEnumAST input, ASTNode arg) {
        InputEnumAST result = new InputEnumAST(input.label(), input.name(), input.def(), input.prompt(), null, "");
        result.children().addAll(input.children().stream()
                .filter(c -> c instanceof OptionAST).collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }

    @Override
    public InputNodeAST visit(InputListAST input, ASTNode arg) {
        return null;
    }
}
