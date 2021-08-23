package io.helidon.build.archetype.engine.v2.interpreter;

interface Visitor<A> {

    void visit(Visitable v, A arg);

    void visit(StepAST step, A arg);

    void visit(InputAST input, A arg);

    void visit(ExecAST exec, A arg);

    void visit(SourceAST source, A arg);
}
