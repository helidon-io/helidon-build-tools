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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.common.PropertyEvaluator;

import static io.helidon.build.archetype.engine.v1.Prompter.prompt;

/**
 * Class FlowNodeControllers.
 */
public class FlowNodeControllers {

    private FlowNodeControllers() {
    }

    /**
     * Controller creator.
     *
     * @param flowNode The flow node.
     * @param properties Properties used to resolve and expressions.
     * @return A flow controller.
     */
    public static FlowNodeController create(ArchetypeDescriptor.FlowNode flowNode, Map<String, String> properties) {
        if (flowNode instanceof ArchetypeDescriptor.Input) {
            return new InputController((ArchetypeDescriptor.Input) flowNode, properties);
        }
        if (flowNode instanceof ArchetypeDescriptor.Select) {
            return new SelectController((ArchetypeDescriptor.Select) flowNode, properties);
        }
        throw new UnsupportedOperationException("No support for " + flowNode);
    }

    /**
     * Base class for all controllers.
     */
    public abstract static class FlowNodeController {
        private final Map<String, String> properties;

        FlowNodeController(Map<String, String> properties) {
            this.properties = properties;
        }

        Map<String, String> properties() {
            return properties;
        }

        /**
         * Execute the controller.
         */
        public abstract void execute();
    }

    /**
     * Controller for {@code ArchetypeDescriptor.Input}.
     */
    static class InputController extends FlowNodeController {
        private final ArchetypeDescriptor.Input input;

        InputController(ArchetypeDescriptor.Input input, Map<String, String> properties) {
            super(properties);
            this.input = input;
        }

        /**
         * Executes the controller. Evaluates default value if an expression.
         */
        @Override
        public void execute() {
            String property = input.property().id();
            String defaultValue = input.defaultValue()
                    .map(v -> hasExpression(v) ? PropertyEvaluator.evaluate(v, properties()) : v)
                    .orElse(null);
            String v = prompt(input.text(), defaultValue);
            properties().put(property, v);
        }
    }

    /**
     * Controller for {@code ArchetypeDescriptor.Select}.
     */
    static class SelectController extends FlowNodeController {
        private final ArchetypeDescriptor.Select select;

        SelectController(ArchetypeDescriptor.Select select, Map<String, String> properties) {
            super(properties);
            this.select = select;
        }

        /**
         * Executes the controller. Assumes that first choice is always default.
         * Sets the property associated with selection to {@code "true"}.
         */
        @Override
        public void execute() {
            LinkedList<ArchetypeDescriptor.Choice> choices = select.choices();
            List<String> options = choices.stream()
                    .map(ArchetypeDescriptor.Choice::text)
                    .collect(Collectors.toList());
            int index = prompt(select.text(), options, 0);
            String v = options.get(index);
            choices.stream()
                    .filter(c -> c.text().equals(v))
                    .map(ArchetypeDescriptor.Choice::property)
                    .findFirst()
                    .ifPresent(p -> System.setProperty(p.id(), "true"));
        }
    }

    /**
     * Simple check for values that contain expressions.
     *
     * @param value The value.
     * @return Returns {@code true} if value contains an expression, false otherwise.
     */
    private static boolean hasExpression(String value) {
        return value.contains("${") && value.contains("}");
    }
}
