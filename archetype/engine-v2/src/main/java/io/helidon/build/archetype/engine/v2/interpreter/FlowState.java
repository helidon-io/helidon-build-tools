package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class FlowState {

//    private final LinkedList<StepAST> tree = new LinkedList<>();
//    private final List<StepAST> results = new ArrayList<>();
//    private final List<UserInputAST> unresolvedInputs = new ArrayList<>();
//    private final LinkedList<StepAST> optionalSteps = new LinkedList<>();
//    private FlowStateEnum state;

    abstract LinkedList<StepAST> tree();

    abstract List<StepAST> results();

    abstract List<UserInputAST> unresolvedInputs();

    abstract LinkedList<StepAST> optionalSteps();

    abstract void build(ContextAST context);

    abstract FlowStateEnum state();
}
