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

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v2.descriptor.ContextBoolean;
import io.helidon.build.archetype.engine.v2.descriptor.ContextEnum;
import io.helidon.build.archetype.engine.v2.descriptor.ContextList;
import io.helidon.build.archetype.engine.v2.descriptor.ContextText;
import io.helidon.build.archetype.engine.v2.descriptor.FileSet;
import io.helidon.build.archetype.engine.v2.descriptor.FileSets;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.descriptor.Template;
import io.helidon.build.archetype.engine.v2.descriptor.Templates;
import io.helidon.build.archetype.engine.v2.descriptor.Transformation;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

/**
 * Interpret user inputs and produce new steps.
 */
public class Interpreter implements Visitor<ASTNode> {

    private final Prompter prompter;
    private final Archetype archetype;

    Interpreter(Prompter prompter, Archetype archetype) {
        this.prompter = prompter;
        this.archetype = archetype;
    }

    @Override
    public void visit(Visitable v, ASTNode parent) {
        v.accept(this, parent);
    }

    @Override
    public void visit(StepAST step, ASTNode parent) {
        resolveInputs(step);
        acceptAll(step.children(), step);
    }

    @Override
    public void visit(InputAST input, ASTNode parent) {
        resolveInputs(input);
        acceptAll(input.children(), input);
    }

    @Override
    public void visit(InputBooleanAST input, ASTNode parent) {
        resolveInputs(input);
        acceptAll(input.children(), input);
    }

    @Override
    public void visit(InputEnumAST input, ASTNode parent) {
        resolveInputs(input);
        acceptAll(input.children(), input);
    }

    @Override
    public void visit(InputListAST input, ASTNode parent) {
        resolveInputs(input);
        acceptAll(input.children(), input);
    }

    @Override
    public void visit(InputTextAST input, ASTNode parent) {

    }

    @Override
    public void visit(ExecAST exec, ASTNode parent) {
        ArchetypeDescriptor descriptor = archetype.getDescriptor(exec.src());
        XmlDescriptor xmlDescriptor = XmlDescriptor.from(descriptor, exec.currentDirectory());
        acceptAll(xmlDescriptor.children(), parent);
        if (parent instanceof HelpNode) {
            ((HelpNode) parent).addHelp(xmlDescriptor.help());
        }
        parent.children().addAll(xmlDescriptor.children());
    }

    @Override
    public void visit(SourceAST source, ASTNode parent) {
        ArchetypeDescriptor descriptor = archetype.getDescriptor(source.source());
        XmlDescriptor xmlDescriptor = XmlDescriptor.from(descriptor, source.currentDirectory());
        acceptAll(xmlDescriptor.children(), parent);
        if (parent instanceof HelpNode) {
            ((HelpNode) parent).addHelp(xmlDescriptor.help());
        }
        parent.children().addAll(xmlDescriptor.children());
    }

    @Override
    public void visit(ContextAST context, ASTNode parent) {

    }

    @Override
    public void visit(ContextBoolean contextBoolean, ASTNode parent) {

    }

    @Override
    public void visit(ContextEnum contextEnum, ASTNode parent) {

    }

    @Override
    public void visit(ContextList contextList, ASTNode parent) {

    }

    @Override
    public void visit(ContextText contextText, ASTNode parent) {

    }

    @Override
    public void visit(OptionAST option, ASTNode parent) {

    }

    @Override
    public void visit(OutputAST output, ASTNode parent) {

    }

    @Override
    public void visit(Transformation transformation, ASTNode parent) {

    }

    @Override
    public void visit(FileSets fileSets, ASTNode parent) {

    }

    @Override
    public void visit(FileSet fileSet, ASTNode parent) {

    }

    @Override
    public void visit(Template template, ASTNode parent) {

    }

    @Override
    public void visit(Templates templates, ASTNode parent) {

    }

    @Override
    public void visit(ModelAST model, ASTNode parent) {

    }

    @Override
    public void visit(IfStatement statement, ASTNode parent) {

    }

    @Override
    public void visit(ModelKeyValue value, ASTNode parent) {

    }

    @Override
    public void visit(ValueType value, ASTNode parent) {

    }

    @Override
    public void visit(ModelKeyListAST list, ASTNode parent) {

    }

    @Override
    public void visit(MapTypeAST map, ASTNode parent) {

    }

    @Override
    public void visit(ListTypeAST list, ASTNode parent) {

    }

    @Override
    public void visit(ModelKeyMapAST map, ASTNode parent) {

    }

    private void acceptAll(LinkedList<Visitable> nodes, ASTNode parent) {
        LinkedList<Visitable> list = new LinkedList<>(nodes);
        while (!list.isEmpty()) {
            list.pop().accept(this, parent);
        }
    }

    private LinkedList<InputAST> resolveInputs(ASTNode parent) {
        LinkedList<InputAST> result = new LinkedList<>();
        LinkedList<InputAST> unresolvedInputs = parent.children().stream()
                .filter(node -> node instanceof InputAST)
                .map(node -> (InputAST) node)
                .collect(Collectors.toCollection(LinkedList::new));
        if (!unresolvedInputs.isEmpty()) {
            LinkedList<InputAST> resolvedInputs = prompter.resolve(unresolvedInputs);
            result.addAll(resolvedInputs);
            //replace inputs in the parent node
            parent.children().removeIf(node -> node instanceof InputAST);
            parent.children().addAll(resolvedInputs);
        }
        return result;
    }
}
