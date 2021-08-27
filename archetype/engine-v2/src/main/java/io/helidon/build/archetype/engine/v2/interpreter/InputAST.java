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

import io.helidon.build.archetype.engine.v2.descriptor.Input;

/**
 * Archetype Input AST node.
 */
public class InputAST extends ASTNode {

    InputAST(String currentDirectory) {
        super(currentDirectory);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static InputAST from(Input input, String currentDirectory) {
        InputAST result = new InputAST(currentDirectory);

        result.children().addAll(transformList(input.nodes(), i -> InputNodeASTFactory.from(i, currentDirectory)));
        result.children().addAll(transformList(input.contexts(), c -> ContextAST.from(c, currentDirectory)));
        result.children().addAll(transformList(input.steps(), s -> StepAST.from(s, currentDirectory)));
        result.children().addAll(transformList(input.inputs(), i -> InputAST.from(i, currentDirectory)));
        result.children().addAll(transformList(input.sources(), s -> SourceAST.from(s, currentDirectory)));
        result.children().addAll(transformList(input.execs(), ExecAST::from));
        result.children().add(OutputAST.from(input.output(), currentDirectory));

        return result;
    }
}
