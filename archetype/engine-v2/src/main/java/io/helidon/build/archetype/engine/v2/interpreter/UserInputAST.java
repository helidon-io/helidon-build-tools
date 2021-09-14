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
 * User input.
 */
public class UserInputAST extends ASTNode {

    private final String label;
    private final String help;
    private final String path;

    UserInputAST(String label, String help, String path, Location location) {
        super(null, location);
        this.label = label;
        this.help = help;
        this.path = path;
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
     * Get the help.
     *
     * @return help
     */
    public String help() {
        return help;
    }

    /**
     * Get the path.
     *
     * @return path
     */
    public String path() {
        return path;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static UserInputAST create(InputNodeAST input, StepAST step) {
        return new UserInputAST(step.label(), step.help(), input.path(), input.location());
    }
}