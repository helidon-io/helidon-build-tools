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

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

public class ArchetypeDescriptor {

    private final Map<String, String> archetypeAttributes;
    private final LinkedList<Context> contexts;
    private final LinkedList<Step> step;
    private final LinkedList<Input> inputs;
    private final LinkedList<Source> sources;
    private final LinkedList<Exec> execs;
    private final Output output;
    private String help;

    public ArchetypeDescriptor(Map<String, String> archetypeAttributes,
                               LinkedList<Context> context,
                               LinkedList<Step> step,
                               LinkedList<Input> inputs,
                               LinkedList<Source> source,
                               LinkedList<Exec> exec,
                               Output output,
                               String help) {
        this.archetypeAttributes = archetypeAttributes;
        this.contexts = context;
        this.step = step;
        this.inputs = inputs;
        this.sources = source;
        this.execs = exec;
        this.output = output;
        this.help = help;
    }

    /**
     * Create a archetype descriptor instance from an input stream.
     *
     * @param is input stream
     * @return ArchetypeDescriptor
     */
    public static ArchetypeDescriptor read(InputStream is) {
        return ArchetypeDescriptorReader.read(is);
    }

    public LinkedList<Context> context() {
        return contexts;
    }

    public LinkedList<Step> step() {
        return step;
    }

    public LinkedList<Input> inputs() {
        return inputs;
    }

    public LinkedList<Source> source() {
        return sources;
    }

    public LinkedList<Exec> exec() {
        return execs;
    }

    public Output output() {
        return output;
    }

    public String help() {
        return help;
    }

    public Map<String, String> archetypeAttributes() {
        return archetypeAttributes;
    }

    public void help(String help) {
        this.help = help;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArchetypeDescriptor a = (ArchetypeDescriptor) o;
        return contexts.equals(a.contexts)
                && step.equals(a.step)
                && inputs.equals(a.inputs)
                && sources.equals(a.sources)
                && execs.equals(a.execs)
                && output.equals(a.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contexts, step, inputs, sources, execs, output);
    }

    @Override
    public String toString() {
        return "ArchetypeDescriptor{"
                + "output=" + output()
                + ", help=" + help()
                + '}';
    }
}
