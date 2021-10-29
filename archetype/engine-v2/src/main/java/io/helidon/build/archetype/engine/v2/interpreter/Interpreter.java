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

import java.nio.file.Path;
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
import io.helidon.build.common.PropertyEvaluator;

/**
 * Interpret user inputs and produce new steps.
 */
public class Interpreter implements Visitor<ASTNode> {

    private final Archetype archetype;
    private final Map<String, ContextNodeAST> pathToContextNodeMap;
    private final InputResolverVisitor inputResolverVisitor = new InputResolverVisitor();
    private final UserInputVisitor userInputVisitor = new UserInputVisitor();
    private final ContextToStringConvertor contextToStringConvertor = new ContextToStringConvertor();
    private final ContextNodeFromDefaultValueCreator contextNodeFromDefaultValueCreator =
            new ContextNodeFromDefaultValueCreator();
    private final List<UserInputAST> unresolvedInputs = new ArrayList<>();
    private final Deque<ASTNode> stack = new ArrayDeque<>();
    private final List<Visitor<ASTNode>> additionalVisitors;
    private final Map<String, ScriptAST> pathToSourceNode = new HashMap<>();
    private final String startDescriptorPath;
    private final Map<String, String> externalDefaults;
    private boolean skipOptional;
    private boolean canBeGenerated = false;

    Interpreter(Archetype archetype,
                String startDescriptorPath,
                boolean skipOptional,
                List<Visitor<ASTNode>> additionalVisitors,
                Map<String, String> externalDefaults) {
        this.archetype = archetype;
        pathToContextNodeMap = new HashMap<>();
        this.additionalVisitors = additionalVisitors;
        this.startDescriptorPath = startDescriptorPath;
        this.externalDefaults = externalDefaults;
        this.skipOptional = skipOptional;
    }

    boolean canBeGenerated() {
        return canBeGenerated;
    }

    void skipOptional(boolean skipOptional) {
        this.skipOptional = skipOptional;
    }

    Map<String, ContextNodeAST> pathToContextNodeMap() {
        return pathToContextNodeMap;
    }

    Queue<ASTNode> stack() {
        return stack;
    }

    List<UserInputAST> unresolvedInputs() {
        return unresolvedInputs;
    }

    @Override
    public void visit(XmlDescriptor xmlDescriptor, ASTNode parent) {
        pushToStack(xmlDescriptor);
        acceptAll(xmlDescriptor, parent);
        stack.pop();
    }

    @Override
    public void visit(Visitable input, ASTNode arg) {
        //class do not process other types of the nodes
    }

