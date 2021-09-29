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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.interpreter.ContextListAST;
import io.helidon.build.archetype.engine.v2.interpreter.ContextNodeAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputListAST;
import io.helidon.build.archetype.engine.v2.interpreter.OptionAST;
import io.helidon.build.archetype.engine.v2.interpreter.UserInputAST;

/**
 * Prompt of the one or more values from the list.
 */
public class ListPrompt extends Prompt<List<String>> {

    private final String min;
    private final String max;
    private final List<Option> options = new ArrayList<>();

    private ListPrompt(
            String stepLabel,
            String stepHelp,
            String help,
            String label,
            String name,
            String def,
            String prompt,
            String min,
            String max,
            List<Option> options,
            boolean optional,
            boolean canBeGenerated
    ) {
        super(stepLabel, stepHelp, help, label, name, def, prompt, optional, canBeGenerated);
        this.max = max;
        this.min = min;
        if (options != null) {
            this.options.addAll(options);
        }
    }

    /**
     * Get the options.
     *
     * @return options
     */
    public List<Option> options() {
        return options;
    }

    /**
     * Get the minimum.
     *
     * @return minimum
     */
    public String min() {
        return min;
    }

    /**
     * Get the maximum.
     *
     * @return maximum
     */
    public String max() {
        return max;
    }

    /**
     * Create a new builder.
     *
     * @return a new builder
     */
    public static ListPrompt.Builder builder() {
        return new ListPrompt.Builder();
    }

    @Override
    public List<String> accept(Prompter prompter) {
        return prompter.prompt(this);
    }

    @Override
    public ContextNodeAST acceptAndConvert(Prompter prompter, String path) {
        List<String> values = prompter.prompt(this);
        ContextListAST result = new ContextListAST(path);
        result.values().addAll(values);
        return result;
    }

    public static class Builder extends Prompt.Builder<ListPrompt, ListPrompt.Builder> {

        private String min;
        private String max;
        private List<Option> options = new ArrayList<>();

        /**
         * Set the options.
         *
         * @param options list of options
         * @return Builder
         */
        public ListPrompt.Builder options(List<Option> options) {
            if (options != null) {
                this.options = options;
            }
            return this;
        }

        /**
         * Set the minimum.
         *
         * @param min min
         * @return Builder
         */
        public ListPrompt.Builder min(String min) {
            this.min = min;
            return this;
        }

        /**
         * Set the maximum.
         *
         * @param max max
         * @return Builder
         */
        public ListPrompt.Builder max(String max) {
            this.max = max;
            return this;
        }

        @Override
        public ListPrompt.Builder instance() {
            return this;
        }

        @Override
        public ListPrompt.Builder userInputAST(UserInputAST userInputAST) {
            if (userInputAST.children().isEmpty()) {
                throw new IllegalArgumentException("UserInputAST must contain a child note");
            }
            if (userInputAST.children().get(0) instanceof InputListAST) {
                InputListAST inputListAST = (InputListAST) userInputAST.children().get(0);

                initFields(userInputAST);
                min = inputListAST.min();
                max = inputListAST.max();
                options.addAll(
                        inputListAST.children().stream()
                                .filter(ch -> ch instanceof OptionAST)
                                .map(ch -> (OptionAST) ch)
                                .map(o -> new Option(o.label(), o.value(), o.help()))
                                .collect(Collectors.toList())
                );

                return this;
            }
            throw new IllegalArgumentException(
                    String.format(
                            "Incorrect type of the child node in the UserInputAST instance. Must be - %s. Actual - %s.",
                            ListPrompt.class.getName(),
                            userInputAST.children().get(0).getClass().getName()
                    )
            );
        }

        @Override
        public ListPrompt build() {
            return new ListPrompt(
                    stepLabel(),
                    stepHelp(),
                    help(),
                    label(),
                    name(),
                    defaultValue(),
                    prompt(),
                    min,
                    max,
                    options,
                    optional(),
                    canBeGenerated()
            );
        }
    }
}
