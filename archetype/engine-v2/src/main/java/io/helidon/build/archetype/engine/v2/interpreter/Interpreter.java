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

import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
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
    private final Deque<StepAST> stepStack = new ArrayDeque<>();
    private final Map<String, ContextNodeAST> pathToContextNodeMap;
    private final InputResolverVisitor inputResolverVisitor = new InputResolverVisitor();
    private final UserInputVisitor userInputVisitor = new UserInputVisitor();
    private final List<UserInputAST> unresolvedInputs = new ArrayList<>();

    Interpreter(Prompter prompter, Archetype archetype) {
        this.prompter = prompter;
        this.archetype = archetype;
        pathToContextNodeMap = new HashMap<>();
    }

    @Override
    public void visit(Visitable v, ASTNode parent) {//Visitable
        //todo change
//        v.accept(this, parent);
    }

    @Override
    public void visit(Flow v, ASTNode parent) {//Visitable
        //todo change
//        v.accept(this, parent);
    }

    @Override
    public void visit(XmlDescriptor v, ASTNode parent) {//Visitable
        //todo change
//        v.accept(this, parent);
    }

    @Override
    public void visit(StepAST step, ASTNode parent) {
        stepStack.push(step);
        resolveInputs(step);
        acceptAll(step.children(), step);
        stepStack.pop();
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
        boolean result = resolve(input);
        if (!result) {
            InputNodeAST unresolvedUserInputNode = userInputVisitor.visit(input, parent);
            UserInputAST unresolvedInput = UserInputAST.create(input, stepStack.peek());
            unresolvedInput.children().add(unresolvedUserInputNode);
            unresolvedInputs.add(unresolvedInput);
            //TODO throw Break or something like this
        }
        acceptAll(input.children(), input);
    }

    private boolean resolve(InputNodeAST input) {
        ContextNodeAST contextNodeAST = pathToContextNodeMap.get(input.path());
        if (contextNodeAST != null) {
            input.accept(inputResolverVisitor, contextNodeAST);
        }
        return contextNodeAST != null;
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
        ArchetypeDescriptor descriptor = archetype.getDescriptor(resolveScriptPath(exec.currentDirectory(), exec.src()));
        //todo maybe need to change dir
        XmlDescriptor xmlDescriptor = XmlDescriptor.create(descriptor, exec, exec.currentDirectory());//exec.currentDirectory()
        resolveInputs(xmlDescriptor);
        acceptAll(xmlDescriptor.children(), exec);
        exec.help(xmlDescriptor.help());
        exec.children().addAll(xmlDescriptor.children());
    }

    @Override
    public void visit(SourceAST source, ASTNode parent) {
//        String descriptorPath = Paths.get(source.currentDirectory(), source.source()).toString();
        ArchetypeDescriptor descriptor = archetype.getDescriptor(resolveScriptPath(source.currentDirectory(), source.source()));
        //todo maybe need to change dir
//        String currentDir = Paths.get(source.currentDirectory(), source.source()).getParent().toString();
        String currentDir = Paths.get(resolveScriptPath(source.currentDirectory(), source.source())).getParent().toString();
        XmlDescriptor xmlDescriptor = XmlDescriptor.create(descriptor, source, currentDir);//source.currentDirectory()
        resolveInputs(xmlDescriptor);
        acceptAll(xmlDescriptor.children(), source);
        source.help(xmlDescriptor.help());
        source.children().addAll(xmlDescriptor.children());
    }

    private String resolveScriptPath(String currentDirectory, String scriptSrc) {
        return Paths.get(currentDirectory).resolve(scriptSrc).normalize().toString();
    }

    @Override
    public void visit(ContextAST context, ASTNode parent) {
        acceptAll(context.children(), context);
    }

    @Override
    public void visit(ContextBooleanAST contextBoolean, ASTNode parent) {
        pathToContextNodeMap.putIfAbsent(contextBoolean.path(), contextBoolean);
    }

    @Override
    public void visit(ContextEnumAST contextEnum, ASTNode parent) {
        pathToContextNodeMap.putIfAbsent(contextEnum.path(), contextEnum);
    }

    @Override
    public void visit(ContextListAST contextList, ASTNode parent) {
        pathToContextNodeMap.putIfAbsent(contextList.path(), contextList);
    }

    @Override
    public void visit(ContextTextAST contextText, ASTNode parent) {
        pathToContextNodeMap.putIfAbsent(contextText.path(), contextText);
    }

    @Override
    public void visit(OptionAST option, ASTNode parent) {
        resolveInputs(option);
        acceptAll(option.children(), option);
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
        resolveInputs(statement);
        acceptAll(statement.children(), statement);
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
            Visitable visitable = list.pop();
            visitable.accept(this, parent);
        }
    }

    private LinkedList<InputAST> resolveInputs(ASTNode parent) {
        LinkedList<InputAST> result = new LinkedList<>();
        LinkedList<InputAST> unresolvedInputs = parent.children().stream()
                .filter(node -> node instanceof InputAST)
                .map(node -> (InputAST) node)
                .collect(Collectors.toCollection(LinkedList::new));
        if (!unresolvedInputs.isEmpty()) {
            //todo REMOVE
            for (InputAST inputAST : unresolvedInputs) {
                inputAST.children().stream().filter(i -> i instanceof InputNodeAST).forEach(i -> System.out.println(((InputNodeAST) i).label()));
            }
            LinkedList<InputAST> resolvedInputs = prompter.resolve(unresolvedInputs);
            //todo REMOVE
            for (InputAST inputAST : resolvedInputs) {
                inputAST.children().stream().filter(i -> i instanceof InputNodeAST).forEach(i -> System.out.println("===" + ((InputNodeAST) i).label()));
            }
            result.addAll(resolvedInputs);
            //replace inputs in the parent node
            parent.children().removeIf(node -> node instanceof InputAST);
            parent.children().addAll(resolvedInputs);
        }
        return result;
    }
}
