/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.helidon.build.common.ansi.StyleFunction.Bold;
import static io.helidon.build.common.ansi.StyleFunction.BoldBlue;

/**
 * Class Prompter.
 */
public class Prompter {

    private Prompter() {
    }

    /**
     * Prompt a text value.
     *
     * @param question        question printed on the prompt
     * @param defaultResponse optional of default response
     * @return user input
     */
    public static String prompt(String question, Optional<String> defaultResponse) {
        return prompt(question, defaultResponse.orElse(null));
    }

    /**
     * Prompt a text value.
     *
     * @param question        question printed on the prompt
     * @param defaultResponse default response, may be {@code null}
     * @return user input
     */
    public static String prompt(String question, String defaultResponse) {
        try {
            String def = BoldBlue.apply(defaultResponse);
            String q = defaultResponse != null
                    ? String.format("%s (Default: %s): ", question, def)
                    : String.format("%s: ", question);
            System.out.print(Bold.apply(q));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String response = reader.readLine();
            return response == null || response.length() == 0 ? defaultResponse : response.trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prompt for a selection.
     *
     * @param question      question printed on the prompt
     * @param options       the set of options that can be selected
     * @param defaultOption the index of the default option
     * @return user input
     */
    public static int prompt(String question, List<String> options, int defaultOption) {
        return prompt(question, options.toArray(new String[]{}), defaultOption);
    }

    /**
     * Prompt for a selection.
     *
     * @param question      question printed on the prompt
     * @param options       the set of options that can be selected
     * @param defaultOption the index of the default option
     * @return user input
     */
    public static int prompt(String question, String[] options, int defaultOption) {
        Objects.checkIndex(defaultOption, options.length);
        try {
            System.out.println(Bold.apply(question));
            for (int i = 0; i < options.length; i++) {
                String o = BoldBlue.apply(String.format("  (%d) %s ", i + 1, options[i]));
                System.out.println(o);
            }
            String def = BoldBlue.apply(String.format("%d", defaultOption + 1));
            String q = String.format("Enter selection (Default: %s): ", def);
            System.out.print(Bold.apply(q));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String response = reader.readLine();
            if (response == null || response.trim().length() == 0) {
                return defaultOption;
            }
            int option = Integer.parseInt(response.trim());
            if (option <= 0 || option > options.length) {
                return prompt(question, options, defaultOption);
            }
            return option - 1;
        } catch (NumberFormatException e) {
            return prompt(question, options, defaultOption);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prompt for a yes no.
     *
     * @param question      question printed on the prompt
     * @param defaultOption the default value, {@code true} for "yes"
     * @return user input
     */
    public static boolean promptYesNo(String question, boolean defaultOption) {
        try {
            String def = BoldBlue.apply(defaultOption ? "y" : "n");
            String q = String.format("%s (Default: %s): ", question, def);
            System.out.print(Bold.apply(q));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String response = reader.readLine();
            if (response == null || response.trim().length() == 0) {
                return defaultOption;
            }
            response = response.trim().toLowerCase();
            return response.equals("y") || response.equals("n")
                    ? response.equals("y") : promptYesNo(question, defaultOption);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Display a line on the prompt.
     *
     * @param message message to display
     */
    public static void displayLine(String message) {
        System.out.println(message);
    }
}
