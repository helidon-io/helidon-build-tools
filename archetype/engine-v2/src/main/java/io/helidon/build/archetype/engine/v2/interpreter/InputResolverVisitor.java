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

class InputResolverVisitor extends VisitorEmptyImpl<ASTNode> {

    @Override
    public void visit(InputEnumAST input, ASTNode arg) {
        if (arg instanceof ContextEnumAST) {
            LinkedList<Visitable> resolvedOptions = input.children().stream()
                    .filter(c -> c instanceof OptionAST)
                    .filter(o -> ((OptionAST) o).value().equals(((ContextEnumAST) arg).value()))
                    .collect(Collectors.toCollection(LinkedList::new));
            input.children().removeIf(c -> c instanceof OptionAST);
            input.children().addAll(resolvedOptions);
        }
    }

    @Override
    public void visit(InputListAST input, ASTNode arg) {
        if (arg instanceof ContextListAST) {
            LinkedList<Visitable> resolvedOptions = input.children().stream()
                    .filter(c -> c instanceof OptionAST)
                    .filter(o -> ((ContextListAST) arg).values().contains(((OptionAST) o).value()))
                    .collect(Collectors.toCollection(LinkedList::new));
            input.children().removeIf(c -> c instanceof OptionAST);
            input.children().addAll(resolvedOptions);
        }
    }
}
