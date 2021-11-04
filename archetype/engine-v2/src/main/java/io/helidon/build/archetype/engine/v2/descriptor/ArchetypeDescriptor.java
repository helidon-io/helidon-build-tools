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
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

/**
 * Helidon archetype V2 descriptor.
 */
public class ArchetypeDescriptor {

    private final Path archetypePath;
    private final Path descriptorPath;
    private final Map<String, String> archetypeAttributes;
    private final LinkedList<Context> contexts;
    private final LinkedList<Step> steps;
    private final LinkedList<Input> inputs;
    private final LinkedList<Source> sources;
    private final LinkedList<Exec> execs;
    private final Output output;
    private String help;

    ArchetypeDescriptor(Path archetypePath,
                        Path descriptorPath,
                        Map<String, String> archetypeAttributes,
                        LinkedList<Context> context,
                        LinkedList<Step> step,
                        LinkedList<Input> inputs,
                        LinkedList<Source> source,
                        LinkedList<Exec> exec,
                        Output output,
                        String help) {
        this.archetypePath = archetypePath;
        this.descriptorPath = descriptorPath;
        this.archetypeAttributes = archetypeAttributes;
        this.contexts = context;
        this.steps = step;
        this.inputs = inputs;
        this.sources = source;
        this.execs = exec;
        this.output = output;
        this.help = help;
    }

    /**
     * Create a archetype descriptor instance from an input stream.
     *
     *
     *
     * @param archetypePath path to the archetype.
     * @param descriptorPath path to descriptor.
     * @param is input stream
     * @return ArchetypeDescriptor
     */
    public static ArchetypeDescriptor read(Path archetypePath, Path descriptorPath, InputStream is) {
        return ArchetypeDescriptorReader.read(archetypePath, descriptorPath, is);
    }

    /**
     * Returns the path to the archetype containing this descriptor.
     * @return the path.
     */
    public Path archetypePath() {
        return archetypePath;
    }

    /**
     * Returns the path to this descriptor.
     * @return the path.
     */
    public Path descriptorPath() {
        return descriptorPath;
    }

    /**
     * Get the list of context archetype from main archetype-script xml element.
     *
     * @return List of Context
     */
    public LinkedList<Context> contexts() {
        return contexts;
    }

    /**
     * Get the list of step archetype from main archetype-script xml element.
     *
     * @return List of Step
     */
    public LinkedList<Step> steps() {
        return steps;
    }

    /**
     * Get a list of input archetype from main archetype-script xml element.
     *
     * @return List of Input
     */
    public LinkedList<Input> inputs() {
        return inputs;
    }

    /**
     * Get the list of source archetype from main archetype-script xml element.
     *
     * @return List of Source
     */
    public LinkedList<Source> sources() {
        return sources;
    }

    /**
     * Get the list of exec archetype from main archetype-script xml element.
     *
     * @return List of Exec
     */
    public LinkedList<Exec> execs() {
        return execs;
    }

    /**
     * Get the output archetype from main archetype-script xml element.
     *
     * @return Output
     */
    public Output output() {
        return output;
    }

    /**
     * Get the help archetype from main archetype-script xml element.
     *
     * @return help as a String
     */
    public String help() {
        return help;
    }

    /**
     * Get the main archetype-script xml element attributes.
     *
     * @return Map of attributes
     */
    public Map<String, String> archetypeAttributes() {
        return archetypeAttributes;
    }

    /**
     * Set the help archetype from main archetype-script xml element.
     *
     * @param help String contained into help element
     */
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
                && steps.equals(a.steps)
                && inputs.equals(a.inputs)
                && sources.equals(a.sources)
                && execs.equals(a.execs)
                && output.equals(a.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), archetypeAttributes, contexts, steps, inputs, sources, execs, output);
    }

    @Override
    public String toString() {
        return "ArchetypeDescriptor{"
                + "archetypeAttributes=" + archetypeAttributes()
                + ", contexts=" + contexts()
                + ", steps=" + steps()
                + ", inputs=" + inputs()
                + ", sources=" + sources()
                + ", execs=" + execs()
                + ", output=" + output()
                + ", help=" + help()
                + '}';
    }
}
