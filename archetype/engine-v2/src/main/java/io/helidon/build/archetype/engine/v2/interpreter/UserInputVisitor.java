package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;
import java.util.stream.Collectors;

//todo implement for other inputnode types
//todo maybe need to create separate types for the user inputs and do not use children of the InputNodeAST
public class UserInputVisitor implements GenericVisitor<InputNodeAST, ASTNode> {

    @Override
    public InputNodeAST visit(InputEnumAST input, ASTNode arg) {
        InputEnumAST result = new InputEnumAST(input.label(), input.name(), input.def(), input.prompt(), null, "");
        result.help(input.help());
        result.children().addAll(input.children().stream()
                .filter(c -> c instanceof OptionAST)
                .map(o -> copyOption((OptionAST) o))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }

    @Override
    public InputNodeAST visit(InputListAST input, ASTNode arg) {
        InputListAST result = new InputListAST(input.label(), input.name(), input.def(), input.prompt(), input.min(),
                input.max(), null, "");
        result.help(input.help());
        result.children().addAll(input.children().stream()
                .filter(c -> c instanceof OptionAST)
                .map(o -> copyOption((OptionAST) o))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }

    @Override
    public InputNodeAST visit(InputBooleanAST input, ASTNode arg) {
        InputBooleanAST result = new InputBooleanAST(input.label(), input.name(), input.def(), input.prompt(), null, "");
        result.help(input.help());
        return result;
    }

    private OptionAST copyOption(OptionAST optionFrom) {
        return new OptionAST(optionFrom.label(), optionFrom.value(), optionFrom.help(), null, "");
    }
}
