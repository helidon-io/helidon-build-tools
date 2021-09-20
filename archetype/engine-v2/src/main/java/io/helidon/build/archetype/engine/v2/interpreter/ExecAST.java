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

import io.helidon.build.archetype.engine.v2.descriptor.Exec;

/**
 * Archetype exec AST node.
 */
public class ExecAST extends ASTNode {

    private final String url;
    private final String src;
    private String help;

    ExecAST(String url, String src, ASTNode parent, Location location) {
        super(parent, location);
        this.url = url;
        this.src = src;
    }

    /**
     * Get the help.
     *
     * @return help
     */
    public String help() {
        return help;
    }

    /**
     * Set the help content.
     *
     * @param help help content
     */
    public void help(String help) {
        this.help = help;
    }

    /**
     * Get the source.
     *
     * @return source as a String
     */
    public String src() {
        return src;
    }

    /**
     * Get the url.
     *
     * @return url as a String
     */
    public String url() {
        return url;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static ExecAST create(Exec execFrom, ASTNode parent, Location location) {
        return new ExecAST(
                execFrom.url(),
                execFrom.src(),
                parent,
                location
        );
    }
}
