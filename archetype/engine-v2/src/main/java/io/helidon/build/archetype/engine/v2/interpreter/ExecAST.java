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

import java.nio.file.Paths;

import io.helidon.build.archetype.engine.v2.descriptor.Exec;

/**
 * Archetype exec AST node.
 */
public class ExecAST extends ASTNode {

    private final String url;
    private final String src;

    ExecAST(String url, String src, String currentDirectory) {
        super(currentDirectory);
        this.url = url;
        this.src = src;
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

    /**
     * Create a new instance of the {@code ExecAST} from the {@code Exec} instance.
     *
     * @param exec Exec instance
     * @return a new instance of the {@code ExecAST}
     */
    static ExecAST from(Exec exec) {
        return new ExecAST(
                exec.url(),
                exec.src(),
                Paths.get(exec.src()).getParent().toString()
        );
    }
}
