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

import io.helidon.build.archetype.engine.v2.interpreter.ContextEnumAST;
import io.helidon.build.archetype.engine.v2.interpreter.ContextNodeAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputEnumAST;
import io.helidon.build.archetype.engine.v2.interpreter.OptionAST;
import io.helidon.build.archetype.engine.v2.interpreter.UserInputAST;

/**
 * Prompt of the one value from the enum.
 */
public class EnumPrompt extends Prompt<String> {

    private final List<Option> options = new ArrayList<>();

    private EnumPrompt(
            String stepLabel,
            String stepHelp,
            String help,
            String label,
            String name,
            String def,
            String prompt,
            List<Option> options,
            boolean optional,
            boolean canBeGenerated
    ) {
        super(stepLabel, stepHelp, help, label, name, def, prompt, optional, canBeGenerated);
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
     * Create a new builder.
     *
     * @return a new builder
     */
    public static EnumPrompt.Builder builder() {
        return new EnumPrompt.Builder();
    }

    @Override
    public String accept(Prompter prompter) {
        return prompter.prompt(this);
    }

    @Override
    public ContextNodeAST acceptAndConvert(Prompter prompter, String path) {
        String value = prompter.prompt(this);
        ContextEnumAST result = new ContextEnumAST(path);
        result.value(value);
        return result;
    }

    public static class Builder extends Prompt.Builder<EnumPrompt, EnumPrompt.Builder> {

        private List<Option> options = new ArrayList<>();

        /**
         * Set the options.
         *
         * @param options list of options
         * @return Builder
         */
        public EnumPrompt.Builder options(List<Option> options) {
            if (options != null) {
                this.options = options;
            }
            return this;
        }

        @Override
        public EnumPrompt.Builder instance() {
            return this;
        }

        @Override
        public EnumPrompt.Builder userInputAST(UserInputAST userInputAST) {
            if (userInputAST.children().isEmpty()) {
                throw new IllegalArgumentException("UserInputAST must contain a child note");
            }
            if (userInputAST.children().get(0) instanceof EnumPrompt) {
                InputEnumAST inputEnumAST = (InputEnumAST) userInputAST.children().get(0);

                initFields(userInputAST);
                options.addAll(
                        inputEnumAST.children().stream()
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
                            EnumPrompt.class.getName(),
                            userInputAST.children().get(0).getClass().getName()
                    )
            );
        }

        @Override
        public EnumPrompt build() {
            return new EnumPrompt(
                    stepLabel(),
                    stepHelp(),
                    help(),
                    label(),
                    name(),
                    defaultValue(),
                    prompt(),
                    options,
                    optional(),
                    canBeGenerated()
            );
        }
    }
}
