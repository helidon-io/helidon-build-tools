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

import java.util.LinkedList;

import io.helidon.build.archetype.engine.v2.descriptor.ContextList;

/**
 * Archetype AST list node in {@link ContextAST} nodes.
 */
public class ContextListAST extends ContextNodeAST {

    private final LinkedList<String> values;

    ContextListAST(String path, ASTNode parent, String currentDirectory) {
        super(path, parent, currentDirectory);
        values = new LinkedList<>();
    }

    /**
     * Get the list values.
     *
     * @return values
     */
    public LinkedList<String> values() {
        return values;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static ContextListAST create(ContextList listFrom, ASTNode parent, String currentDirectory) {
        ContextListAST result = new ContextListAST(listFrom.path(), parent, currentDirectory);
        result.values.addAll(listFrom.values());
        return result;
    }
}
