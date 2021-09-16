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

import io.helidon.build.archetype.engine.v2.descriptor.Template;

/**
 * Archetype template AST node in {@link OutputAST}.
 */
public class TemplateAST extends ASTNode implements ConditionalNode {

    private final String engine;
    private final String source;
    private final String target;

    TemplateAST(String engine, String source, String target, ASTNode parent, Location location) {
        super(parent, location);
        this.engine = engine;
        this.source = source;
        this.target = target;
    }

    /**
     * Get the engine.
     *
     * @return engine
     */
    public String engine() {
        return engine;
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

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static TemplateAST create(Template templateFrom, ASTNode parent, Location location) {
        TemplateAST result = new TemplateAST(templateFrom.engine(), templateFrom.source(), templateFrom.target(), parent,
                location);
        if (templateFrom.model() != null) {
            ModelAST model = ModelAST.create(templateFrom.model(), result, location);
            result.children().add(ConditionalNode.mapConditional(templateFrom.model(), model, result, location));
        }
        return result;
    }

}
