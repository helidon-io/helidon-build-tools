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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CLIPrompterTest {

    private Prompter prompter;
    private static final InputStream DEFAULT_STDIN = System.in;

    @BeforeEach
    public void setup() {
        prompter = new CLIPrompter();
    }

    @AfterEach
    public void clear() {
        System.setIn(DEFAULT_STDIN);
    }

    @Test
    public void testInputBooleanWithEmptyUserInputAndDefaultValue() {
        simulateUserInput("");
        String defaultValue = "yes";
        boolean response = prompter.prompt(getBooleanPrompt(false, defaultValue));

        assertThat(response, is(true));
    }

    @Test
    public void testInputBooleanWithNotEmptyUserInput() {
        simulateUserInput("NO");
        String defaultValue = "yes";
        boolean response = prompter.prompt(getBooleanPrompt(false, defaultValue));

        assertThat(response, is(false));
    }

    @Test
    public void testInputListWithEmptyUserInputAndDefaultValue() {
        simulateUserInput("");
        String defaultValue = "value 1";
        List<String> response = prompter.prompt(getListPrompt(false, defaultValue));

        assertThat(response.get(0), is(defaultValue));
    }

    @Test
    public void testInputListWithEmptyUserInputAndTwoDefaultValues() {
        simulateUserInput("");
        String defaultValue = "value 1, value 3";
        List<String> response = prompter.prompt(getListPrompt(false, defaultValue));

        assertThat(response, containsInAnyOrder("value 1", "value 3"));
        assertThat(response.size(), is(2));
    }

    @Test
    public void testInputListWithNotEmptyUserInput() {
        simulateUserInput("1 3 ");
        String defaultValue = null;
        List<String> response = prompter.prompt(getListPrompt(false, defaultValue));

        assertThat(response, containsInAnyOrder("value 1", "value 3"));
        assertThat(response.size(), is(2));
    }

    @Test
    public void testInputListWithNotEmptyUserInputWithRecurringValues() {
        simulateUserInput("1 3 3 1");
        String defaultValue = null;
        List<String> response = prompter.prompt(getListPrompt(false, defaultValue));

        assertThat(response, containsInAnyOrder("value 1", "value 3"));
        assertThat(response.size(), is(2));
    }

    @Test
    public void testInputEnumWithEmptyUserInputAndDefaultValue() {
        simulateUserInput("");
        String defaultValue = "value 1";
        String response = prompter.prompt(getEnumPrompt(false, defaultValue));

        assertThat(response, is(defaultValue));
    }

    @Test
    public void testInputEnumWithNotEmptyUserInput() {
        simulateUserInput("2");
        String defaultValue = "value 3";
        String expectedValue = "value 2";
        String response = prompter.prompt(getEnumPrompt(false, defaultValue));

        assertThat(response, is(expectedValue));
    }

    @Test
    public void testInputTextWithEmptyUserInputAndNullDefault() {
        simulateUserInput("");
        String response = prompter.prompt(getTextPrompt(false, null));

        assertThat(response, nullValue());
    }

    @Test
    public void testInputTextWithEmptyUserInput() {
        String defaultValue = "input text default";
        simulateUserInput("");
        String response = prompter.prompt(getTextPrompt(false, defaultValue));

        assertThat(response, is(defaultValue));
    }

    @Test
    public void testInputTextWithNotEmptyUserInput() {
        String defaultValue = "input text default";
        String userInput = "some text";
        simulateUserInput(userInput);
        String response = prompter.prompt(getTextPrompt(false, defaultValue));

        assertThat(response, is(userInput));
    }

    private void simulateUserInput(String userInput) {
        InputStream in = new ByteArrayInputStream(userInput.getBytes());
        System.setIn(in);
    }

    private EnumPrompt getEnumPrompt(boolean isOptional, String defaultValue) {
        return EnumPrompt.builder()
                .stepLabel("input enum step label")
                .stepHelp("input enum step label")
                .defaultValue(defaultValue)
                .help("input enum help")
                .prompt("input enum prompt")
                .label("input enum label")
                .name("input enum name")
                .optional(isOptional)
                .options(List.of(
                        new Option("label 1", "value 1", "help 1"),
                        new Option("", "value 2", "help 2"),
                        new Option(null, "value 3", "help 3"),
                        new Option("label 4", "value 4", "help 4")
                ))
                .build();
    }

    private ListPrompt getListPrompt(boolean isOptional, String defaultValue) {
        return ListPrompt.builder()
                .stepLabel("input list step label")
                .stepHelp("input list step label")
                .defaultValue(defaultValue)
                .help("input list help")
                .prompt("input list prompt")
                .label("input list label")
                .name("input list name")
                .optional(isOptional)
                .options(List.of(
                        new Option("label 1", "value 1", "help 1"),
                        new Option("", "value 2", "help 2"),
                        new Option(null, "value 3", "help 3"),
                        new Option("label 4", "value 4", "help 4")
                ))
                .build();
    }

    private TextPrompt getTextPrompt(boolean isOptional, String defaultValue) {
        return TextPrompt.builder()
                .stepLabel("input text step label")
                .stepHelp("input text step label")
                .defaultValue(defaultValue)
                .help("input text help")
                .prompt("input text prompt")
                .label("input text label")
                .name("input text name")
                .optional(isOptional)
                .placeHolder("input text placeholder")
                .build();
    }

    private BooleanPrompt getBooleanPrompt(boolean isOptional, String defaultValue) {
        return BooleanPrompt.builder()
                .stepLabel("input boolean step label")
                .stepHelp("input boolean step label")
                .defaultValue(defaultValue)
                .help("input boolean help")
                .prompt("input boolean prompt")
                .label("input boolean label")
                .name("input boolean name")
                .optional(isOptional)
                .build();
    }
}
