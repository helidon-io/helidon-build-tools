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

import io.helidon.build.archetype.engine.v2.descriptor.InputList;

/**
 * Archetype AST list node in {@link InputAST} archetype.
 */
public class InputListAST extends InputNodeAST {

    private final String min;
    private final String max;

    InputListAST(String label, String name, String def, String prompt, String min, String max, ASTNode parent,
                 String currentDirectory) {
        super(label, name, def, prompt, parent, currentDirectory);
        this.min = min;
        this.max = max;
    }

    /**
     * Get the minimum.
     *
     * @return minimum
     */
    public String min() {
        return min;
    }

    /**
     * Get the maximum.
     *
     * @return maximum
     */
    public String max() {
        return max;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static InputListAST from(InputList input, ASTNode parent, String currentDirectory) {
        InputListAST result =
                new InputListAST(input.label(), input.name(), input.def(), input.prompt(),
                        input.min(), input.max(),
                        parent, currentDirectory);
        result.children().addAll(transformList(input.contexts(), c -> ContextAST.create(c, result, currentDirectory)));
        result.help(input.help());
        result.children().addAll(transformList(input.steps(), s -> StepAST.create(s, result, currentDirectory)));
        result.children().addAll(transformList(input.inputs(), i -> InputAST.create(i, result, currentDirectory)));
        result.children().addAll(transformList(input.sources(), s -> SourceAST.create(s, result, currentDirectory)));
        result.children().addAll(transformList(input.execs(), e -> ExecAST.create(e, result, currentDirectory)));
        if (input.output() != null) {
            result.children().add(OutputAST.create(input.output(), result, currentDirectory));
        }
        result.children().addAll(transformList(input.options(), o -> OptionAST.create(o, result, currentDirectory)));
        return result;
    }
}
