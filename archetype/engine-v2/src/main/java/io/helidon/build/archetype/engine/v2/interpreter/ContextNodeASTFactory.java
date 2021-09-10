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

import io.helidon.build.archetype.engine.v2.descriptor.ContextBoolean;
import io.helidon.build.archetype.engine.v2.descriptor.ContextEnum;
import io.helidon.build.archetype.engine.v2.descriptor.ContextList;
import io.helidon.build.archetype.engine.v2.descriptor.ContextNode;
import io.helidon.build.archetype.engine.v2.descriptor.ContextText;

/**
 * Factory for {@link ContextNodeAST} instances.
 */
abstract class ContextNodeASTFactory {

    static ContextNodeAST create(ContextNode node, ASTNode parent, ASTNode.Location location) {
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
}
