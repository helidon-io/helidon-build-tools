package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.stream.Collectors;

class ContextConvertorVisitor extends GenericVisitorEmptyImpl<String, ASTNode> {

    @Override
    public String visit(ContextBooleanAST input, ASTNode arg) {
        return String.valueOf(input.bool());
    }

    @Override
    public String visit(ContextEnumAST input, ASTNode arg) {
        return input.value();
    }

    @Override
    public String visit(ContextListAST input, ASTNode arg) {
        return input.values().stream().collect(Collectors.joining("', '", "['", "']"));
    }

    @Override
    public String visit(ContextTextAST input, ASTNode arg) {
        String text = input.text();
        if (text.startsWith("'") && text.endsWith("'")) {
            return text;
        }
        return "'" + text + "'";
    }
}
