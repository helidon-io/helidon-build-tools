/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;

/**
 * Class Prompter.
 */
public class Prompter {
    private static final Predicate<String> NO_VALIDATION = input -> true;

    /**
     * Prompt a text value.
     *
     * @param question question printed on the prompt
     * @param defaultAnswer default response, may be {@code null}
     * @param validator test for response validity
     * @return user input or defaultAnswer if none
     */
    public static String prompt(String question, String defaultAnswer, Predicate<String> validator) {
        while (true) {
            try {
                String fullPrompt;
                String styledPrompt = Bold.apply(question);
                if (defaultAnswer == null) {
                    fullPrompt = String.format("%s: ", styledPrompt);
                } else {
                    String styledDefaultAnswer = BoldBlue.apply(String.format("%s", defaultAnswer));
                    fullPrompt = String.format("%s (default: %s): ", styledPrompt, styledDefaultAnswer);
                }
                System.out.print(fullPrompt);
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String response = reader.readLine();
                if (response == null || response.trim().isEmpty()) {
                    if (defaultAnswer != null) {
                        return defaultAnswer;
                    }
                } else if (validator.test(response)) {
                    return response;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Prompt a text value.
     *
     * @param question question printed on the prompt
     * @param defaultResponse optional of default response
     * @return user input
     */
    public static String prompt(String question, Optional<String> defaultResponse) {
        return prompt(question, defaultResponse.orElse(null));
    }

    /**
     * Prompt a text value.
     *
     * @param question question printed on the prompt
     * @param defaultResponse default response, may be {@code null}
     * @return user input
     */
    public static String prompt(String question, String defaultResponse) {
        return prompt(question, defaultResponse, NO_VALIDATION);
    }

    /**
     * Prompt for a selection.
     *
     * @param question question printed on the prompt
     * @param options the set of options that can be selected
     * @param defaultOption the index of the default option
     * @return user input
     */
    public static int prompt(String question, List<String> options, int defaultOption) {
        return prompt(question, options.toArray(new String[]{}), defaultOption);
    }

    /**
     * Prompt for a selection.
     *
     * @param question question printed on the prompt
     * @param options the set of options that can be selected
     * @param defaultOption the index of the default option
     * @return user input
     */
    public static int prompt(String question, String[] options, int defaultOption) {
        Objects.checkIndex(defaultOption, options.length);
        System.out.println(Bold.apply(question));
        for (int i = 0; i < options.length; i++) {
            String o = BoldBlue.apply(String.format("  (%d) %s ", i + 1, options[i]));
            System.out.println(o);
        }
        String defaultResponse = Integer.toString(defaultOption + 1);
        String response = prompt("Enter selection", defaultResponse, input -> {
            try {
                int option = Integer.parseInt(input);
                return option > 0 && option <= options.length;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        return Integer.parseInt(response) - 1;
    }

    /**
     * Prompt for a yes no.
     *
     * @param question question printed on the prompt
     * @param defaultOption the default value, {@code true} for "yes"
     * @return user input
     */
    public static boolean promptYesNo(String question, boolean defaultOption) {
        String defaultResponse = defaultOption ? "y" : "n";
        String response = prompt(question, defaultResponse,
                                 input -> input.equalsIgnoreCase("y") || input.equalsIgnoreCase("n"));
        return response.equalsIgnoreCase("y");
    }

    /**
     * Display a line on the prompt.
     *
     * @param message message to display
     */
    public static void displayLine(String message) {
        System.out.println(message);
    }

    private Prompter() {
    }
}
