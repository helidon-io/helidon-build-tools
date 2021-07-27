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
 * Archetype enum in {@link Input}.
 */
public class InputEnum extends InputNode {

    private String help;
    private final LinkedList<Option> options = new LinkedList<>();
    private final LinkedList<Context> contexts = new LinkedList<>();
    private final LinkedList<Step> steps = new LinkedList<>();
    private final LinkedList<Input> inputs = new LinkedList<>();
    private final LinkedList<Source> sources = new LinkedList<>();
    private final LinkedList<Exec> execs = new LinkedList<>();
    private Output output;

    InputEnum(String label,
              String name,
              String def,
              String prompt,
              boolean optional) {
        super(label, name, def, prompt, optional);
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
     * Set the Output.
     *
     * @param output element
     */
    public void output(Output output) {
        this.output = output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InputEnum inputList = (InputEnum) o;
        return help.equals(inputList.help)
                && options == inputList.options
                && contexts == inputList.contexts
                && steps == inputList.steps
                && inputs == inputList.inputs
                && sources == inputList.sources
                && execs == inputList.execs
                && output.equals(inputList.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), help, options, contexts, steps, inputs, sources, execs, output);
    }

    @Override
    public String toString() {
        return "InputEnum{"
                + "help=" + help()
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
