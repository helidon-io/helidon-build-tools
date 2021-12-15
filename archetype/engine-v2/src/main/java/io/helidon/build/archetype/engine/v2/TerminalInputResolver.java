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

package io.helidon.build.archetype.engine.v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Value;

import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldRed;

/**
 * Prompter that uses CLI for input/output.
 */
public class TerminalInputResolver extends InputResolver {

    private final InputStream in;
    private String lastLabel;

    /**
     * Create a new prompter.
     *
     * @param in input stream to use for reading user input
     */
    public TerminalInputResolver(InputStream in) {
        this.in = Objects.requireNonNull(in, "input stream is null");
    }

    @Override
    public VisitResult visitBoolean(Input.Boolean input, Context context) {
        VisitResult result = onVisitInput(input, context);
        if (result != null) {
            return result;
        }
        while (true) {
            try {
                printLabel(input);
                Value defaultValue = defaultValue(input, context);
                String defaultText = defaultValue != null ? BoldBlue.apply(defaultValue.asBoolean()) : null;
                String question = String.format("%s (yes/no)", Bold.apply(input.label()));
                String response = prompt(question, defaultText);
                if (response == null || response.trim().length() == 0) {
                    context.push(input.name(), defaultValue);
                    return VisitResult.CONTINUE;
                }
                boolean value;
                switch (response.trim().toLowerCase()) {
                    case "y":
                    case "yes":
                        value = true;
                        break;
                    case "n":
                    case "no":
                        value = false;
                        break;
                    default:
                        continue;
                }
                context.push(input.name(), Value.create(value));
                return VisitResult.CONTINUE;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public VisitResult visitText(Input.Text input, Context context) {
        VisitResult result = onVisitInput(input, context);
        if (result != null) {
            return result;
        }
        try {
            String defaultValue = defaultValue(input, context).asString();
            if (defaultValue != null) {
                defaultValue = context.substituteVariables(defaultValue);
            }
            String defaultText = defaultValue != null ? BoldBlue.apply(defaultValue) : null;
            String response = prompt(input.label(), defaultText);
            if (response == null || response.trim().length() == 0) {
                context.push(input.name(), Value.create(defaultValue));
            } else {
                context.push(input.name(), Value.create(response));
            }
            return VisitResult.CONTINUE;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public VisitResult visitEnum(Input.Enum input, Context context) {
        VisitResult result = onVisitInput(input, context);
        if (result != null) {
            return result;
        }
        while (true) {
            try {
                Value defaultValue = defaultValue(input, context);
                int defaultIndex = input.optionIndex(defaultValue.asString());

                // skip prompting if there is only one option with a default value
                if (input.options().size() == 1 && defaultIndex >= 0) {
                    context.push(input.name(), defaultValue);
                    return VisitResult.CONTINUE;
                }

                printLabel(input);
                printOptions(input);

                String defaultText = defaultIndex != -1
                        ? BoldBlue.apply(String.format("%s", defaultIndex + 1))
                        : null;

                String response = prompt("Enter selection", defaultText);
                lastLabel = input.label();
                if ((response == null || response.trim().length() == 0)) {
                    if (defaultIndex >= 0) {
                        context.push(input.name(), defaultValue);
                        return VisitResult.CONTINUE;
                    }
                } else {
                    int index = Integer.parseInt(response.trim());
                    if (index > 0 && index <= input.options().size()) {
                        context.push(input.name(), Value.create(input.options().get(index - 1).value()));
                        return VisitResult.CONTINUE;
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println(BoldRed.apply(e.getMessage()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public VisitResult visitList(Input.List input, Context context) {
        VisitResult result = onVisitInput(input, context);
        if (result != null) {
            return result;
        }
        while (true) {
            try {
                String question = "Enter selection (one or more numbers separated by the spaces)";
                printLabel(input);
                printOptions(input);

                Value defaultValue = defaultValue(input, context);
                List<Integer> defaultIndexes = input.optionIndexes(defaultValue.asList());
                String defaultText = defaultIndexes.size() > 0
                        ? BoldBlue.apply(String.format("%s",
                        defaultIndexes.stream().map(i -> (i + 1) + "").collect(Collectors.joining(", "))))
                        : null;

                String response = prompt(question, defaultText);

                lastLabel = input.label();
                if (response == null || response.trim().length() == 0) {
                    if (!defaultIndexes.isEmpty()) {
                        context.push(input.name(), defaultValue);
                        return VisitResult.CONTINUE;
                    }
                } else {
                    context.push(input.name(), Value.create(input.parseResponse(response)));
                    return VisitResult.CONTINUE;
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println(BoldRed.apply(e.getMessage()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void printLabel(Input input) {
        String label = input.label();
        if (label != null && !label.equals(lastLabel)) {
            System.out.println(Bold.apply(label));
        }
    }

    private static void printOptions(Input.Options input) {
        List<Input.Option> options = input.options();
        int index = 0;
        for (Input.Option option : options) {
            String o = BoldBlue.apply(String.format("  (%d) %s ", index + 1, option.value()));
            String optionText = option.label() != null && !option.label().isBlank()
                    ? String.format("%s | %s", o, option.label())
                    : o;
            System.out.println(optionText);
            index++;
        }
    }

    private String prompt(String prompt, String defaultText) throws IOException {
        String promptText = defaultText != null
                ? String.format("%s (default: %s): ", prompt, defaultText)
                : String.format("%s: ", prompt);
        System.out.print(Bold.apply(promptText));
        System.out.flush();
        return new BufferedReader(new InputStreamReader(in)).readLine();
    }
}
