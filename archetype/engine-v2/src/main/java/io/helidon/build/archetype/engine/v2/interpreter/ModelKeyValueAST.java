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

import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;

/**
 * Archetype value AST node used in {@link ListTypeAST}.
 */
public class ModelKeyValueAST extends ValueTypeAST {

    private final String key;

    ModelKeyValueAST(String key, String url, String file, String template, int order, String value,
                     ASTNode parent, Location location) {
        super(url, file, template, order, value, parent, location);
        this.key = key;
    }

    /**
     * Get the key of the map.
     *
     * @return key
     */
    public String key() {
        return key;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static ModelKeyValueAST create(ModelKeyValue valueFrom, ASTNode parent, Location location) {
        return new ModelKeyValueAST(valueFrom.key(), valueFrom.url(), valueFrom.file(), valueFrom.template(),
                valueFrom.order(), valueFrom.value(), parent, location);
    }
}
