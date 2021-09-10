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

import io.helidon.build.archetype.engine.v2.descriptor.InputText;

/**
 * Archetype AST text node in {@link InputAST} nodes.
 */
public class InputTextAST extends InputNodeAST {

    private final String placeHolder;

    InputTextAST(String label, String name, String def, String prompt, String placeHolder, ASTNode parent,
                 Location location) {
        super(label, name, def, prompt, parent, location);
        this.placeHolder = placeHolder;
    }

    /**
     * Get the placeholder.
     *
     * @return placeholder
     */
    public String placeHolder() {
        return placeHolder;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static InputTextAST create(InputText input, ASTNode parent, Location location) {
        InputTextAST result =
                new InputTextAST(input.label(), input.name(), input.def(), input.prompt(),
                        input.placeHolder(),
                        parent,
                        location);
        result.help(input.help());
        return result;
    }
}
