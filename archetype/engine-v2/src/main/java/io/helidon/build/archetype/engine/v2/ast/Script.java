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
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ScriptLoader;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Script.
 */
public final class Script extends DeclaredBlock {

    private final Map<String, Method> methods;

    private Script(Builder builder, Map<String, Method> methods) {
        super(builder);
        this.methods = methods;
    }

    /**
     * Get the methods.
     *
     * @return methods
     */
    public Map<String, Method> methods() {
        return methods;
    }

    @Override
    public String blockName() {
        return scriptPath().toString();
    }

    @Override
    public String toString() {
        return "Script{"
                + "path=" + scriptPath()
                + '}';
    }

    /**
     * Create a new builder.
     *
     * @param loader     script loader
     * @param scriptPath script path
     * @return builder
     */
    public static Builder builder(ScriptLoader loader, Path scriptPath) {
        return new Builder(loader, scriptPath);
    }

    /**
     * Script builder.
     */
    public static final class Builder extends Block.Builder {

        private Builder(ScriptLoader loader, Path scriptPath) {
            super(loader, scriptPath, null, Kind.SCRIPT);
        }

        @Override
        protected Script doBuild() {
            // get and remove the method declarations from the nested builders
            List<Node.Builder<? extends Node, ?>> nestedBuilders = nestedBuilders();
            Block.Builder methodBuilder = null;
            for (Node.Builder<? extends Node, ?> builder : nestedBuilders) {
                if (builder instanceof Block.Builder && ((Block.Builder) builder).kind() == Kind.METHODS) {
                    methodBuilder = (Block.Builder) builder;
                    break;
                }
            }
            Map<String, Method> methods;
            if (methodBuilder != null) {
                nestedBuilders.remove(methodBuilder);
                methods = methodBuilder.childrenStream(Method.class)
                                       .collect(toUnmodifiableMap(Method::name, identity()));
            } else {
                methods = Map.of();
            }
            return new Script(this, methods);
        }

        @Override
        public Script build() {
            return (Script) super.build();
        }
    }
}
