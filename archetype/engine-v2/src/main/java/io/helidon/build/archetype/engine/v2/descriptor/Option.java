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
 * Archetype option used in {@link InputList} and {@link InputEnum}.
 */
public class Option {

    private final String label;
    private final String value;
    private String help;
    private final LinkedList<Context> contexts = new LinkedList<>();
    private final LinkedList<Step> steps = new LinkedList<>();
    private final LinkedList<Input> inputs = new LinkedList<>();
    private final LinkedList<Source> sources = new LinkedList<>();
    private final LinkedList<Exec> execs = new LinkedList<>();
    private Output output;

    Option(String label, String value) {
        this.label = label;
        this.value = value;
    }

    /**
     * Get the label.
     *
     * @return label
     */
    public String label() {
        return label;
    }

    /**
     * Get the value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Get the help element content.
     *
     * @return help
     */
    public String help() {
        return help;
    }

    /**
     * Set the help element content.
     *
     * @param help content
     */
    public void help(String help) {
        this.help = help;
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
        Option option = (Option) o;
        return label.equals(option.label)
                && value.equals(option.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), label, value, help, contexts, steps, inputs, sources, execs, output);
    }

    @Override
    public String toString() {
        return "Option{"
                + "label=" + label()
                + ", value=" + value()
                + ", help=" + help()
                + ", contexts=" + contexts()
                + ", steps=" + steps()
                + ", inputs=" + inputs()
                + ", sources=" + sources()
                + ", execs=" + execs()
                + ", output=" + output()
                + '}';
    }
}
