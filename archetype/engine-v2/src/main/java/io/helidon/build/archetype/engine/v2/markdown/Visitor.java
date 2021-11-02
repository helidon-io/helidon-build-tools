package io.helidon.build.archetype.engine.v2.markdown;


/**
 * Node visitor.
 */
public interface Visitor {

    default void visit(FencedCodeBlock fencedCodeBlock) {
        visitChildren(fencedCodeBlock);
    }

    default void visit(Paragraph paragraph) {
        visitChildren(paragraph);
    }

    default void visit(Document document) {
        visitChildren(document);
    }

    default void visit(StrongEmphasis strongEmphasis) {
        visitChildren(strongEmphasis);
    }

    default void visit(Emphasis emphasis) {
        visitChildren(emphasis);
    }

    default void visit(Code code) {
        visitChildren(code);
    }

    default void visit(Text text) {
        visitChildren(text);
    }

    default void visit(Link link) {
        visitChildren(link);
    }

    default void visit(CustomNode customNode) {
        visitChildren(customNode);
    }

    /**
     * Visit the child nodes.
     *
     * @param parent the parent node whose children should be visited
     */
    private void visitChildren(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null) {
            // A subclass of this visitor might modify the node, resulting in getNext returning a different node or no
            // node after visiting it. So get the next node before visiting.
            Node next = node.getNext();
            node.accept(this);
            node = next;
        }
    }
}
