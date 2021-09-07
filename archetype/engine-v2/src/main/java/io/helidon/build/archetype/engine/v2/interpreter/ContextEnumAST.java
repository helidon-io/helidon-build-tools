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

import io.helidon.build.archetype.engine.v2.descriptor.ContextEnum;

/**
 * Archetype AST enum node in {@link ContextAST} nodes.
 */
public class ContextEnumAST extends ContextNodeAST {

    private String value;

    ContextEnumAST(String path, ASTNode parent, String currentDirectory) {
        super(path, parent, currentDirectory);
    }

    /**
     * Get the enum value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Set the enum value.
     */
    public void value(String value) {
        this.value = value;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static ContextEnumAST create(ContextEnum enumFrom, ASTNode parent, String currentDirectory) {
        ContextEnumAST result = new ContextEnumAST(enumFrom.path(), parent, currentDirectory);
        result.value(enumFrom.values().peek());
        return result;
    }
}
