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

package io.helidon.build.cli.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static io.helidon.build.util.StyleFunction.Bold;
import static io.helidon.build.util.StyleFunction.BoldBlue;

/**
 * Class Prompter.
 */
class Prompter {
    private static final Predicate<String> NO_VALIDATION = input -> true;

    static String prompt(String prompt, String defaultAnswer, Predicate<String> validator) {
        while (true) {
            try {
                String styledDefaultAnswer = BoldBlue.apply(String.format("%s", defaultAnswer));
                String styledPrompt = Bold.apply(prompt);
                String fullPrompt = String.format("%s (default: %s): ", styledPrompt, styledDefaultAnswer);
                System.out.print(fullPrompt);
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String response = reader.readLine();
                if (response == null || response.trim().isEmpty()) {
                    return defaultAnswer;
                } else if (validator.test(response)) {
                    return response;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static String prompt(String question, Optional<String> defaultResponse) {
        return prompt(question, defaultResponse.orElse(null));
    }

    static String prompt(String question, String defaultResponse) {
        return prompt(question, defaultResponse, NO_VALIDATION);
    }

    static int prompt(String question, List<String> options, int defaultOption) {
        return prompt(question, options.toArray(new String[]{}), defaultOption);
    }

    static int prompt(String question, String[] options, int defaultOption) {
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

    static boolean promptYesNo(String question, boolean defaultOption) {
        String defaultResponse = defaultOption ? "y" : "n";
        String response = prompt(question, defaultResponse,
                                 input -> input.equalsIgnoreCase("y") || input.equalsIgnoreCase("n"));
        return response.equalsIgnoreCase("y");
    }


    static void displayLine(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        String r = prompt("Helidon version", "2.0.0-SNAPSHOT");
        System.out.println("Response is '" + r + "'");

        String[] flavorOptions = new String[]{"SE", "MP"};
        int flavorIndex = prompt("Helidon flavor", flavorOptions, 0);
        System.out.println("Response is '" + flavorOptions[flavorIndex] + "'");
    }

    private Prompter() {
    }
}
