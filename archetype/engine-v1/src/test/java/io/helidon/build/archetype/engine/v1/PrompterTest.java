/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v1;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.is;

class PrompterTest {

    @Test
    void testPromptWithDefaultAnswer() {
        String result = prompter("\n").prompt("Enter your name", "defaultName");
        assertThat(result, is("defaultName"));
    }

    @Test
    void testPromptWithValidator() {
        Predicate<String> validator = s -> s.equals("validInput");
        String result = prompter("validInput\n").prompt("Enter valid input", null, validator);
        assertThat(result, is("validInput"));
    }

    @Test
    void testPromptSelection() {
        List<String> options = List.of("Option 1", "Option 2", "Option 3");
        int result = prompter("2\n").prompt("Choose an option", options, 0);
        assertThat(result, is(1));
    }

    @Test
    void testPromptYesNo() {
        boolean result = prompter("y\n").promptYesNo("Do you agree?", false);
        assertThat(result, is(true));
    }

    @Test
    void testPromptsShareReader() {
        Prompter prompter = prompter("first\nsecond\n");
        assertThat(prompter.prompt("First", null), is("first"));
        assertThat(prompter.prompt("Second", null), is("second"));
    }

    private static Prompter prompter(String input) {
        return new Prompter(new BufferedReader(new StringReader(input)));
    }
}
