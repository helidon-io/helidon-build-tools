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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;

/**
 * Prompter that uses CLI for input/output.
 */
public class CLIPrompter extends DefaultPrompterImpl {

    private String lastStepLabel;

    /**
     * Create a new instance of the class.
     *
     * @param skipOptional true if the interpreter has to skip optional inputs, false - otherwise.
     */
    public CLIPrompter(boolean skipOptional) {
        super(skipOptional);
    }

    /**
     * Create a new instance of the class.
     */
    public CLIPrompter() {
        super(false);
    }

    @Override
    public String prompt(TextPrompt inputInfo) {
        try {
            String question = "Enter text";
            printTitle(inputInfo);

            String defaultValue = inputInfo.defaultValue() != null && !inputInfo.defaultValue().isEmpty()
                    ? BoldBlue.apply(inputInfo.defaultValue())
                    : null;
            String response = request(question, defaultValue);

            lastStepLabel = inputInfo.stepLabel();

            return response == null || response.length() == 0 ? inputInfo.defaultValue() : response.trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String prompt(EnumPrompt inputInfo) {
        try {
            String question = "Enter selection";
            printTitle(inputInfo);

            int defaultIndex = processOptions(inputInfo.options(), inputInfo.defaultValue());
            String defValue = defaultIndex != -1
                    ? BoldBlue.apply(String.format("%s", defaultIndex + 1))
                    : null;

            String response = request(question, defValue);

            lastStepLabel = inputInfo.stepLabel();

            if (response == null || response.trim().length() == 0) {
                if (defaultIndex != -1) {
                    return inputInfo.options().get(defaultIndex).value();
                }
                return prompt(inputInfo);
            }
            int option = Integer.parseInt(response.trim());
            if (option <= 0 || option > inputInfo.options().size()) {
                return prompt(inputInfo);
            }
            return inputInfo.options().get(option - 1).value();
        } catch (NumberFormatException e) {
            return prompt(inputInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> prompt(ListPrompt inputInfo) {
        try {
            String question = "Enter selection (one or more numbers separated by the spaces)";
            printTitle(inputInfo);

            List<Integer> defaultIndexes = new ArrayList<>();
            int defaultIndex = processOptions(inputInfo.options(), inputInfo.defaultValue());
            if (defaultIndex == -1) {
                defaultIndexes.addAll(processListDefaultValue(inputInfo));
            } else {
                defaultIndexes.add(defaultIndex);
            }
            String defValue = defaultIndexes.size() > 0
                    ? BoldBlue.apply(String.format("%s",
                    defaultIndexes.stream().map(i -> (i + 1) + "").collect(Collectors.joining(", "))))
                    : null;

            String response = request(question, defValue);

            lastStepLabel = inputInfo.stepLabel();

            if (response == null || response.trim().length() == 0) {
                if (!defaultIndexes.isEmpty()) {
                    return getListDefaultValues(inputInfo);
                }
                return prompt(inputInfo);
            }
            List<Integer> optionsIndexes = parseUserListInput(response.trim());
            return getOptionsValues(optionsIndexes, inputInfo.options());
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return prompt(inputInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Integer> processListDefaultValue(ListPrompt inputInfo) {
        if (inputInfo.defaultValue() == null) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<>();
        List<String> values = getListDefaultValues(inputInfo);
        for (int x = 0; x < inputInfo.options().size(); x++) {
            if (values.contains(inputInfo.options().get(x).value())) {
                result.add(x);
            }
        }
        return result;
    }

    private List<String> getListDefaultValues(ListPrompt inputInfo) {
        return Stream.of(inputInfo.defaultValue().split(",")).map(String::trim).collect(Collectors.toList());
    }

    @Override
    public boolean prompt(BooleanPrompt inputInfo) {
        try {
            if (inputInfo.stepLabel() != null && !inputInfo.stepLabel().equals(lastStepLabel)) {
                System.out.println(Bold.apply(inputInfo.stepLabel()));
            }

            String defaultValue = inputInfo.defaultValue() != null && !inputInfo.defaultValue().trim().isEmpty()
                    ? BoldBlue.apply(String.format("%s", inputInfo.defaultValue().trim().toLowerCase()))
                    : null;
            String question = String.format("%s (yes/no)", getQuestion(inputInfo));
            String response = request(question, defaultValue);

            lastStepLabel = inputInfo.stepLabel();

            if (response == null || response.trim().length() == 0) {
                if (inputInfo.defaultValue() != null && !inputInfo.defaultValue().trim().isEmpty()) {
                    return inputInfo.defaultValue().trim().equalsIgnoreCase("yes");
                } else {
                    return prompt(inputInfo);
                }
            }
            response = response.trim().toLowerCase();
            return response.equals("yes") || response.equals("no")
                    ? response.equals("yes") : prompt(inputInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printTitle(Prompt inputInfo) {
        if (inputInfo.stepLabel() != null && !inputInfo.stepLabel().equals(lastStepLabel)) {
            System.out.println(Bold.apply(inputInfo.stepLabel()));
        }
        System.out.println(getQuestion(inputInfo));
    }

    private String getQuestion(Prompt inputInfo) {
        return Bold.apply(Optional.ofNullable(inputInfo.prompt()).orElse(inputInfo.label()));
    }

    private int processOptions(List<Option> options, String defaultValue) {
        int defaultIndex = -1;

        for (int i = 0; i < options.size(); i++) {
            String o = BoldBlue.apply(String.format("  (%d) %s ", i + 1, options.get(i).value()));
            String option = options.get(i).label() != null && !options.get(i).label().isBlank()
                    ? String.format("%s | %s", o, options.get(i).label())
                    : o;
            if (Objects.equals(options.get(i).value(), defaultValue)) {
                defaultIndex = i;
            }
            System.out.println(option);
        }

        return defaultIndex;
    }

    private String request(String question, String defaultValue) throws IOException {
        String request = defaultValue != null
                ? String.format("%s (Default: %s): ", question, defaultValue)
                : String.format("%s: ", question);
        System.out.print(Bold.apply(request));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        return reader.readLine();
    }

    private List<String> getOptionsValues(List<Integer> indexes, List<Option> options) {
        Set<String> result = new LinkedHashSet<>();
        for (Integer index : indexes) {
            Objects.checkIndex(index - 1, options.size());
            result.add(options.get(index - 1).value());
        }
        return new ArrayList<>(result);
    }

    private List<Integer> parseUserListInput(String userInput) {
        String[] tokens = userInput.split("\\s+");
        List<Integer> result = new ArrayList<>();
        for (int x = 0; x < tokens.length; x++) {
            int option = Integer.parseInt(tokens[x]);
            result.add(option);
        }
        return result;
    }
}
