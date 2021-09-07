/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    Flow(Archetype archetype, String startDescriptorPath, Prompter prompter) {
        super(null, "");
        this.prompter = prompter;
        this.archetype = archetype;
        entryPoint = archetype.getDescriptor(startDescriptorPath);
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
            StepAST stepAST = StepAST.create(step, null, "");
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
        private String startDescriptorPath = "flavor.xml";

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
         * Sets a path to the start descriptor and returns a reference to this Builder so that the methods can be chained
         * together.
         *
         * @param startDescriptorPath the {@code startDescriptorPath} to set
         * @return a reference to this Builder
         */
        public Builder startDescriptorPath(String startDescriptorPath) {
            this.prompter = prompter;
            return this;
        }

        /**
         * Returns a {@code Flow} built from the parameters previously set.
         *
         * @return a {@code Flow} built with parameters of this {@code Flow.Builder}
         */
        public Flow build() {
            if (archetype == null) {
                throw new InterpreterException("Archetype must be specified.");
            }
            if (prompter == null) {
                throw new InterpreterException("Prompter must be specified.");
            }
            return new Flow(archetype, startDescriptorPath, prompter);
        }
    }
}
