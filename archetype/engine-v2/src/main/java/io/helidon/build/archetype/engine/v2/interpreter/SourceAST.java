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

import io.helidon.build.archetype.engine.v2.descriptor.Source;

/**
 * Archetype source AST node.
 */
public class SourceAST extends ASTNode {

    private final String src;
    private final String url;

    SourceAST(String src, String url, String currentDirectory) {
        super(currentDirectory);
        this.src = src;
        this.url = url;
    }

    /**
     * Get the source attribute.
     *
     * @return source
     */
    public String source() {
        return src;
    }

    /**
     * Get the url attribute.
     *
     * @return url
     */
    public String url() {
        return url;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static SourceAST from(Source source, String currentDirectory) {
        return new SourceAST(source.source(), source.url(), currentDirectory);
    }
}
