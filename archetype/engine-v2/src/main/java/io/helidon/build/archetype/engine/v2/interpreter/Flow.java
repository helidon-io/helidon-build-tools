package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v2.descriptor.Step;

/**
 * Input graph.
 */
public class Flow extends ASTNode {

    private final Prompter prompter;
    private final Interpreter interpreter;
    private final Archetype archetype;
    private final ArchetypeDescriptor entryPoint;
    private final LinkedList<StepAST> resolvedSteps = new LinkedList<>();
    private final LinkedList<Step> unresolvedSteps = new LinkedList<>();

    Flow(Archetype archetype, Prompter prompter) {
        this.prompter = prompter;
        this.archetype = archetype;
        //todo maybe the path to the entry point can be different
        entryPoint = archetype.getDescriptor("helidon-archetype.xml");
        interpreter = new Interpreter(prompter, archetype);
    }

    /**
     * Get the steps to build a new Helidon project.
     *
     * @return generated steps.
     */
    public LinkedList<StepAST> steps() {
        return resolvedSteps;
    }

    /**
     * Build the flow, that can be used to create a new Helidon project.
     */
    public void build() {
        unresolvedSteps.addAll(getSteps(entryPoint));
        while (!unresolvedSteps.isEmpty()) {
            Step step = unresolvedSteps.pop();
            StepAST stepAST = StepAST.from(step);
            interpreter.visit(stepAST, this);
            resolvedSteps.add(stepAST);
        }
    }

    private LinkedList<Step> getSteps(ArchetypeDescriptor entryPoint) {
        return entryPoint.steps() != null ? entryPoint.steps() : new LinkedList<>();
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    /**
     * Create a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@code Flow} builder static inner class.
     */
    public static final class Builder {

        private Prompter prompter;
        private Archetype archetype;

        private Builder() {
        }

        /**
         * Sets the {@code archetype} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param archetype the {@code archetype} to set
         * @return a reference to this Builder
         */
        public Builder archetype(Archetype archetype) {
            this.archetype = archetype;
            return this;
        }

        /**
         * Sets the {@code prompter} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param prompter the {@code prompter} to set
         * @return a reference to this Builder
         */
        public Builder prompter(Prompter prompter) {
            this.prompter = prompter;
            return this;
        }

        /**
         * Returns a {@code Flow} built from the parameters previously set.
         *
         * @return a {@code Flow} built with parameters of this {@code Flow.Builder}
         */
        public Flow build() {
            return new Flow(archetype, prompter);
        }
    }
}
