package io.helidon.build.archetype.engine.v2;

import java.util.List;
import java.util.Optional;

public class ContextNodeImpl implements ContextNode {

    List<ContextNode> nodes;
    String name;

    ContextNodeImpl(String name, List<ContextNode> nodes) {
        this.name = name;
        this.nodes = nodes;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<ContextValue> value() {
        return Optional.empty();
    }

    @Override
    public List<ContextNode> children() {
        return nodes;
    }
}
