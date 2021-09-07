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
 * Base class for {@link InputAST} nodes.
 */
public abstract class InputNodeAST extends ASTNode {

    private final String label;
    private final String name;
    private final String def;
    private final String prompt;
    private String help;
    private boolean optional = false;

    InputNodeAST(String label, String name, String def, String prompt, ASTNode parent, String currentDirectory) {
        super(parent, currentDirectory);
        this.label = label;
        this.name = name;
        this.def = def;
        this.prompt = prompt;
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
     * Get the label.
     *
     * @return label
     */
    public String label() {
        return label;
    }

    /**
     * Get the name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Get the default value.
     *
     * @return default value
     */
    public String def() {
        return def;
    }

    /**
     * Get the prompt.
     *
     * @return prompt
     */
    public String prompt() {
        return prompt;
    }

    /**
     * Get the optional attribute.
     *
     * @return boolean
     */
    public boolean isOptional() {
        return optional;
    }

    public String path() {
        StringBuilder path = new StringBuilder(name);
        if (parent() != null) {
            calculatePath(parent(), path);
        }
        return path.toString();
    }

    private void calculatePath(ASTNode parent, StringBuilder path) {
        if (parent.parent() != null) {
            if (parent.parent() instanceof InputNodeAST) {
                path.insert(0, ((InputNodeAST) parent.parent()).name() + ".");
            }
            calculatePath(parent.parent(), path);
        }
    }
}
