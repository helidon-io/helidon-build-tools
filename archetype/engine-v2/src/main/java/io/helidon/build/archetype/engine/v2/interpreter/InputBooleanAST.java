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

import io.helidon.build.archetype.engine.v2.descriptor.InputBoolean;

/**
 * Boolean AST node in {@link InputAST}.
 */
public class InputBooleanAST extends InputNodeAST {

    InputBooleanAST(String label, String name, String def, String prompt, String currentDirectory) {
        super(label, name, def, prompt, currentDirectory);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static InputBooleanAST from(InputBoolean input, String currentDirectory) {
        InputBooleanAST result =
                new InputBooleanAST(input.label(), input.name(), input.def(), input.prompt(), currentDirectory);
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
