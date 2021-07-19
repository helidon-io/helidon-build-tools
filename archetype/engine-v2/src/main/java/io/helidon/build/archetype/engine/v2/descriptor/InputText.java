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

/**
 * Archetype text in {@link Input} nodes.
 */
public class InputText extends InputNode {

    private final String placeHolder;
    private String help;

    InputText(String label,
              String name,
              String def,
              String prompt,
              boolean optional,
              String placeHolder) {
        super(label, name, def, prompt, optional);
        this.placeHolder = placeHolder;
    }

    /**
     * Get the placeholder.
     *
     * @return placeholder
     */
    public String placeHolder() {
        return placeHolder;
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
     * @param help help content
     */
    public void help(String help) {
        this.help = help;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InputText text = (InputText) o;
        return placeHolder.equals(text.placeHolder)
                && help.equals(text.help);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), placeHolder, help);
    }

    @Override
    public String toString() {
        return "Text{"
                + "placeholder=" + placeHolder()
                + ", help=" + help()
                + ", label=" + label()
                + ", name=" + name()
                + ", default=" + def()
                + ", prompt=" + prompt()
                + ", optional=" + isOptional()
                + '}';
    }
}
