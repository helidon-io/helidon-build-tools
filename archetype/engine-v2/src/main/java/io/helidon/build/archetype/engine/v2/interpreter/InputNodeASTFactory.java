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

import io.helidon.build.archetype.engine.v2.descriptor.InputBoolean;
import io.helidon.build.archetype.engine.v2.descriptor.InputEnum;
import io.helidon.build.archetype.engine.v2.descriptor.InputList;
import io.helidon.build.archetype.engine.v2.descriptor.InputNode;
import io.helidon.build.archetype.engine.v2.descriptor.InputText;

/**
 * Factory for {@link InputNodeAST} instances.
 */
abstract class InputNodeASTFactory {

    static InputNodeAST create(InputNode input, ASTNode parent, String currentDirectory) {
        if (input instanceof InputBoolean) {
            return InputBooleanAST.from((InputBoolean) input, parent, currentDirectory);
        }
        if (input instanceof InputEnum) {
            return InputEnumAST.create((InputEnum) input, parent, currentDirectory);
        }
        if (input instanceof InputList) {
            return InputListAST.from((InputList) input, parent, currentDirectory);
        }
        if (input instanceof InputText) {
            return InputTextAST.create((InputText) input, parent, currentDirectory);
        }
        throw new InterpreterException(String.format("Unsupported type of the InputNode with name %s and type %s",
                input.name(),
                input.getClass()
        ));
    }
}
