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

import io.helidon.build.archetype.engine.v2.interpreter.InputBooleanAST;
import io.helidon.build.archetype.engine.v2.interpreter.UserInputAST;

/**
 * Prompt of the boolean value.
 */
public class BooleanPrompt extends Prompt {

    private BooleanPrompt(
            String stepLabel,
            String stepHelp,
            String help,
            String label,
            String name,
            String def,
            String prompt,
            boolean optional
    ) {
        super(stepLabel, stepHelp, help, label, name, def, prompt, optional);
    }

    /**
     * Create a new builder.
     *
     * @return a new builder
     */
    public static BooleanPrompt.Builder builder() {
        return new BooleanPrompt.Builder();
    }

    public static class Builder extends Prompt.Builder<BooleanPrompt, BooleanPrompt.Builder> {

        @Override
        public BooleanPrompt.Builder instance() {
            return this;
        }

        @Override
        public BooleanPrompt.Builder userInputAST(UserInputAST userInputAST) {
            if (userInputAST.children().isEmpty()) {
                throw new IllegalArgumentException("UserInputAST must contain a child note");
            }
            if (userInputAST.children().get(0) instanceof InputBooleanAST) {
                initFields(userInputAST);
                return this;
            }
            throw new IllegalArgumentException(
                    String.format(
                            "Incorrect type of the child node in the UserInputAST instance. Must be - %s. Actual - %s.",
                            InputBooleanAST.class.getName(),
                            userInputAST.children().get(0).getClass().getName()
                    )
            );
        }

        @Override
        public BooleanPrompt build() {
            return new BooleanPrompt(
                    stepLabel(),
                    stepHelp(),
                    help(),
                    label(),
                    name(),
                    defaultValue(),
                    prompt(),
                    optional()
            );
        }
    }
}
