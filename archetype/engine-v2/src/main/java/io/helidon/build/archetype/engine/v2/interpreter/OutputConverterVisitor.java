package io.helidon.build.archetype.engine.v2.interpreter;

public class OutputConverterVisitor extends GenericVisitorEmptyImpl<ASTNode, ASTNode> {

    @Override
    public ASTNode visit(OutputAST input, ASTNode arg) {
        OutputAST result = new OutputAST(input.parent(), input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(TransformationAST input, ASTNode arg) {
        return input;
    }

    @Override
    public ASTNode visit(FileSetsAST input, ASTNode arg) {
        return input;
    }

    @Override
    public ASTNode visit(FileSetAST input, ASTNode arg) {
        return input;
    }

    @Override
    public ASTNode visit(TemplateAST input, ASTNode arg) {
        TemplateAST result = new TemplateAST(input.engine(), input.source(), input.target(), input.parent(), input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(TemplatesAST input, ASTNode arg) {
        TemplatesAST result = new TemplatesAST(
                input.engine(),
                input.transformation(),
                input.directory(),
                input.includes(),
                input.excludes(),
                input.parent(),
                input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(ModelAST input, ASTNode arg) {
        ModelAST result = new ModelAST(input.parent(), input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(IfStatement input, ASTNode arg) {
        if (input.children().size() == 0) {
            return null;
        } else {
            return input.children().get(0).accept(this, arg);
        }
    }

    @Override
    public ASTNode visit(ModelKeyValueAST input, ASTNode arg) {
        ModelKeyValueAST result = new ModelKeyValueAST(
                input.key(),
                input.url(),
                input.file(),
                input.template(),
                input.order(),
                input.value(),
                input.parent(),
                input.location()
        );
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(ValueTypeAST input, ASTNode arg) {
        ValueTypeAST result = new ValueTypeAST(
                input.url(),
                input.file(),
                input.template(),
                input.order(),
                input.value(),
                input.parent(),
                input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(ModelKeyListAST input, ASTNode arg) {
        ModelKeyListAST result = new ModelKeyListAST(input.key(), input.order(), input.parent(), input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(MapTypeAST input, ASTNode arg) {
        MapTypeAST result = new MapTypeAST(input.order(), input.parent(), input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(ListTypeAST input, ASTNode arg) {
        ListTypeAST result = new ListTypeAST(input.order(), input.parent(), input.location());
        acceptAll(input, result);
        return result;
    }

    @Override
    public ASTNode visit(ModelKeyMapAST input, ASTNode arg) {
        ModelKeyMapAST result = new ModelKeyMapAST(input.key(), input.order(), input.parent(), input.location());
        acceptAll(input, result);
        return result;
    }

    private void acceptAll(ASTNode input, ASTNode result) {
        for (Visitable visitable : input.children()) {
            ASTNode child = visitable.accept(this, input);
            if (child != null) {
                result.children().add(child);
            }
        }
    }
}
