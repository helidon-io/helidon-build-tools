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

import io.helidon.build.archetype.engine.v2.descriptor.ContextBoolean;
import io.helidon.build.archetype.engine.v2.descriptor.ContextEnum;
import io.helidon.build.archetype.engine.v2.descriptor.ContextList;
import io.helidon.build.archetype.engine.v2.descriptor.ContextNode;
import io.helidon.build.archetype.engine.v2.descriptor.ContextText;

/**
 * Factory for {@link ContextNodeAST} instances.
 */
public abstract class ContextNodeASTFactory {

    /**
     * Creates a new instance of the {@code ContextNodeAST}.
     *
     * @param node     ContextNode
     * @param parent   parent for the new ContextNodeAST instance
     * @param location ASTNode.Location for the new ContextNodeAST instance
     * @return a new instance of the {@code ContextNodeAST}
     */
    public static ContextNodeAST create(ContextNode node, ASTNode parent, ASTNode.Location location) {
        if (node instanceof ContextBoolean) {
            return ContextBooleanAST.create((ContextBoolean) node, parent, location);
        }
        if (node instanceof ContextEnum) {
            return ContextEnumAST.create((ContextEnum) node, parent, location);
        }
        if (node instanceof ContextList) {
            return ContextListAST.create((ContextList) node, parent, location);
        }
        if (node instanceof ContextText) {
            return ContextTextAST.create((ContextText) node, parent, location);
        }
        throw new InterpreterException(String.format("Unsupported type of the ContextNode with path %s and type %s",
                node.path(),
                node.getClass()
        ));
    }

    /**
     * Creates a new instance of the {@code ContextNodeAST}.
     *
     * @param input       InputNodeAST
     * @param path        path for the new instance
     * @param stringValue string representation of the value that will be stored inside the new instance of the {@code
     *                    ContextNodeAST}
     * @return a new instance of the {@code ContextNodeAST}
     */
    public static ContextNodeAST create(InputNodeAST input, String path, String stringValue) {
        if (input instanceof InputBooleanAST) {
            ContextBooleanAST result = new ContextBooleanAST(path);
            result.bool(Boolean.parseBoolean(stringValue));
            return result;
        }
        if (input instanceof InputEnumAST) {
            ContextEnumAST result = new ContextEnumAST(path);
            result.value(stringValue);
            return result;
        }
        if (input instanceof InputListAST) {
            ContextListAST result = new ContextListAST(path);
            result.values().addAll(List.of(stringValue.split(",")));
            return result;
        }
        if (input instanceof InputTextAST) {
            ContextTextAST result = new ContextTextAST(path);
            result.text(stringValue);
            return result;
        }
        throw new InterpreterException(String.format("Unsupported type of the InputNodeAST with type %s, path %s and value %s",
                input.getClass().getName(),
                path,
                stringValue
        ));
    }
}
