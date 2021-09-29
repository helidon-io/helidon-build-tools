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
import io.helidon.build.archetype.engine.v2.interpreter.InputEnumAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputListAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputTextAST;
import io.helidon.build.archetype.engine.v2.interpreter.UserInputAST;

/**
 * Factory methods to create {@link Prompt} instances.
 */
public abstract class PromptFactory {

    /**
     * Creates {@link Prompt} instance depending on the type of the input {@code UserInputAST} parameter.
     *
     * @param userInputAST   userInputAST
     * @param canBeGenerated mark that indicates whether project can be generated if optional inputs will be skipped
     * @return {@link Prompt} instance
     */
    public static Prompt<?> create(UserInputAST userInputAST, boolean canBeGenerated) {
        if (userInputAST.children().isEmpty()) {
            throw new IllegalArgumentException("UserInputAST must contain a child note");
        }
        if (userInputAST.children().get(0) instanceof InputBooleanAST) {
            return BooleanPrompt.builder()
                    .userInputAST((UserInputAST) userInputAST.children().get(0))
                    .canBeGenerated(canBeGenerated)
                    .build();
        }
        if (userInputAST.children().get(0) instanceof InputTextAST) {
            return TextPrompt.builder()
                    .userInputAST((UserInputAST) userInputAST.children().get(0))
                    .canBeGenerated(canBeGenerated)
                    .build();
        }
        if (userInputAST.children().get(0) instanceof InputEnumAST) {
            return EnumPrompt.builder()
                    .userInputAST((UserInputAST) userInputAST.children().get(0))
                    .canBeGenerated(canBeGenerated)
                    .build();
        }
        if (userInputAST.children().get(0) instanceof InputListAST) {
            return ListPrompt.builder()
                    .userInputAST((UserInputAST) userInputAST.children().get(0))
                    .canBeGenerated(canBeGenerated)
                    .build();
        }
        throw new IllegalArgumentException("Unexpected type of a child note in UserInputAST object");
    }
}
