package io.helidon.build.archetype.engine.v2.interpreter;

abstract class GenericVisitorEmptyImpl<T, A> implements GenericVisitor<T, A> {
    @Override
    public T visit(InputEnumAST input, A arg) {
        return null;
    }

    @Override
    public T visit(InputListAST input, A arg) {
        return null;
    }

    @Override
    public T visit(InputBooleanAST input, A arg) {
        return null;
    }

    @Override
    public T visit(InputTextAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ContextBooleanAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ContextEnumAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ContextListAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ContextTextAST input, A arg) {
        return null;
    }

    @Override
    public T visit(StepAST input, A arg) {
        return null;
    }

    @Override
    public T visit(InputAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ExecAST input, A arg) {
        return null;
    }

    @Override
    public T visit(SourceAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ContextAST input, A arg) {
        return null;
    }

    @Override
    public T visit(OptionAST input, A arg) {
        return null;
    }

    @Override
    public T visit(OutputAST input, A arg) {
        return null;
    }

    @Override
    public T visit(TransformationAST input, A arg) {
        return null;
    }

    @Override
    public T visit(FileSetsAST input, A arg) {
        return null;
    }

    @Override
    public T visit(FileSetAST input, A arg) {
        return null;
    }

    @Override
    public T visit(TemplateAST input, A arg) {
        return null;
    }

    @Override
    public T visit(TemplatesAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ModelAST input, A arg) {
        return null;
    }

    @Override
    public T visit(IfStatement input, A arg) {
        return null;
    }

    @Override
    public T visit(ModelKeyValueAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ValueTypeAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ModelKeyListAST input, A arg) {
        return null;
    }

    @Override
    public T visit(MapTypeAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ListTypeAST input, A arg) {
        return null;
    }

    @Override
    public T visit(ModelKeyMapAST input, A arg) {
        return null;
    }

    @Override
    public T visit(XmlDescriptor input, A arg) {
        return null;
    }

    @Override
    public T visit(UserInputAST input, A arg) {
        return null;
    }

    @Override
    public T visit(Visitable input, A arg) {
        return null;
    }
}
