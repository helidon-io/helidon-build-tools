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

import io.helidon.build.archetype.engine.v2.descriptor.Replacement;
import io.helidon.build.archetype.engine.v2.descriptor.Transformation;

/**
 * Archetype transformation AST node in {@link OutputAST}.
 */
public class TransformationAST extends ASTNode {

    private final String id;
    private final LinkedList<Replacement> replacements;

    TransformationAST(String id, LinkedList<Replacement> replacements, ASTNode parent, Location location) {
        super(parent, location);
        this.id = id;
        this.replacements = replacements;
    }

    /**
     * Get the transformation id.
     *
     * @return transformation id, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Get the replacements.
     *
     * @return list of replacement, never {@code null}
     */
    public LinkedList<Replacement> replacements() {
        return replacements;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static TransformationAST create(Transformation transformationFrom, ASTNode parent, Location location) {
        return new TransformationAST(transformationFrom.id(), transformationFrom.replacements(), parent, location);
    }
}
