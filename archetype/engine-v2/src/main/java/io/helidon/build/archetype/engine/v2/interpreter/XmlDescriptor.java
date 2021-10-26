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
class XmlDescriptor extends ASTNode {

    private final Map<String, String> archetypeAttributes = new LinkedHashMap<>();
    private String help;

    XmlDescriptor(Location location) {
        super(null, location);
    }

    Map<String, String> archetypeAttributes() {
        return archetypeAttributes;
    }

    /**
     * Get the help.
     *
     * @return help
     */
    public String help() {
        return help;
    }

    /**
     * Set the help content.
     *
     * @param help help content
     */
    public void help(String help) {
        this.help = help;
    }

    static XmlDescriptor create(ArchetypeDescriptor descriptor, ASTNode parent, Location location) {
        XmlDescriptor result = new XmlDescriptor(location);
        result.parent(parent);
        descriptor.archetypeAttributes().forEach((key, value) -> result.archetypeAttributes().putIfAbsent(key, value));
        result.children().addAll(transformList(descriptor.contexts(), c -> ContextAST.create(c, parent, location)));
        result.help(descriptor.help());
        // TODO: shouldn't all of the create calls below use result instead of parent?
        result.children().addAll(transformList(descriptor.steps(), s -> StepAST.create(s, parent, location)));
        result.children().addAll(transformList(descriptor.inputs(), i -> InputAST.create(i, parent, location)));
        result.children().addAll(transformList(descriptor.sources(), s -> SourceAST.create(s, parent, location)));
        result.children().addAll(transformList(descriptor.execs(), e -> ExecAST.create(e, parent, location)));
        if (descriptor.output() != null) {
            result.children().add(
                    ConditionalNode.mapConditional(
                            descriptor.output(),
                            OutputAST.create(descriptor.output(), result, location),
                            result,
                            location
                    ));
        }
        return result;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
