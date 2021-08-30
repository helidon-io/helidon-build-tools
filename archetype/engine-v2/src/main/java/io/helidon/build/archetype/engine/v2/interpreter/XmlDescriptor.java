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

package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedHashMap;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;

/**
 * Archetype descriptor.
 */
class XmlDescriptor extends ASTNode implements HelpNode {

    private final Map<String, String> archetypeAttributes = new LinkedHashMap<>();
    private final StringBuilder help = new StringBuilder();

    XmlDescriptor(String currentDirectory) {
        super(currentDirectory);
    }

    Map<String, String> archetypeAttributes() {
        return archetypeAttributes;
    }

    @Override
    public String help() {
        return help.toString();
    }

    @Override
    public void addHelp(String help) {
        this.help.append(help);
    }

    static XmlDescriptor from(ArchetypeDescriptor descriptor, String currentDirectory) {
        XmlDescriptor result = new XmlDescriptor(currentDirectory);
        descriptor.archetypeAttributes().forEach((key, value) -> result.archetypeAttributes().putIfAbsent(key, value));
        result.addHelp(descriptor.help());
        result.children().addAll(transformList(descriptor.contexts(), c -> ContextAST.from(c, currentDirectory)));
        result.children().addAll(transformList(descriptor.steps(), s -> StepAST.from(s, currentDirectory)));
        result.children().addAll(transformList(descriptor.inputs(), i -> InputAST.from(i, currentDirectory)));
        result.children().addAll(transformList(descriptor.sources(), s -> SourceAST.from(s, currentDirectory)));
        result.children().addAll(transformList(descriptor.execs(), ExecAST::from));
        result.children().add(OutputAST.from(descriptor.output(), currentDirectory));
        return result;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }
}
