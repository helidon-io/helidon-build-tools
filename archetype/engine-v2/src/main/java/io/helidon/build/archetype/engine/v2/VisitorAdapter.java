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

package io.helidon.build.archetype.engine.v2;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Validation;

/**
 * Visitor adapter.
 * Combines all visitor interfaces together in order to facilitate the use of individual visitor interfaces.
 *
 * @param <A> visitor argument type
 */
public class VisitorAdapter<A> implements Node.Visitor<A>, Block.Visitor<A> {

    private final Input.Visitor<A> inputVisitor;
    private final Output.Visitor<A> outputVisitor;
    private final Model.Visitor<A> modelVisitor;
    private final Validation.Visitor<A> validationVisitor;

    /**
     * Create a new adapter for the given input visitor.
     *
     * @param validationVisitor  validation visitor
     * @param inputVisitor       input visitor
     * @param outputVisitor      output visitor
     * @param modelVisitor       model visitor
     */
    protected VisitorAdapter(Input.Visitor<A> inputVisitor,
                             Output.Visitor<A> outputVisitor,
                             Model.Visitor<A> modelVisitor,
                             Validation.Visitor<A> validationVisitor) {
        this.validationVisitor = validationVisitor;
        this.inputVisitor = inputVisitor;
        this.outputVisitor = outputVisitor;
        this.modelVisitor = modelVisitor;
    }

    @Override
    public VisitResult visitBlock(Block block, A arg) {
        return block.accept((Block.Visitor<A>) this, arg);
    }

    @Override
    public VisitResult postVisitBlock(Block block, A arg) {
        return block.acceptAfter((Block.Visitor<A>) this, arg);
    }

    @Override
    public VisitResult visitInput(Input input, A arg) {
        if (inputVisitor != null) {
            return input.accept(inputVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitInput(Input input, A arg) {
        if (inputVisitor != null) {
            return input.acceptAfter(inputVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitOutput(Output output, A arg) {
        if (outputVisitor != null) {
            return output.accept(outputVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitOutput(Output output, A arg) {
        if (outputVisitor != null) {
            return output.acceptAfter(outputVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitModel(Model model, A arg) {
        if (modelVisitor != null) {
            return model.accept(modelVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitModel(Model model, A arg) {
        if (modelVisitor != null) {
            return model.acceptAfter(modelVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitValidation(Validation validation, A arg) {
        if (validationVisitor != null) {
            return validation.accept(validationVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitValidation(Validation validation, A arg) {
        if (validationVisitor != null) {
            return validation.acceptAfter(validationVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitRegex(Validation.Regex regex, A arg) {
        if (validationVisitor != null) {
            return regex.accept(validationVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitRegex(Validation.Regex regex, A arg) {
        if (validationVisitor != null) {
            return regex.acceptAfter(validationVisitor, arg);
        }
        return VisitResult.CONTINUE;
    }
}
