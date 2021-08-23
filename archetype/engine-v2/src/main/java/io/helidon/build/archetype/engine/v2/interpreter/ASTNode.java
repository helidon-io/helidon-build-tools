package io.helidon.build.archetype.engine.v2.interpreter;

import java.io.Serializable;
import java.util.LinkedList;

public abstract class ASTNode implements Visitable, Serializable {

    private final LinkedList<ASTNode> children = new LinkedList<>();

    public LinkedList<ASTNode> children() {
        return children;
    }
}
