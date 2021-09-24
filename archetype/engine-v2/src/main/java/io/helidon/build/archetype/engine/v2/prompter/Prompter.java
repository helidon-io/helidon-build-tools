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

import java.util.List;

/**
 * Prompter for user input.
 */
public interface Prompter {

    /**
     * Prompt a text value.
     *
     * @param inputInfo information about the current request.
     * @return user input (response from the user)
     */
    String prompt(TextPrompt inputInfo);

    /**
     * Prompt for a selection.
     *
     * @param inputInfo information about the current requests.
     * @return user input (the value of the chosen option)
     */
    String prompt(EnumPrompt inputInfo);

    /**
     * Prompt for a multiply selection.
     *
     * @param inputInfo information about the current requests.
     * @return user input (the values of the chosen options)
     */
    List<String> prompt(ListPrompt inputInfo);

    /**
     * Prompt for a yes no.
     *
     * @param inputInfo information about the current requests.
     * @return user input (true if user chose yes, no - otherwise)
     */
    boolean prompt(BooleanPrompt inputInfo);
}
