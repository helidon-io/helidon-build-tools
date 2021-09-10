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

import io.helidon.build.archetype.engine.v2.descriptor.FileSet;

/**
 * Archetype file AST node in {@link OutputAST} node.
 */
public class FileSetAST extends ASTNode {

    private final String source;
    private final String target;

    FileSetAST(String source, String target, ASTNode parent, String currentDirectory) {
        super(parent, currentDirectory);
        this.source = source;
        this.target = target;
    }

    /**
     * Get the source.
     *
     * @return source
     */
    public String source() {
        return source;
    }

    /**
     * Get the target.
     *
     * @return target
     */
    public String target() {
        return target;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static FileSetAST create(FileSet fileSetFrom, ASTNode parent, String currentDirectory) {
        return new FileSetAST(fileSetFrom.source(), fileSetFrom.target(), parent, currentDirectory);
    }
}
