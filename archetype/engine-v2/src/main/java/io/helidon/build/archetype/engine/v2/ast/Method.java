/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import io.helidon.build.archetype.engine.v2.ScriptLoader;

/**
 * Method.
 */
public final class Method extends DeclaredBlock {

    private final String name;

    private Method(Builder builder) {
        super(builder);
        this.name = builder.attribute("name", true).asString();
    }

    /**
     * Get the name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    @Override
    public String blockName() {
        return scriptPath() + "#" + name;
    }

    @Override
    public String toString() {
        return "Method{"
                + "name='" + name + '\''
                + '}';
    }

    /**
     * Create a new builder.
     *
     * @param loader     script loader
     * @param scriptPath script path
     * @param location   location
     * @return builder
     */
    public static Builder builder(ScriptLoader loader, Path scriptPath, Location location) {
        return new Builder(loader, scriptPath, location);
    }

    /**
     * Method builder.
     */
    public static final class Builder extends Block.Builder {

        private Builder(ScriptLoader loader, Path scriptPath, Location location) {
            super(loader, scriptPath, location, Kind.METHOD);
        }

        @Override
        protected Method doBuild() {
            return new Method(this);
        }

        @Override
        public Method build() {
            return (Method) super.build();
        }
    }
}
