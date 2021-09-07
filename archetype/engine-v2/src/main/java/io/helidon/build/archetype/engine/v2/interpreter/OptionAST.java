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
public class OptionAST extends ASTNode {

    private final String label;
    private final String value;
    private String help;

    OptionAST(String label, String value, ASTNode parent, String currentDirectory) {
        super(parent, currentDirectory);
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

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static OptionAST create(Option inputFrom, ASTNode parent, String currentDirectory) {
        OptionAST result = new OptionAST(inputFrom.label(), inputFrom.value(), parent, currentDirectory);
        result.help(inputFrom.help());
        result.children().addAll(transformList(inputFrom.contexts(), c -> ContextAST.create(c, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.steps(), s -> StepAST.create(s, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.inputs(), i -> InputAST.create(i, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.sources(), s -> SourceAST.create(s, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.execs(), e -> ExecAST.create(e, result, currentDirectory)));
        if (inputFrom.output() != null) {
            result.children().add(OutputAST.create(inputFrom.output(), result, currentDirectory));
        }
        return result;
    }
}
