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

import io.helidon.build.archetype.engine.v2.interpreter.ContextNodeAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputNodeAST;
import io.helidon.build.archetype.engine.v2.interpreter.UserInputAST;

/**
 * Prompt.
 *
 * @param <T> type of the prompt.
 */
public abstract class Prompt<T> {

    private final String stepLabel;
    private final String stepHelp;
    private final String help;
    private final String label;
    private final String name;
    private final String def;
    private final String prompt;
    private final boolean optional;
    private final boolean canBeGenerated;

    Prompt(
            String stepLabel,
            String stepHelp,
            String help,
            String label,
            String name,
            String def,
            String prompt,
            boolean optional,
            boolean canBeGenerated
    ) {
        this.stepLabel = stepLabel;
        this.stepHelp = stepHelp;
        this.help = help;
        this.label = label;
        this.name = name;
        this.def = def;
        this.prompt = prompt;
        this.optional = optional;
        this.canBeGenerated = canBeGenerated;
    }

    /**
     * Add additional operation using {@code Prompter} instance.
     *
     * @param prompter prompter
     * @return value that has type {@code T}.
     */
    public abstract T accept(Prompter prompter);

    /**
     * Add additional operation using {@code Prompter} instance and create a new instance of the {@code ContextNodeAST}.
     *
     * @param prompter prompter
     * @param path     path that used to identify new ContextNodeAST instance.
     * @return a new instance of the {@code ContextNodeAST}
     */
    public abstract ContextNodeAST acceptAndConvert(Prompter prompter, String path);

    /**
     * Get the step label.
     *
     * @return label
     */
    public String stepLabel() {
        return stepLabel;
    }

    /**
     * Get the step help.
     *
     * @return help
     */
    public String stepHelp() {
        return stepHelp;
    }

    /**
     * Get help.
     *
     * @return help
     */
    public String help() {
        return help;
    }

    /**
     * Get the label.
     *
     * @return label
     */
    public String label() {
        return label;
    }

    /**
     * Get the name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Get the default value.
     *
     * @return default value
     */
    public String defaultValue() {
        return def;
    }

    /**
     * Get the prompt.
     *
     * @return prompt
     */
    public String prompt() {
        return prompt;
    }

    /**
     * Get the optional attribute.
     *
     * @return boolean
     */
    public boolean optional() {
        return optional;
    }

    /**
     * Get the canBeGenerated attribute.
     *
     * @return boolean
     */
    public boolean canBeGenerated() {
        return canBeGenerated;
    }

    /**
     * {@code Prompt} builder static inner class.
     *
     * @param <B> type of the {@code Builder} that derives from this class
     * @param <T> type of result in {@code build} method
     */
    public abstract static class Builder<T, B> {

        private String stepHelp;
        private String help;
        private String stepLabel;
        private String label;
        private String name;
        private String def;
        private String prompt;
        private boolean optional;
        private boolean canBeGenerated;

        /**
         * Sets the {@code stepHelp} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param stepHelp stepHelp
         * @return reference to the Builder
         */
        public B stepHelp(String stepHelp) {
            this.stepHelp = stepHelp;
            return instance();
        }

        /**
         * Sets the {@code help} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param help help
         * @return reference to the Builder
         */
        public B help(String help) {
            this.help = help;
            return instance();
        }

        /**
         * Sets the {@code stepLabel} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param stepLabel step label
         * @return reference to the Builder
         */
        public B stepLabel(String stepLabel) {
            this.stepLabel = stepLabel;
            return instance();
        }

        /**
         * Sets the {@code label} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param label label
         * @return reference to the Builder
         */
        public B label(String label) {
            this.label = label;
            return instance();
        }

        /**
         * Sets the {@code name} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param name name
         * @return reference to the Builder
         */
        public B name(String name) {
            this.name = name;
            return instance();
        }

        /**
         * Sets the {@code defaultValue} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param def default value
         * @return reference to the Builder
         */
        public B defaultValue(String def) {
            this.def = def;
            return instance();
        }

        /**
         * Sets the {@code prompt} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param prompt prompt
         * @return reference to the Builder
         */
        public B prompt(String prompt) {
            this.prompt = prompt;
            return instance();
        }

        /**
         * Sets the {@code optional} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param optional optional
         * @return reference to the Builder
         */
        public B optional(boolean optional) {
            this.optional = optional;
            return instance();
        }

        /**
         * Sets the {@code canBeGenerated} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param canBeGenerated canBeGenerated
         * @return reference to the Builder
         */
        public B canBeGenerated(boolean canBeGenerated) {
            this.canBeGenerated = canBeGenerated;
            return instance();
        }

        String stepHelp() {
            return stepHelp;
        }

        String help() {
            return help;
        }

        String stepLabel() {
            return stepLabel;
        }

        String label() {
            return label;
        }

        String name() {
            return name;
        }

        String defaultValue() {
            return def;
        }

        String prompt() {
            return prompt;
        }

        boolean optional() {
            return optional;
        }

        boolean canBeGenerated() {
            return canBeGenerated;
        }

        /**
         * Process the {@code userInputAST} and returns a reference to the Builder so that the methods can be chained together.
         *
         * @param userInputAST userInputAST
         * @return reference to the Builder
         */
        public abstract B userInputAST(UserInputAST userInputAST);

        /**
         * Returns the reference to the actual builder.
         *
         * @return reference to the Builder
         */
        public abstract B instance();

        /**
         * Returns a {@code T} built from the parameters previously set.
         *
         * @return a {@code T} built with parameters of this {@code Prompt.Builder}
         */
        public abstract T build();

        void initFields(UserInputAST userInputAST) {
            stepHelp(userInputAST.help());
            stepLabel(userInputAST.help());

            InputNodeAST inputNodeAST = (InputNodeAST) userInputAST.children().get(0);

            help(inputNodeAST.help());
            label(inputNodeAST.label());
            name(inputNodeAST.name());
            defaultValue(inputNodeAST.defaultValue());
            prompt(inputNodeAST.prompt());
            optional(inputNodeAST.isOptional());
        }
    }
}
