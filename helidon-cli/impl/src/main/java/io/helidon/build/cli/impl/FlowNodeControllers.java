/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeDescriptor;

import static io.helidon.build.cli.impl.Prompter.prompt;

/**
 * Class FlowNodeControllers.
 */
class FlowNodeControllers {

    private FlowNodeControllers() {
    }

    static FlowNodeController create(ArchetypeDescriptor.FlowNode flowNode) {
        if (flowNode instanceof ArchetypeDescriptor.Input) {
            return new InputController((ArchetypeDescriptor.Input) flowNode);
        }
        if (flowNode instanceof ArchetypeDescriptor.Select) {
            return new SelectController((ArchetypeDescriptor.Select) flowNode);
        }
        throw new UnsupportedOperationException("No support for " + flowNode);
    }

    static abstract class FlowNodeController {
        abstract void execute();
    }

    static class InputController extends FlowNodeController {
        private final ArchetypeDescriptor.Input input;

        InputController(ArchetypeDescriptor.Input input) {
            this.input = input;
        }

        @Override
        void execute() {
            String v = prompt(input.text(), input.defaultValue());
            System.setProperty(input.property().id(), v);
        }
    }

    static class SelectController extends FlowNodeController {
        private final ArchetypeDescriptor.Select select;

        SelectController(ArchetypeDescriptor.Select select) {
            this.select = select;
        }

        @Override
        void execute() {
            LinkedList<ArchetypeDescriptor.Choice> choices = select.choices();
            List<String> options = choices.stream()
                    .map(ArchetypeDescriptor.Choice::text)
                    .collect(Collectors.toList());
            String v = prompt(select.text(), options, 0);
            ArchetypeDescriptor.Property property = choices.stream()
                    .filter(c -> c.text().equals(v))
                    .map(ArchetypeDescriptor.Choice::property)
                    .findFirst().get();
            System.setProperty(property.id(), "true");
        }
    }
}
