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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create {@code ContextNodeAST} instance using default values of the corresponding {@code InputNodeAST} node.
 */
class ContextNodeFromDefaultValueCreator extends GenericVisitorEmptyImpl<ContextNodeAST, ASTNode> {

    @Override
    public ContextNodeAST visit(InputEnumAST input, ASTNode arg) {
        if (input.defaultValue() == null) {
            return null;
        }
        ContextEnumAST result = new ContextEnumAST(input.path());
        result.value(input.defaultValue());
        return result;
    }

    @Override
    public ContextNodeAST visit(InputListAST input, ASTNode arg) {
        if (input.defaultValue() == null) {
            return null;
        }
        ContextListAST result = new ContextListAST(input.path());
        List<String> values = Stream.of(input.defaultValue().split(",")).map(String::trim).collect(Collectors.toList());
        result.values().addAll(values);
        return result;
    }

    @Override
    public ContextNodeAST visit(InputBooleanAST input, ASTNode arg) {
        if (input.defaultValue() == null) {
            return null;
        }
        ContextBooleanAST result = new ContextBooleanAST(input.path());
        result.bool(input.defaultValue().trim().equalsIgnoreCase("yes"));
        return result;
    }

    @Override
    public ContextNodeAST visit(InputTextAST input, ASTNode arg) {
        if (input.defaultValue() == null) {
            return null;
        }
        ContextTextAST result = new ContextTextAST(input.path());
        result.text(input.defaultValue());
        return result;
    }
}
