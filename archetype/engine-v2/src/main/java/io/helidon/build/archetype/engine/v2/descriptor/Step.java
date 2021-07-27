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

public class Step extends Conditional {

    private final String label;
    private String help;

    private final LinkedList<Context> contexts = new LinkedList<>();
    private final LinkedList<Exec> execs = new LinkedList<>();
    private final LinkedList<Source> sources = new LinkedList<>();
    private final LinkedList<Input> inputs = new LinkedList<>();

    protected Step(String label, String ifProperty) {
        super(ifProperty);
        this.label = label;
    }

    public String label() {
        return label;
    }

    public String help() {
        return help;
    }

    public LinkedList<Context> contexts() {
        return contexts;
    }

    public LinkedList<Exec> execs() {
        return execs;
    }

    public LinkedList<Source> sources() {
        return sources;
    }

    public LinkedList<Input> inputs() {
        return inputs;
    }

    public void help(String help) {
        this.help = help;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Step step = (Step) o;
        return label.equals(step.label)
                && help.equals(step.help)
                && contexts.equals(step.contexts)
                && execs.equals(step.execs)
                && sources.equals(step.sources)
                && inputs.equals(step.inputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), label, help, contexts, execs, sources, inputs);
    }

    @Override
    public String toString() {
        return "Step{"
                + "label=" + label()
                + ", help=" + help()
                + ", contexts=" + contexts()
                + ", execs=" + execs()
                + ", sources=" + sources()
                + ", inputs=" + inputs()
                + '}';
    }
}
