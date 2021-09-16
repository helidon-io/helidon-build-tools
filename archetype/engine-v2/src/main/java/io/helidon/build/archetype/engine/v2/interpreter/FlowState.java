package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public abstract class FlowState {

    abstract Optional<Flow.Result> result();

    abstract void build(ContextAST context);

    abstract FlowStateEnum type();
}
