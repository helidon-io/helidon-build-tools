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

/**
 * Archetype script AST node.
 */
public abstract class ScriptAST extends ASTNode {

    private final String src;
    private final String url;
    private String help;

    ScriptAST(String src, String url, ASTNode parent, Location location) {
        super(parent, location);
        this.src = src;
        this.url = url;
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

    /**
     * Returns the include type, e.g. "exec" or "source".
     * @return The type.
     */
    public abstract String includeType();
}
