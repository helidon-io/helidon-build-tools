package io.helidon.build.archetype.engine.v2;

import java.util.List;
import java.util.Optional;

public interface ContextNode {
    String name();
    Optional<ContextValue> value();
    List<ContextNode> children();
}
