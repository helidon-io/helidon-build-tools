/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class PrompterTest {

    @Test
    void testPromptWithDefaultAnswer() {
        String input = "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        String result = Prompter.prompt("Enter your name", "defaultName");
        assertEquals("defaultName", result);
    }

    @Test
    void testPromptWithValidator() {
        String input = "validInput\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Predicate<String> validator = s -> s.equals("validInput");
        String result = Prompter.prompt("Enter valid input", null, validator);
        assertEquals("validInput", result);
    }

    @Test
    void testPromptWithOptionalDefault() {
        String input = "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        String result = Prompter.prompt("Enter your name", Optional.of("defaultName"));
        assertEquals("defaultName", result);
    }

    @Test
    void testPromptSelection() {
        String input = "2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        List<String> options = List.of("Option 1", "Option 2", "Option 3");
        int result = Prompter.prompt("Choose an option", options, 0);
        assertEquals(1, result);
    }

    @Test
    void testPromptYesNo() {
        String input = "y\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        boolean result = Prompter.promptYesNo("Do you agree?", false);
        assertTrue(result);
    }

}