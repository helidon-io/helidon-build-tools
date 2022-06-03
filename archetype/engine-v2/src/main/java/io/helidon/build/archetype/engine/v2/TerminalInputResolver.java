/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.archetype.engine.v2.ast.Input.Enum.optionIndex;
import static io.helidon.build.common.Strings.padding;
import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldRed;
import static java.util.Collections.emptyList;

// TODO use prompt attribute when available
//      restrict so that it is not usable in option

/**
 * Prompter that uses CLI for input/output.
 */
public class TerminalInputResolver extends InputResolver {
    private static final String ENTER_SELECTION = Bold.apply("Enter selection");
    private static final String ENTER_LIST_SELECTION = ENTER_SELECTION + " (numbers separated by spaces or 'none')";

    private final InputStream in;
    private String lastName;

    /**
     * Create an interactive input resolver.
     */
    public TerminalInputResolver() {
        this.in = System.in;
    }

    /**
     * Create a new interactive input resolver.
     *
     * @param in input stream to use for reading user input
     */
    public TerminalInputResolver(InputStream in) {
        this.in = Objects.requireNonNull(in, "input stream is null");
    }

    @Override
    protected void onVisitStep(Step step, Context context) {
        System.out.printf("%n| %s%n%n", step.name());
    }

    @Override
    public VisitResult visitBoolean(Input.Boolean input, Context context) {
        ContextScope scope = context.scope();
        ContextScope nextScope = scope.getOrCreate(input.id(), input.isGlobal());
        VisitResult result = onVisitInput(input, nextScope, context);
        while (result == null) {
            try {
                Value defaultValue = defaultValue(input, context);
                String defaultText = defaultValue != null ? BoldBlue.apply(Input.Boolean.asString(defaultValue)) : null;
                String question = String.format("%s (yes/no)", Bold.apply(input.name()));
                String response = prompt(question, defaultText);
                if (response == null || response.trim().length() == 0) {
                    scope.put(nextScope.id(), defaultValue, ContextValue.ValueKind.DEFAULT);
                    if (defaultValue == null || !defaultValue.asBoolean()) {
                        result = VisitResult.SKIP_SUBTREE;
                    } else {
                        result = VisitResult.CONTINUE;
                    }
                    break;
                }
                boolean value;
                try {
                    value = Input.Boolean.valueOf(response, true);
                } catch (Exception e) {
                    System.out.println(BoldRed.apply("Invalid response: " + response));
                    if (LogLevel.isDebug()) {
                        Log.debug(e.getMessage());
                    }
                    continue;
                }
                scope.put(nextScope.id(), Value.create(value), ContextValue.ValueKind.USER);
                if (!value) {
                    result = VisitResult.SKIP_SUBTREE;
                } else {
                    result = VisitResult.CONTINUE;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        context.pushScope(nextScope);
        return result;
    }

    @Override
    public VisitResult visitText(Input.Text input, Context context) {
        ContextScope scope = context.scope();
        ContextScope nextScope = scope.getOrCreate(input.id(), input.isGlobal());
        VisitResult result = onVisitInput(input, nextScope, context);
        if (result == null) {
            try {
                String defaultValue = defaultValue(input, context).asString();
                String defaultText = defaultValue != null ? BoldBlue.apply(defaultValue) : null;
                String response = prompt(Bold.apply(input.name()), defaultText);
                ContextValue.ValueKind valueKind;
                Value value;
                if (response == null || response.trim().length() == 0) {
                    value = Value.create(defaultValue);
                    valueKind = ContextValue.ValueKind.DEFAULT;
                } else {
                    value = Value.create(response);
                    valueKind = ContextValue.ValueKind.USER;
                }
                scope.put(nextScope.id(), value, valueKind);
                result = VisitResult.CONTINUE;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        context.pushScope(nextScope);
        return result;
    }

    @Override
    public VisitResult visitEnum(Input.Enum input, Context context) {
        ContextScope scope = context.scope();
        ContextScope nextScope = scope.getOrCreate(input.id(), input.isGlobal());
        VisitResult result = onVisitInput(input, nextScope, context);
        while (result == null) {
            String response = null;
            try {
                Value defaultValue = defaultValue(input, context);
                List<Input.Option> options = input.options(context::filterNode);
                int defaultIndex = optionIndex(defaultValue.asString(), options);
                // skip prompting if there is only one option with a default value
                if (options.size() == 1 && defaultIndex >= 0) {
                    scope.put(nextScope.id(), defaultValue, ContextValue.ValueKind.DEFAULT);
                    result = VisitResult.CONTINUE;
                    break;
                }

                printName(input);
                printOptions(options);

                String defaultText = defaultIndex != -1
                        ? BoldBlue.apply(String.format("%s", defaultIndex + 1))
                        : null;

                response = prompt(ENTER_SELECTION, defaultText);
                lastName = input.name();
                Value value;
                ContextValue.ValueKind valueKind;
                if ((response == null || response.trim().length() == 0)) {
                    if (defaultIndex < 0) {
                        continue;
                    }
                    value = defaultValue;
                    valueKind = ContextValue.ValueKind.DEFAULT;
                } else {
                    value = Value.create(parseEnumResponse(response, options));
                    valueKind = ContextValue.ValueKind.USER;
                }
                scope.put(nextScope.id(), value, valueKind);
                result = VisitResult.CONTINUE;
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println(BoldRed.apply("Invalid response: " + response));
                if (LogLevel.isDebug()) {
                    Log.debug(e.getMessage());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        context.pushScope(nextScope);
        return result;
    }

    @Override
    public VisitResult visitList(Input.List input, Context context) {
        ContextScope scope = context.scope();
        ContextScope nextScope = scope.getOrCreate(input.id(), input.isGlobal());
        VisitResult result = onVisitInput(input, nextScope, context);
        while (result == null) {
            String response = null;
            try {
                List<Input.Option> options = input.options(context::filterNode);

                printName(input);
                printOptions(options);

                Value defaultValue = defaultValue(input, context);
                String defaultText = BoldBlue.apply(defaultResponse(defaultValue.asList(), options));

                response = prompt(ENTER_LIST_SELECTION, defaultText);
                lastName = input.name();
                Value value;
                ContextValue.ValueKind valueKind;
                if (response == null || response.trim().length() == 0) {
                    value = defaultValue;
                    valueKind = ContextValue.ValueKind.DEFAULT;
                } else {
                    value = Value.create(parseListResponse(response, options));
                    valueKind = ContextValue.ValueKind.USER;
                }
                scope.put(nextScope.id(), value, valueKind);
                result = VisitResult.CONTINUE;
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println(BoldRed.apply("Invalid response: " + response));
                if (LogLevel.isDebug()) {
                    Log.debug(e.getMessage());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        context.pushScope(nextScope);
        return result;
    }

    private void printName(Input input) {
        String name = input.name();
        if (name != null && !name.equals(lastName)) {
            System.out.println(Bold.apply(name));
        }
    }

    private static void printOptions(List<Input.Option> options) {
        int index = 0;
        int maxKeyWidth = maxKeyWidth(optionValues(options));
        for (Input.Option option : options) {
            System.out.println(optionText(option, maxKeyWidth, index));
            index++;
        }
    }

    private String prompt(String prompt, String defaultText) throws IOException {
        String promptText = defaultText != null
                ? String.format("%s (default: %s): ", prompt, defaultText)
                : String.format("%s: ", prompt);
        System.out.print(promptText);
        System.out.flush();
        return new BufferedReader(new InputStreamReader(in)).readLine();
    }

    private static String optionText(Input.Option option, int maxKeyWidth, int index) {
        String o = BoldBlue.apply(String.format("  (%d) %s ", index + 1, option.value()));
        String name = option.name();
        if (name != null && !name.isBlank()) {
            return String.format("%s%s| %s", o, padding(" ", maxKeyWidth, option.value()), name);
        }
        return o;
    }

    private static int maxKeyWidth(List<String> names) {
        int maxLen = 0;
        for (String name : names) {
            int len = name.length();
            if (len > maxLen) {
                maxLen = len;
            }
        }
        return maxLen;
    }

    private static List<String> optionValues(List<Input.Option> options) {
        return options.stream()
                      .map(Input.Option::value)
                      .collect(Collectors.toList());
    }

    private static String parseEnumResponse(String response, List<Input.Option> options) {
        int index = Integer.parseInt(response.trim());
        return options.get(index - 1).value().toLowerCase();
    }

    private static List<String> parseListResponse(String response, List<Input.Option> options) {
        response = response.trim();
        if ("none".equals(response)) {
            return emptyList();
        }
        return Arrays.stream(response.trim().split("\\s+"))
                     .map(Integer::parseInt)
                     .distinct()
                     .map(i -> options.get(i - 1))
                     .map(Input.Option::value)
                     .map(String::toLowerCase)
                     .collect(Collectors.toList());
    }

    private static String defaultResponse(List<String> optionNames, List<Input.Option> options) {
        if (optionNames == null || optionNames.isEmpty()) {
            return "none";
        }
        return IntStream.range(0, options.size())
                        .boxed()
                        .filter(i -> containsIgnoreCase(optionNames, options.get(i).value()))
                        .map(i -> (i + 1) + "")
                        .collect(Collectors.joining(", "));
    }

    private static boolean containsIgnoreCase(Collection<String> values, String expected) {
        for (String optionName : values) {
            if (optionName.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }
}
