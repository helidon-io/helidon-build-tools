/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.ast;

import java.nio.file.Path;

import static java.lang.Boolean.parseBoolean;

/**
 * Step.
 */
public class Step extends Block {

    private final String label;
    private final String help;
    private final boolean optional;

    private Step(Builder builder) {
        super(builder);
        label = builder.attribute("label", false);
        help = builder.attribute("help", false);
        optional = parseBoolean(builder.attribute("optional", false));
    }

    /**
     * Get the step label.
     *
     * @return name
     */
    public String label() {
        return label;
    }

    /**
     * Get the step help.
     *
     * @return name
     */
    public String help() {
        return help;
    }

    /**
     * Test if this step is optional.
     *
     * @return {@code true} if optional, {@code false} otherwise
     */
    public boolean isOptional() {
        return optional;
    }

    @Override
    public <A> VisitResult accept(Visitor<A> visitor, A arg) {
        return visitor.visitStep(this, arg);
    }

    @Override
    public <A> VisitResult acceptAfter(Visitor<A> visitor, A arg) {
        return visitor.postVisitStep(this, arg);
    }

    @Override
    public String toString() {
        return "Step{"
                + "label='" + label() + '\''
                + ", optional=" + isOptional()
                + '}';
    }

    /**
     * Create a new Step block builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @param kind       block kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Kind kind) {
        return new Builder(scriptPath, position, kind);
    }

    /**
     * Step builder.
     */
    public static final class Builder extends Block.Builder {

        private Builder(Path scriptPath, Position position, Kind kind) {
            super(scriptPath, position, kind);
        }

        @Override
        protected Block doBuild() {
            return new Step(this);
        }
    }
}
