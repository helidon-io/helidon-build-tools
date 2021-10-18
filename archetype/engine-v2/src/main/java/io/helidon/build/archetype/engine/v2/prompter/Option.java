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

package io.helidon.build.archetype.engine.v2.prompter;

/**
 * Option that used to store value in {@link EnumPrompt} and {@link ListPrompt}.
 */
public class Option {

    private final String label;
    private final String value;
    private final String help;

    /**
     * Option constructor.
     *
     * @param label label
     * @param value value
     * @param help  help
     */
    public Option(String label, String value, String help) {
        this.label = label;
        this.value = value;
        this.help = help;
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
     * Get the help.
     *
     * @return help
     */
    public String help() {
        return help;
    }
}
