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
import java.util.stream.Collectors;

/**
 * Visitor to prepare user input AST node.
 */
public class UserInputVisitor extends GenericVisitorEmptyImpl<InputNodeAST, ASTNode> {

    @Override
    public InputNodeAST visit(InputEnumAST input, ASTNode arg) {
        InputEnumAST result = new InputEnumAST(
                input.label(), input.name(), input.defaultValue(), input.prompt(), input.isOptional(), null,
                ASTNode.Location.builder().build());
        result.help(input.help());
        result.children().addAll(input.children().stream()
                .filter(c -> c instanceof OptionAST)
                .map(o -> copyOption((OptionAST) o))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }

    @Override
    public InputNodeAST visit(InputListAST input, ASTNode arg) {
        InputListAST result = new InputListAST(
                input.label(), input.name(), input.defaultValue(), input.prompt(), input.min(),
                input.max(), input.isOptional(), null, ASTNode.Location.builder().build());
        result.help(input.help());
        result.children().addAll(input.children().stream()
                .filter(c -> c instanceof OptionAST)
                .map(o -> copyOption((OptionAST) o))
                .collect(Collectors.toCollection(LinkedList::new)));
        return result;
    }

    @Override
    public InputNodeAST visit(InputBooleanAST input, ASTNode arg) {
        InputBooleanAST result = new InputBooleanAST(
                input.label(), input.name(), input.defaultValue(), input.prompt(), input.isOptional(), null,
                ASTNode.Location.builder().build());
        result.help(input.help());
        return result;
    }

    @Override
    public InputNodeAST visit(InputTextAST input, ASTNode arg) {
        return new InputTextAST(
                input.label(),
                input.name(),
                input.defaultValue(),
                input.prompt(),
                input.placeHolder(),
                input.isOptional(),
                null,
                ASTNode.Location.builder().build());
    }

    private OptionAST copyOption(OptionAST optionFrom) {
        return new OptionAST(optionFrom.label(), optionFrom.value(), optionFrom.help(), null, ASTNode.Location.builder().build());
    }
}
