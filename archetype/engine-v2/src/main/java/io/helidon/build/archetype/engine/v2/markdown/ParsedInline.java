package io.helidon.build.archetype.engine.v2.markdown;

public class ParsedInline {
    private final Node node;
    private final Position position;

    ParsedInline(Node node, Position position) {
        this.node = node;
        this.position = position;
    }

    public Node getNode() {
        return node;
    }

    public Position getPosition() {
        return position;
    }

    public static ParsedInline none() {
        return null;
    }

    public static ParsedInline of(Node node, Position position) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        if (position == null) {
            throw new NullPointerException("position must not be null");
        }
        return new ParsedInline(node, position);
    }
}
