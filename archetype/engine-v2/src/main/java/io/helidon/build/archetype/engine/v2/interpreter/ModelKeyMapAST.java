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

import java.util.LinkedList;

import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;

/**
 * Archetype map AST node with key attribute used in {@link ModelAST} and {@link MapTypeAST}.
 */
public class ModelKeyMapAST extends MapTypeAST {

    private final String key;

    ModelKeyMapAST(String key, int order, String currentDirectory) {
        super(order, currentDirectory);
        this.key = key;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    /**
     * Get the key of the map.
     *
     * @return key
     */
    public String key() {
        return key;
    }

    static ModelKeyMapAST from(ModelKeyMap map, String currentDirectory) {
        ModelKeyMapAST result = new ModelKeyMapAST(map.key(), map.order(), currentDirectory);

        LinkedList<Visitable> children = getChildren(map, currentDirectory);
        ConditionalNode.addChildren(map, result, children, currentDirectory);

        return result;
    }

    private static LinkedList<Visitable> getChildren(ModelKeyMap map, String currentDirectory) {
        LinkedList<Visitable> result = new LinkedList<>();
        result.addAll(map.keyValues());
        result.addAll(transformList(map.keyLists(), l -> ModelKeyListAST.from(l, currentDirectory)));
        result.addAll(transformList(map.keyMaps(), m -> ModelKeyMapAST.from(m, currentDirectory)));
        return result;
    }

}
