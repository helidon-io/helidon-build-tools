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
import java.util.List;
import java.util.Map;
import java.util.Queue;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v2.descriptor.Input;

/**
 * Interpret user inputs and produce new steps.
 */
public class Interpreter implements Visitor<ASTNode> {

    private final Archetype archetype;
    private final Map<String, ContextNodeAST> pathToContextNodeMap;
    private final InputResolverVisitor inputResolverVisitor = new InputResolverVisitor();
    private final UserInputVisitor userInputVisitor = new UserInputVisitor();
    private final List<UserInputAST> unresolvedInputs = new ArrayList<>();
    private final Deque<ASTNode> stack = new ArrayDeque<>();

    Interpreter(Archetype archetype) {
        this.archetype = archetype;
        pathToContextNodeMap = new HashMap<>();
    }

    public Queue<ASTNode> stack() {
        return stack;
    }

    public List<UserInputAST> unresolvedInputs() {
        return unresolvedInputs;
    }

    @Override
    public void visit(Visitable v, ASTNode parent) {
        //todo change
//        v.accept(this, parent);
    }

    @Override
    public void visit(Flow v, ASTNode parent) {
        //todo change
//        v.accept(this, parent);
    }

    @Override
    public void visit(XmlDescriptor xmlDescriptor, ASTNode parent) {
        pushToStack(xmlDescriptor);
        acceptAll(xmlDescriptor, parent);
        stack.pop();
    }

    @Override
    public void visit(StepAST input, ASTNode parent) {
        pushToStack(input);
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(InputAST input, ASTNode parent) {
        pushToStack(input);
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(InputBooleanAST input, ASTNode parent) {
        pushToStack(input);
        boolean result = resolve(input);
        if (!result) {
            InputNodeAST unresolvedUserInputNode = userInputVisitor.visit(input, parent);
            processUnresolvedInput(input, unresolvedUserInputNode);
        }
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(InputEnumAST input, ASTNode parent) {
        pushToStack(input);
        boolean result = resolve(input);
        if (!result) {
            InputNodeAST unresolvedUserInputNode = userInputVisitor.visit(input, parent);
            processUnresolvedInput(input, unresolvedUserInputNode);
        }
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(InputListAST input, ASTNode parent) {
        pushToStack(input);
        boolean result = resolve(input);
        if (!result) {
            InputNodeAST unresolvedUserInputNode = userInputVisitor.visit(input, parent);
            processUnresolvedInput(input, unresolvedUserInputNode);
        }
        acceptAll(input);
        stack.pop();
    }

    private void processUnresolvedInput(InputNodeAST input, InputNodeAST unresolvedUserInputNode) {
        UserInputAST unresolvedInput = UserInputAST.create(input, getParentStep(input));
        unresolvedInput.children().add(unresolvedUserInputNode);
        unresolvedInputs.add(unresolvedInput);
        throw new WaitForUserInput();
    }

    private StepAST getParentStep(ASTNode node) {
        if (node.parent() == null) {
            return (StepAST) node;
        }
        if (node.parent() instanceof StepAST) {
            return (StepAST) node.parent();
        }
        return getParentStep(node.parent());
    }

    @Override
    public void visit(InputTextAST input, ASTNode parent) {

    }

    @Override
    public void visit(ExecAST exec, ASTNode parent) {
        ArchetypeDescriptor descriptor = archetype.getDescriptor(resolveScriptPath(exec.currentDirectory(), exec.src()));
        //todo maybe need to change dir
        XmlDescriptor xmlDescriptor = XmlDescriptor.create(descriptor, exec, exec.currentDirectory());//exec.currentDirectory()
        pushToStack(exec);
        xmlDescriptor.accept(this, exec);
        exec.help(xmlDescriptor.help());
        exec.children().addAll(xmlDescriptor.children());
        stack.pop();
    }

    @Override
    public void visit(SourceAST source, ASTNode parent) {
        ArchetypeDescriptor descriptor = archetype.getDescriptor(resolveScriptPath(source.currentDirectory(), source.source()));
        //todo maybe need to change dir
        String currentDir = Paths.get(resolveScriptPath(source.currentDirectory(), source.source())).getParent().toString();
        XmlDescriptor xmlDescriptor = XmlDescriptor.create(descriptor, source, currentDir);//source.currentDirectory()
        pushToStack(source);
        xmlDescriptor.accept(this, source);
        source.help(xmlDescriptor.help());
        source.children().addAll(xmlDescriptor.children());
        stack.pop();
    }

    private String resolveScriptPath(String currentDirectory, String scriptSrc) {
        return Paths.get(currentDirectory).resolve(scriptSrc).normalize().toString();
    }

    @Override
    public void visit(ContextAST context, ASTNode parent) {
        if (context == null) {
            return;
        }
        cleanUnresolvedInputs(context);
        acceptAll(context);
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
    public void visit(OptionAST input, ASTNode parent) {
        if (input != stack.peek()) {
            stack.push(input);
        }
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(OutputAST output, ASTNode parent) {
    }

    @Override
    public void visit(TransformationAST transformation, ASTNode parent) {
    }

    @Override
    public void visit(FileSetsAST fileSets, ASTNode parent) {
    }

    @Override
    public void visit(FileSetAST fileSet, ASTNode parent) {
    }

    @Override
    public void visit(TemplateAST template, ASTNode parent) {
    }

    @Override
    public void visit(TemplatesAST templates, ASTNode parent) {
    }

    @Override
    public void visit(ModelAST model, ASTNode parent) {
    }

    @Override
    public void visit(IfStatement input, ASTNode parent) {
        if (input != stack.peek()) {
            stack.push(input);
        }
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(ModelKeyValueAST value, ASTNode parent) {
    }

    @Override
    public void visit(ValueTypeAST value, ASTNode parent) {
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

    private void acceptAll(ASTNode node) {
        while (node.hasNext()) {
            Visitable visitable = node.next();
            visitable.accept(this, node);
        }
    }

    private void acceptAll(ASTNode node, ASTNode parent) {
        while (node.hasNext()) {
            Visitable visitable = node.next();
            visitable.accept(this, parent);
        }
    }

    private void pushToStack(ASTNode node) {
        if (node != stack.peek()) {
            stack.push(node);
        }
    }

    private boolean resolve(InputNodeAST input) {
        ContextNodeAST contextNodeAST = pathToContextNodeMap.get(input.path());
        if (contextNodeAST != null) {
            input.accept(inputResolverVisitor, contextNodeAST);
        }
        return contextNodeAST != null;
    }

    //todo change implementation - use context
    private void cleanUnresolvedInputs(ContextAST context) {
        unresolvedInputs.clear();
    }
}
