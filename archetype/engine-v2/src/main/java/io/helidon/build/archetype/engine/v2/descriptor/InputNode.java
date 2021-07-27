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

import java.util.Objects;

public abstract class InputNode {

    private final String label;
    private final String name;
    private final String def;
    private final String prompt;
    private boolean optional = false;

    InputNode(String label, String name, String def, String prompt, boolean optional) {
        this.label = label;
        this.name = name;
        this.def = def;
        this.prompt = prompt;
        this.optional = optional;
    }

    public String label() {
        return label;
    }

    public String name() {
        return name;
    }

    public String def() {
        return def;
    }

    public String prompt() {
        return prompt;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InputNode inputNode = (InputNode) o;
        return label.equals(inputNode.label)
                && name.equals(inputNode.name)
                && def.equals(inputNode.def)
                && prompt.equals(inputNode.prompt)
                && optional == inputNode.optional;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), label, name, def, prompt, optional);
    }

}
