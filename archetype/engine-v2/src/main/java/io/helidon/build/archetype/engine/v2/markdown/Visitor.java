package io.helidon.build.archetype.engine.v2.markdown;


/**
 * Node visitor.
 */
public interface Visitor {

    void visit(FencedCodeBlock fencedCodeBlock);

    void visit(IndentedCodeBlock indentedCodeBlock);

    void visit(LinkReferenceDefinition linkReferenceDefinition);

    void visit(Paragraph paragraph);

    void visit(Document document);

    void visit(StrongEmphasis strongEmphasis);

    void visit(Emphasis emphasis);

    void visit(HtmlBlock htmlBlock);

    void visit(Code code);

    void visit(Text text);

    void visit(Link link);

    void visit(CustomNode customNode);
}
