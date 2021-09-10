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

    InputAST(ASTNode parent, Location location) {
        super(parent, location);
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static InputAST create(Input inputFrom, ASTNode parent, Location location) {
        InputAST result = new InputAST(parent, location);

        result.children().addAll(transformList(inputFrom.contexts(), c -> ContextAST.create(c, result, location)));
        result.children().addAll(transformList(inputFrom.nodes(), i -> InputNodeASTFactory.create(i, result, location)));
        result.children().addAll(transformList(inputFrom.steps(), s -> StepAST.create(s, result, location)));
        result.children().addAll(transformList(inputFrom.inputs(), i -> InputAST.create(i, result, location)));
        result.children().addAll(transformList(inputFrom.sources(), s -> SourceAST.create(s, result, location)));
        result.children().addAll(transformList(inputFrom.execs(), e -> ExecAST.create(e, result, location)));
        if (inputFrom.output() != null) {
            result.children().add(OutputAST.create(inputFrom.output(), result, location));
        }

        return result;
    }
}
