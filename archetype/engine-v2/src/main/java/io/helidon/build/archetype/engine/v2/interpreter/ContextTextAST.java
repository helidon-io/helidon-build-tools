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

import io.helidon.build.archetype.engine.v2.descriptor.ContextText;

/**
 * Archetype AST text node in {@link ContextAST} nodes.
 */
public class ContextTextAST extends ContextNodeAST {

    private String text;

    ContextTextAST(String path, ASTNode parent, Location location) {
        super(path, parent, location);
    }

    /**
     * Create a new instance.
     *
     * @param path path
     */
    public ContextTextAST(String path) {
        super(path, null, Location.builder().build());
    }

    /**
     * Get text string from text element.
     *
     * @return text
     */
    public String text() {
        return text;
    }

    /**
     * Set text string from text element.
     *
     * @param text content
     */
    public void text(String text) {
        this.text = text;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static ContextTextAST create(ContextText textFrom, ASTNode parent, Location location) {
        ContextTextAST result = new ContextTextAST(textFrom.path(), parent, location);
        result.text(textFrom.text());
        return result;
    }
}
