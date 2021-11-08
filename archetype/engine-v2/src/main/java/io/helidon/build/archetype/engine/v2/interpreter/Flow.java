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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;

/**
 * Resolver for the archetype output files.
 */
public class Flow {

    private final Interpreter interpreter;
    private final Archetype archetype;
    private final ArchetypeDescriptor entryPoint;
    private final LinkedList<StepAST> tree = new LinkedList<>();
    private FlowState state;
    private final List<Visitor<ASTNode>> additionalVisitors;
    private boolean skipOptional;

    /**
     * Returns the flag that indicates whether the interpreter skips optional inputs and uses their default values
     * or stops its execution and waits for user input.
     *
     * @return true if the interpreter skips optional inputs, false - otherwise.
     */
    public boolean skipOptional() {
        return skipOptional;
    }

    /**
     * Set the flag that indicates whether the interpreter has to skip optional inputs and uses their default values or it has
     * to stop its execution and waits for user input.
     *
     * @param skipOptional true if the interpreter has to skip optional inputs, false - otherwise.
     */
    public void skipOptional(boolean skipOptional) {
        this.skipOptional = skipOptional;
        interpreter.skipOptional(skipOptional);
    }

    /**
     * Get the mark that indicates whether the project can be generated if optional inputs will be skipped.
     *
     * @return true if the project can be generated if optional inputs will be skipped, false - otherwise.
     */
    public boolean canBeGenerated() {
        return state().canBeGenerated();
    }

    /**
     * Returns result.
     *
     * @return Flow.Result
     */
    public Optional<Flow.Result> result() {
        return state.result();
    }

    /**
     * Returns unresolved inputs.
     *
     * @return List of the unresolved inputs
     */
    public List<UserInputAST> unresolvedInputs() {
        return interpreter.unresolvedInputs();
    }

    /**
     * Returns Map that contains path to the context node and corresponding context node.
     *
     * @return Map
     */
    public Map<String, ContextNodeAST> pathToContextNodeMap() {
        return interpreter.pathToContextNodeMap();
    }

    Interpreter interpreter() {
        return interpreter;
    }

    /**
     * Returns current state of the Flow.
     *
     * @return FlowState
     */
    public FlowState state() {
        return state;
    }

    void state(FlowState state) {
        this.state = state;
    }

    /**
     * Returns archetype.
     *
     * @return archetype
     */
    public Archetype archetype() {
        return archetype;
    }

    /**
     * Returns {@code ArchetypeDescriptor} that represents the entry point script.
     *
     * @return ArchetypeDescriptor
     */
    public ArchetypeDescriptor entryPoint() {
        return entryPoint;
    }

    /**
     * Returns additional visitors used to process nodes in the archetype descriptors.
     *
     * @return list of the additional visitors
     */
    public List<Visitor<ASTNode>> additionalVisitors() {
        return additionalVisitors;
    }

    Flow(Archetype archetype,
         String startDescriptorPath,
         boolean skipOptional,
         List<Visitor<ASTNode>> additionalVisitors,
         Map<String, String> externalValues,
         Map<String, String> externalDefaults) {
        this.archetype = archetype;
        this.additionalVisitors = additionalVisitors;
        entryPoint = archetype.getDescriptor(startDescriptorPath);
        interpreter = new Interpreter(archetype, startDescriptorPath, skipOptional, additionalVisitors, externalValues,
                                      externalDefaults);
        this.skipOptional = skipOptional;
        state = new InitialFlowState(this);
    }

    Flow(Archetype archetype,
         String startDescriptorPath,
         boolean skipOptional,
         List<Visitor<ASTNode>> additionalVisitors,
         Map<String, String> externalDefaults,
         boolean eagerRun
    ) {
        this.archetype = archetype;
        this.additionalVisitors = additionalVisitors;
        entryPoint = archetype.getDescriptor(startDescriptorPath);
        interpreter = new Interpreter(archetype, startDescriptorPath, skipOptional, additionalVisitors, externalDefaults,
                eagerRun);
        this.skipOptional = skipOptional;
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
        private final List<Visitor<ASTNode>> additionalVisitors = new ArrayList<>();
        private boolean skipOptional = false;
        private Map<String, String> externalValues;
        private Map<String, String> externalDefaults;

        private Builder() {
        }

        /**
         * Sets the {@code skipOptional} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param skipOptional skipOptional
         * @return a reference to this Builder
         */
        public Builder skipOptional(boolean skipOptional) {
            this.skipOptional = skipOptional;
            return this;
        }

        /**
         * Sets the {@code externalValues} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param externalValues external values
         * @return a reference to this Builder
         */
        public Builder externalValues(Map<String, String> externalValues) {
            this.externalValues = externalValues;
            return this;
        }

        /**
         * Sets the {@code externalDefaults} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param externalDefaults external defaults
         * @return a reference to this Builder
         */
        public Builder externalDefaults(Map<String, String> externalDefaults) {
            this.externalDefaults = externalDefaults;
            return this;
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
         * Add an additional visitor and returns a reference to this Builder so that the methods can be chained
         * together.
         *
         * @param visitor the {@code visitor} to add
         * @return a reference to this Builder
         */
        public Builder addAdditionalVisitor(Visitor<ASTNode> visitor) {
            additionalVisitors.add(visitor);
            return this;
        }

        /**
         * Add a list of additional visitors and returns a reference to this Builder so that the methods can be chained
         * together.
         *
         * @param visitors the {@code visitors} to add
         * @return a reference to this Builder
         */
        public Builder addAdditionalVisitor(List<Visitor<ASTNode>> visitors) {
            additionalVisitors.addAll(visitors);
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
            if (externalValues == null) {
                externalValues = Map.of();
            }
            if (externalDefaults == null) {
                externalDefaults = Map.of();
            }
            return new Flow(archetype, startDescriptorPath, skipOptional, additionalVisitors, externalValues, externalDefaults);
        }
    }

    /**
     * Final result of the script interpreter work.
     */
    public static class Result {

        private final Map<String, ContextNodeAST> context = new HashMap<>();
        private final List<ASTNode> outputs = new ArrayList<>();
        private final Archetype archetype;

        /**
         * Create a new instance.
         *
         * @param archetype archetype instance that can be used to obtain files that mentioned in {@code outputs()}
         */
        public Result(Archetype archetype) {
            this.archetype = archetype;
        }

        /**
         * Get instance of the {@code Archetype} that can be used to obtain files that mentioned in {@code outputs()}.
         * Archetype must be closed after its using ({@code archetype.close()}).
         *
         * @return archetype
         */
        public Archetype archetype() {
            return archetype;
        }

        /**
         * Returns Map that contains path to the context node and corresponding context node.
         *
         * @return map
         */
        public Map<String, ContextNodeAST> context() {
            return context;
        }

        /**
         * Returns list of the {@code ContextNodeAST} nodes.
         *
         * @return list
         */
        public List<ASTNode> outputs() {
            return outputs;
        }
    }
}
