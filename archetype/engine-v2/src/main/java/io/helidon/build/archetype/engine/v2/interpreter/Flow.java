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

/**
 * Resolver for the archetype output files.
 */
public class Flow extends ASTNode {

    private final Interpreter interpreter;
    private final Archetype archetype;
    private final ArchetypeDescriptor entryPoint;
    private final LinkedList<StepAST> tree = new LinkedList<>();
    private FlowState state;

    public Interpreter interpreter() {
        return interpreter;
    }

    public FlowState state() {
        return state;
    }

    public void state(FlowState state) {
        this.state = state;
    }

    public Archetype archetype() {
        return archetype;
    }

    public ArchetypeDescriptor entryPoint() {
        return entryPoint;
    }

    Flow(Archetype archetype, String startDescriptorPath) {
        super(null, "");
        this.archetype = archetype;
        entryPoint = archetype.getDescriptor(startDescriptorPath);
        interpreter = new Interpreter(archetype);
        state = new InitialFlowState(this);
    }

    LinkedList<StepAST> tree() {
        return tree;
    }

    /**
     * Build the flow, that can be used to create a new Helidon project.
     *
     * @param context initial context
     * @return current state of the flow.
     */
    public FlowState build(ContextAST context) {
        state.build(context);
        return state;
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
         * Sets a path to the start descriptor and returns a reference to this Builder so that the methods can be chained
         * together.
         *
         * @param startDescriptorPath the {@code startDescriptorPath} to set
         * @return a reference to this Builder
         */
        public Builder startDescriptorPath(String startDescriptorPath) {
            this.startDescriptorPath = startDescriptorPath;
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
            return new Flow(archetype, startDescriptorPath);
        }
    }
}
