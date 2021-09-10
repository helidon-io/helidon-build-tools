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

import io.helidon.build.archetype.engine.v2.descriptor.FileSets;

/**
 * Archetype files AST node in {@link OutputAST} node.
 */
public class FileSetsAST extends ASTNode {

    private final LinkedList<String> transformations;
    private final LinkedList<String> includes;
    private final LinkedList<String> excludes;
    private final String directory;

    FileSetsAST(LinkedList<String> transformations, LinkedList<String> includes, LinkedList<String> excludes, String directory,
                ASTNode parent, Location location) {
        super(parent, location);
        this.directory = directory;
        this.transformations = transformations;
        this.includes = includes;
        this.excludes = excludes;
    }

    /**
     * Get the exclude filters.
     *
     * @return list of exclude filter, never {@code null}
     */
    public LinkedList<String> excludes() {
        return excludes;
    }

    /**
     * Get the include filters.
     *
     * @return list of include filter, never {@code null}
     */
    public LinkedList<String> includes() {
        return includes;
    }

    /**
     * Get the applied transformations.
     *
     * @return list of transformation, never {@code null}
     */
    public LinkedList<String> transformations() {
        return transformations;
    }

    /**
     * Get the directory of this file set.
     *
     * @return directory optional, never {@code null}
     */
    public String directory() {
        return directory;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static FileSetsAST create(FileSets fileSetsFrom, ASTNode parent, Location location) {
        return new FileSetsAST(
                fileSetsFrom.transformations(),
                fileSetsFrom.includes(),
                fileSetsFrom.excludes(),
                fileSetsFrom.directory().get(),
                parent,
                location);
    }
}
