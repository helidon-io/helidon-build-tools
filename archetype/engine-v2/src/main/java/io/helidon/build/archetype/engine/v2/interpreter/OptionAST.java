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

import io.helidon.build.archetype.engine.v2.descriptor.Option;

/**
 * Archetype option AST node used in {@link InputListAST} and {@link InputEnumAST}.
 */
public class OptionAST extends ASTNode implements HelpNode {

    private final String label;
    private final String value;
    private final StringBuilder help = new StringBuilder();

    OptionAST(String label, String value, String currentDirectory) {
        super(currentDirectory);
        this.label = label;
        this.value = value;
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
     * Get the value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    @Override
    public String help() {
        return help.toString();
    }

    @Override
    public void addHelp(String help) {
        this.help.append(help);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static OptionAST from(Option input, String currentDirectory) {
        OptionAST result = new OptionAST(input.label(), input.value(), currentDirectory);
        result.addHelp(input.help());
        result.children().addAll(transformList(input.contexts(), c -> ContextAST.from(c, currentDirectory)));
        result.children().addAll(transformList(input.steps(), s -> StepAST.from(s, currentDirectory)));
        result.children().addAll(transformList(input.inputs(), i -> InputAST.from(i, currentDirectory)));
        result.children().addAll(transformList(input.sources(), s -> SourceAST.from(s, currentDirectory)));
        result.children().addAll(transformList(input.execs(), ExecAST::from));
        result.children().add(OutputAST.from(input.output(), currentDirectory));
        return result;
    }
}