    @Override
    public void visit(StepAST input, ASTNode parent) {
        applyAdditionalVisitors(input);
        pushToStack(input);
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(InputAST input, ASTNode parent) {
        applyAdditionalVisitors(input);
        pushToStack(input);
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(InputBooleanAST input, ASTNode parent) {
        applyAdditionalVisitors(input);
        replaceDefaultValue(input);
        validate(input);
        pushToStack(input);
        boolean result = resolve(input);
        if (!result) {
            InputNodeAST unresolvedUserInputNode = userInputVisitor.visit(input, parent);
            processUnresolvedInput(input, unresolvedUserInputNode);
        } else {
            if (((ContextBooleanAST) getContextNode(input)).bool()) {
                acceptAll(input);
            } else {
                input.children().clear();
            }
        }
        stack.pop();
    }

    @Override
    public void visit(InputEnumAST input, ASTNode parent) {
        applyAdditionalVisitors(input);
        replaceDefaultValue(input);
        validate(input);
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
        applyAdditionalVisitors(input);
        replaceDefaultValue(input);
        validate(input);
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
    public void visit(InputTextAST input, ASTNode parent) {
        applyAdditionalVisitors(input);
        replaceDefaultValue(input);
        validate(input);
        pushToStack(input);
        boolean result = resolve(input);
        if (!result) {
            InputNodeAST unresolvedUserInputNode = userInputVisitor.visit(input, parent);
            processUnresolvedInput(input, unresolvedUserInputNode);
        }
        stack.pop();
    }

    @Override
    public void visit(ExecAST exec, ASTNode parent) {
        applyAdditionalVisitors(exec);
        if (pushToStack(exec)) {
            String scriptDirectory = exec.location().scriptDirectory();
            String scriptFile = exec.source();
            String descriptorPath = resolveScriptPath(scriptDirectory, scriptFile);
            ArchetypeDescriptor descriptor = getDescriptor(exec, descriptorPath);
            Path path = Paths.get(descriptorPath);
            String currentDir = path.getParent().toString();
            ASTNode.Location newLocation = ASTNode.Location.builder()
                                                           .scriptFile(descriptorPath)
                                                           .scriptDirectory(currentDir)
                                                           .currentDirectory(currentDir)
                                                           .build();
            setParent(exec, parent);
            XmlDescriptor xmlDescriptor = XmlDescriptor.create(descriptor, exec, newLocation);
            exec.help(xmlDescriptor.help());
            exec.children().addAll(xmlDescriptor.children());
            xmlDescriptor.accept(this, exec);
        }
        stack.pop();
    }

    @Override
    public void visit(SourceAST source, ASTNode parent) {
        applyAdditionalVisitors(source);
        if (pushToStack((source))) {
            String scriptDirectory = source.location().scriptDirectory();
            String scriptFile = source.source();
            String descriptorPath = resolveScriptPath(scriptDirectory, scriptFile);
            ArchetypeDescriptor descriptor = getDescriptor(source, descriptorPath);
            String currentDir = Paths.get(descriptorPath).getParent().toString();
            ASTNode.Location newLocation = ASTNode.Location.builder()
                                                           .scriptFile(descriptorPath)
                                                           .scriptDirectory(currentDir)
                                                           .currentDirectory(source.location().currentDirectory())
                                                           .build();
            setParent(source, parent);
            XmlDescriptor xmlDescriptor = XmlDescriptor.create(descriptor, source, newLocation);
            source.help(xmlDescriptor.help());
            source.children().addAll(xmlDescriptor.children());
            xmlDescriptor.accept(this, source);
        }
        stack.pop();
    }

    private ArchetypeDescriptor getDescriptor(ScriptAST script, String descriptorPath) {
        // Check for duplicate script includes
        ScriptAST existing = pathToSourceNode.putIfAbsent(descriptorPath, script);
        if (existing == null) {
            return archetype.getDescriptor(descriptorPath);
        } else {
            String existingPath = existing.location().scriptPath();
            String existingType = existing.includeType();
            String scriptPath = script.location().scriptPath();
            String scriptType = script.includeType();
            String error = isCycle(script, descriptorPath) ? "Include cycle" : "Duplicate include";
            throw new IllegalStateException(error + ": '" + descriptorPath
                                            + "' is included via <" + existingType
                                            + "> in '" + existingPath + "' and again via <"
                                            + scriptType
                                            + "> in '" + scriptPath + "'");
        }
    }

    private static boolean isCycle(ScriptAST duplicate, String descriptorPath) {
        ASTNode parent = duplicate.parent();
        while (parent != null) {
            if (parent instanceof ScriptAST) {
                ScriptAST scriptParent = (ScriptAST) parent;
                if (scriptParent.location().scriptPath().equals(descriptorPath)) {
                    return true;
                }
            }
            parent = parent.parent();
        }
        return false;
    }

    private static void setParent(ASTNode node, ASTNode parent) {
        if (node.parent() == null && parent != null) {
            node.parent(parent);
        }
    }

    @Override
    public void visit(ContextAST context, ASTNode parent) {
        setParent(context, parent);
        applyAdditionalVisitors(context);
        cleanUnresolvedInputs(context);
        acceptAll(context);
    }

    @Override
    public void visit(ContextBooleanAST contextBoolean, ASTNode parent) {
        setParent(contextBoolean, parent);
        applyAdditionalVisitors(contextBoolean);
        pathToContextNodeMap.putIfAbsent(contextBoolean.path(), contextBoolean);
    }

    @Override
    public void visit(ContextEnumAST contextEnum, ASTNode parent) {
        setParent(contextEnum, parent);
        applyAdditionalVisitors(contextEnum);
        pathToContextNodeMap.putIfAbsent(contextEnum.path(), contextEnum);
    }

    @Override
    public void visit(ContextListAST contextList, ASTNode parent) {
        setParent(contextList, parent);
        applyAdditionalVisitors(contextList);
        pathToContextNodeMap.putIfAbsent(contextList.path(), contextList);
    }

    @Override
    public void visit(ContextTextAST contextText, ASTNode parent) {
        setParent(contextText, parent);
        applyAdditionalVisitors(contextText);
        pathToContextNodeMap.putIfAbsent(contextText.path(), contextText);
    }

    @Override
    public void visit(OptionAST input, ASTNode parent) {
        applyAdditionalVisitors(input);
        pushToStack(input);
        acceptAll(input);
        stack.pop();
    }

    @Override
    public void visit(OutputAST output, ASTNode parent) {
        applyAdditionalVisitors(output);
        pushToStack(output);
        acceptAll(output);
        stack.pop();
    }

    @Override
    public void visit(TransformationAST transformation, ASTNode parent) {
        applyAdditionalVisitors(transformation);
    }

    @Override
    public void visit(FileSetsAST fileSets, ASTNode parent) {
        applyAdditionalVisitors(fileSets);
    }

    @Override
    public void visit(FileSetAST fileSet, ASTNode parent) {
        applyAdditionalVisitors(fileSet);
    }

    @Override
    public void visit(TemplateAST template, ASTNode parent) {
        applyAdditionalVisitors(template);
        pushToStack(template);
        acceptAll(template);
        stack.pop();
    }

    @Override
    public void visit(TemplatesAST templates, ASTNode parent) {
        applyAdditionalVisitors(templates);
        pushToStack(templates);
        acceptAll(templates);
        stack.pop();
    }

    @Override
    public void visit(ModelAST model, ASTNode parent) {
        applyAdditionalVisitors(model);
        pushToStack(model);
        acceptAll(model);
        stack.pop();
    }

    @Override
    public void visit(IfStatement input, ASTNode parent) {
        applyAdditionalVisitors(input);
        pushToStack(input);
        Map<String, String> contextValuesMap = convertContext();
        if (input.expression().evaluate(contextValuesMap)) {
            acceptAll(input);
        } else {
            input.children().clear();
        }
        stack.pop();
    }

    @Override
    public void visit(ModelKeyValueAST value, ASTNode parent) {
        applyAdditionalVisitors(value);
        pushToStack(value);
        acceptAll(value);
        stack.pop();
    }

    @Override
    public void visit(ValueTypeAST value, ASTNode parent) {
        applyAdditionalVisitors(value);
        pushToStack(value);
        acceptAll(value);
        stack.pop();
    }

    @Override
    public void visit(ModelKeyListAST list, ASTNode parent) {
        applyAdditionalVisitors(list);
        pushToStack(list);
        acceptAll(list);
        stack.pop();
    }

    @Override
    public void visit(MapTypeAST map, ASTNode parent) {
        applyAdditionalVisitors(map);
        pushToStack(map);
        acceptAll(map);
        stack.pop();
    }

    @Override
    public void visit(ListTypeAST list, ASTNode parent) {
        applyAdditionalVisitors(list);
        pushToStack(list);
        acceptAll(list);
        stack.pop();
    }

    @Override
    public void visit(ModelKeyMapAST map, ASTNode parent) {
        applyAdditionalVisitors(map);
        pushToStack(map);
        acceptAll(map);
        stack.pop();
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

    private boolean pushToStack(ASTNode node) {
        if (node != stack.peek()) {
            stack.push(node);
            return true;
        }
        return false;
    }

    private boolean resolve(InputNodeAST input) {
        ContextNodeAST contextNodeAST = pathToContextNodeMap.get(input.path());
        if (input.isOptional() && skipOptional && contextNodeAST == null) {
            contextNodeAST = input.accept(contextNodeFromDefaultValueCreator, input);
            if (contextNodeAST != null) {
                pathToContextNodeMap.put(contextNodeAST.path(), contextNodeAST);
            }
        }
        if (contextNodeAST != null) {
            input.accept(inputResolverVisitor, contextNodeAST);
        }
        return contextNodeAST != null;
    }

    private ContextNodeAST getContextNode(InputNodeAST input) {
        return pathToContextNodeMap.get(input.path());
    }

    /**
     * Create unresolvedInput, add it to the {@code unresolvedInputs} and throw {@link WaitForUserInput} to stop the
     * interpreting process.
     *
     * @param input initial unresolved {@code InputNodeAST} from the AST tree.
     * @param unresolvedUserInputNode unresolvedUserInputNode that will be sent to the user
     */
    private void processUnresolvedInput(InputNodeAST input, InputNodeAST unresolvedUserInputNode) {
        UserInputAST unresolvedInput = UserInputAST.create(input, getParentStep(input));
        unresolvedInput.children().add(unresolvedUserInputNode);
        unresolvedInputs.add(unresolvedInput);
        if (input.isOptional()) {
            updateCanBeGenerated();
        }
        throw new WaitForUserInput();
    }

    private void updateCanBeGenerated() {
        if (canBeGenerated) {
            return;
        }
        Flow flow = new Flow(archetype, startDescriptorPath, true, List.of(), Map.of());
        ContextAST context = new ContextAST();
        context.children().addAll(pathToContextNodeMap.values());
        FlowState state = flow.build(context);
        if (state.type() == FlowStateEnum.READY) {
            canBeGenerated = true;
        }
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

    private void cleanUnresolvedInputs(ContextAST context) {
        unresolvedInputs.removeIf(input -> context
                .children().stream()
                .anyMatch(contextNote -> ((ContextNodeAST) contextNote).path().equals(input.path())));
    }

    private String resolveScriptPath(String currentDirectory, String scriptSrc) {
        return Paths.get(currentDirectory).resolve(scriptSrc).normalize().toString();
    }

    private void validate(InputNodeAST input) {
        if (input.isOptional() && input.defaultValue() == null) {
            throw new InterpreterException(
                    String.format("Input node %s is optional but it does not have a default value", input.path()));
        }
    }

    private void applyAdditionalVisitors(ASTNode node) {
        additionalVisitors.forEach(visitor -> node.accept(visitor, null));
    }

    private Map<String, String> convertContext() {
        Map<String, String> result = new HashMap<>();
        pathToContextNodeMap.forEach((key, value) -> result.putIfAbsent(key, value.accept(contextToStringConvertor, null)));
        return result;
    }

    private void replaceDefaultValue(InputNodeAST input) {
        input.defaultValue(replaceDefaultValue(input.path(), input.defaultValue()));
    }

    private String replaceDefaultValue(String path, String defaultValue) {
        String externalDefault = externalDefaults.get(path);
        if (externalDefault == null) {
            if (defaultValue == null) {
                return null;
            }
        } else {
            defaultValue = externalDefault;
        }
        if (!defaultValue.contains("${")) {
            return defaultValue;
        }
        Map<String, String> properties = convertContext();
        properties.replaceAll((key, value) -> value.replaceAll("[\\['\\]]", ""));
        return PropertyEvaluator.resolve(defaultValue, properties);
    }
}
