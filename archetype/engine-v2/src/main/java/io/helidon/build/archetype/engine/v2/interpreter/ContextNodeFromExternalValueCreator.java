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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create {@code ContextNodeAST} instance using default values of the corresponding {@code InputNodeAST} node.
 */
class ContextNodeFromExternalValueCreator extends GenericVisitorEmptyImpl<ContextNodeAST, ASTNode> {

    private final Map<String, String> externalValues;

    ContextNodeFromExternalValueCreator(Map<String, String> externalValues) {
        this.externalValues = externalValues;
    }

    @Override
    public ContextNodeAST visit(InputEnumAST input, ASTNode arg) {
        String path = input.path();
        if (externalValues.containsKey(path)) {
            ContextEnumAST result = new ContextEnumAST(path);
            result.value(externalValues.get(path));
            return result;
        }
        return null;
    }

    @Override
    public ContextNodeAST visit(InputListAST input, ASTNode arg) {
        String path = input.path();
        if (externalValues.containsKey(path)) {
            ContextListAST result = new ContextListAST(path);
            List<String> values = Stream.of(externalValues.get(path).split(",")).map(String::trim).collect(Collectors.toList());
            result.values().addAll(values);
            return result;
        }
        return null;
    }

    @Override
    public ContextNodeAST visit(InputBooleanAST input, ASTNode arg) {
        String path = input.path();
        if (externalValues.containsKey(path)) {
            ContextBooleanAST result = new ContextBooleanAST(path);
            result.bool(externalValues.get(path).trim().equalsIgnoreCase("yes"));
            return result;
        }
        return null;
    }

    @Override
    public ContextNodeAST visit(InputTextAST input, ASTNode arg) {
        String path = input.path();
        if (externalValues.containsKey(path)) {
            ContextTextAST result = new ContextTextAST(path);
            result.text(externalValues.get(path));
            return result;
        }
        return null;
    }
}
