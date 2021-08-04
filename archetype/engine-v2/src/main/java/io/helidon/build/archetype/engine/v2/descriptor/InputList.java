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
 * Archetype list in {@link Input} archetype.
 */
public class InputList extends InputNode {

    private final String min;
    private final String max;
    private String help;
    private final LinkedList<Option> options = new LinkedList<>();
    private final LinkedList<Context> contexts = new LinkedList<>();
    private final LinkedList<Step> steps = new LinkedList<>();
    private final LinkedList<Input> inputs = new LinkedList<>();
    private final LinkedList<Source> sources = new LinkedList<>();
    private final LinkedList<Exec> execs = new LinkedList<>();
    private Output output;

    InputList(String label,
              String name,
              String def,
              String prompt,
              boolean optional,
              String min,
              String max,
              String help) {
        super(label, name, def, prompt, optional);
        this.min = min;
        this.max = max;
        this.help = help;
    }

    /**
     * Get the minimum.
     *
     * @return minimum
     */
    public String min() {
        return min;
    }

    /**
     * Get the maximum.
     *
     * @return maximum
     */
    public String max() {
        return max;
    }

    /**
     * Get the help content.
     *
     * @return help content
     */
    public String help() {
        return help;
    }

    /**
     * Set the help content.
     *
     * @param help help
     */
    public void help(String help) {
        this.help = help;
    }

    /**
     * Get the options.
     *
     * @return options
     */
    public LinkedList<Option> options() {
        return options;
    }

    /**
     * Get the contexts.
     *
     * @return contexts
     */
    public LinkedList<Context> contexts() {
        return contexts;
    }

    /**
     * Get the steps.
     *
     * @return steps
     */
    public LinkedList<Step> steps() {
        return steps;
    }

    /**
     * Get the inputs.
     *
     * @return inputs
     */
    public LinkedList<Input> inputs() {
        return inputs;
    }

    /**
     * Get the sources.
     *
     * @return sources
     */
    public LinkedList<Source> sources() {
        return sources;
    }

    /**
     * Get the execs.
     *
     * @return execs
     */
    public LinkedList<Exec> execs() {
        return execs;
    }

    /**
     * Get the output.
     *
     * @return output
     */
    public Output output() {
        return output;
    }

    /**
     * Set the output.
     *
     * @param output output
     */
    public void output(Output output) {
        this.output = output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InputList inputList = (InputList) o;
        return min == inputList.min
                && max == inputList.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), min, max, help, options, contexts, steps, inputs, sources, execs, output);
    }

    @Override
    public String toString() {
        return "InputList{"
                + "min=" + min()
                + ", max=" + max()
                + ", help=" + help()
                + ", options=" + options()
                + ", contexts=" + contexts()
                + ", steps=" + steps()
                + ", inputs=" + inputs()
                + ", sources=" + sources()
                + ", execs=" + execs()
                + ", output=" + output()
                + ", label=" + label()
                + ", name=" + name()
                + ", default=" + def()
                + ", prompt=" + prompt()
                + ", optional=" + isOptional()
                + '}';
    }
}
