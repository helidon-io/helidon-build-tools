package io.helidon.build.archetype.engine.v2.interpreter;

public interface GenericVisitor<T, A> {

    T visit(InputEnumAST input, A arg);

    T visit(InputListAST input, A arg);

    T visit(InputBooleanAST input, A arg);
}
