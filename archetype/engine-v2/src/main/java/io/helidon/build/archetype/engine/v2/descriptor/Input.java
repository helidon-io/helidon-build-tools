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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.LinkedList;
import java.util.Objects;

/**
 * Archetype Input.
 */
public class Input {

    private final LinkedList<InputNode> nodes = new LinkedList<>();
    private final LinkedList<Context> contexts = new LinkedList<>();
    private final LinkedList<Step> steps = new LinkedList<>();
    private final LinkedList<Input> inputs = new LinkedList<>();
    private final LinkedList<Source> sources = new LinkedList<>();
    private final LinkedList<Exec> execs = new LinkedList<>();
    private Output output;

    protected Input() {
    }

    /**
     * Get the Input nodes: {@link InputText}, {@link InputBoolean}, {@link InputEnum}, {@link InputList}.
     *
     * @return nodes
     */
    public LinkedList<InputNode> nodes() {
        return nodes;
    }

    /**
     * Get input contexts.
     *
     * @return contexts
     */
    public LinkedList<Context> contexts() {
        return contexts;
    }

    /**
     * Get input steps.
     *
     * @return steps
     */
    public LinkedList<Step> steps() {
        return steps;
    }

    /**
     * Get input inputs.
     *
     * @return inputs
     */
    public LinkedList<Input> inputs() {
        return inputs;
    }

    /**
     * Get input sources.
     *
     * @return sources
     */
    public LinkedList<Source> sources() {
        return sources;
    }

    /**
     * Get input execs.
     *
     * @return execs
     */
    public LinkedList<Exec> execs() {
        return execs;
    }

    /**
     * Get input output.
     *
     * @return output
     */
    public Output output() {
        return output;
    }

    /**
     * Set input output.
     *
     * @param  output output
     */
    public void output(Output output) {
        this.output = output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Input input = (Input) o;
        return nodes.equals(input.nodes)
                && contexts.equals(input.contexts)
                && steps.equals(input.steps)
                && inputs.equals(input.inputs)
                && sources.equals(input.sources)
                && execs.equals(input.execs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodes, contexts, steps, inputs, sources, execs, output);
    }

    @Override
    public String toString() {
        return "Input{"
                + "nodes=" + nodes()
                + ", contexts=" + contexts()
                + ", steps=" + steps()
                + ", inputs=" + inputs()
                + ", sources=" + sources()
                + ", execs=" + execs()
                + ", output=" + output()
                + '}';
    }
}
