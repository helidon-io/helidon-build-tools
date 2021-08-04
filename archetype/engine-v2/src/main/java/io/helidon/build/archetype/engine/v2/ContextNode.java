package io.helidon.build.archetype.engine.v2;

import java.util.List;
import java.util.Optional;

public interface ContextNode {

    /**
     * Context node name.
     *
     * @return name
     */
    String name();

    /**
     * Context node value.
     *
     * @return value
     */
    Optional<ContextValue> value();

    /**
     * Context node children.
     *
     * @return children node
     */
    List<ContextNode> children();
}
