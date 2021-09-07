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

import io.helidon.build.archetype.engine.v2.descriptor.InputEnum;

/**
 * Archetype AST enum node in {@link InputAST}.
 */
public class InputEnumAST extends InputNodeAST {

    InputEnumAST(String label, String name, String def, String prompt, ASTNode parent, String currentDirectory) {
        super(label, name, def, prompt, parent, currentDirectory);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static InputEnumAST create(InputEnum inputFrom, ASTNode parent, String currentDirectory) {
        InputEnumAST result =
                new InputEnumAST(inputFrom.label(), inputFrom.name(), inputFrom.def(), inputFrom.prompt(), parent,
                        currentDirectory);
        result.help(inputFrom.help());
        result.children().addAll(transformList(inputFrom.contexts(), c -> ContextAST.create(c, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.steps(), s -> StepAST.create(s, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.inputs(), i -> InputAST.create(i, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.sources(), s -> SourceAST.create(s, result, currentDirectory)));
        result.children().addAll(transformList(inputFrom.execs(), e -> ExecAST.create(e, result, currentDirectory)));
        if (inputFrom.output() != null) {
            result.children().add(OutputAST.create(inputFrom.output(), result, currentDirectory));
        }
        result.children().addAll(transformList(inputFrom.options(), o -> OptionAST.create(o, result, currentDirectory)));
        return result;
    }
}
