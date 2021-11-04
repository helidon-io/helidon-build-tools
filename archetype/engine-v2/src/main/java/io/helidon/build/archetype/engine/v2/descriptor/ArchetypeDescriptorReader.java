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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.common.xml.SimpleXMLParser;

/**
 * {@link ArchetypeDescriptor} reader.
 */
public class ArchetypeDescriptorReader implements SimpleXMLParser.Reader {

    private final LinkedList<String> stack;
    private final LinkedList<Object> objectTracking;

    private Option currentOption;
    private Step currentStep;
    private Output currentOutput;

    private boolean topLevelOutput = false;

    private final Map<String, String> archetypeAttributes = new HashMap<>();
    private final LinkedList<Context> context = new LinkedList<>();
    private final LinkedList<Step> steps = new LinkedList<>();
    private final LinkedList<Input> inputs = new LinkedList<>();
    private final LinkedList<Source> source = new LinkedList<>();
    private final LinkedList<Exec> exec = new LinkedList<>();
    private Output output = null;
    private String help = null;

    private ArchetypeDescriptorReader() {
        stack = new LinkedList<>();
        objectTracking = new LinkedList<>();
    }

    /**
     * Read the descriptor from the given input stream.
     *
     *
     * @param archetypePath path to the archetype.
     * @param descriptorPath path to descriptor.
     * @param is input stream
     * @return descriptor, never {@code null}
     */
    static ArchetypeDescriptor read(Path archetypePath, Path descriptorPath, InputStream is) {
        try {
            ArchetypeDescriptorReader reader = new ArchetypeDescriptorReader();
            SimpleXMLParser.parse(is, reader);
            return new ArchetypeDescriptor(archetypePath, descriptorPath, reader.archetypeAttributes, reader.context,
                                           reader.steps, reader.inputs, reader.source, reader.exec, reader.output, reader.help);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public void startElement(String qName, Map<String, String> attributes) {
        String parent = stack.peek();
        if (parent == null) {
            if (!"archetype-script".equals(qName)) {
                throw new IllegalStateException("Invalid root element '" + qName + "'");
            }
            archetypeAttributes.putAll(attributes);
            stack.push("archetype-script");
        } else {
            switch (parent) {
                case "archetype-script":
                    switch (qName) {
                        case "exec":
                            exec.add(new Exec(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(exec.getLast());
                            stack.push(qName);
                            break;
                        case "source":
                            source.add(new Source(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(source.getLast());
                            stack.push("source");
                            break;
                        case "step":
                            steps.add(new Step(attributes.get("label"), attributes.get("if")));
                            currentStep = steps.getLast();
                            objectTracking.add(steps.getLast());
                            stack.push(qName);
                            break;
                        case "output":
                            topLevelOutput = true;
                            currentOutput = new Output(attributes.get("if"));
                            objectTracking.add(currentOutput);
                            stack.push(qName);
                            break;
                        case "context":
                            context.add(new Context());
                            objectTracking.add(context.getLast());
                            stack.push(qName);
                            break;
                        case "input":
                            inputs.add(new Input());
                            objectTracking.add(inputs.getLast());
                            stack.push(qName);
                            break;
                        case "help":
                            objectTracking.add("help");
                            stack.push(qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid top-level element: " + qName);
                    }
                    break;
                case "exec":
                case "source":
                    break;
                case "context/list":
                    validateChild("value", "context/list", qName);
                    objectTracking.add(parent + "/value");
                    stack.push(parent + "/value");
                    break;
                case "context/enum":
                    validateChild("value", "context/enum", qName);
                    objectTracking.add(parent + "/value");
                    stack.push(parent + "/value");
                    break;
                case "step":
                    switch (qName) {
                        case "context":
                            currentStep.contexts().add(new Context());
                            objectTracking.add(currentStep.contexts().getLast());
                            stack.push("context");
                            break;
                        case "exec":
                            currentStep.execs().add(new Exec(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(currentStep.execs().getLast());
                            stack.push("step/" + qName);
                            break;
                        case "source":
                            currentStep.sources().add(new Source(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(currentStep.sources().getLast());
                            stack.push("step/" + qName);
                            break;
                        case "input":
                            currentStep.inputs().add(new Input());
                            objectTracking.add(currentStep.inputs().getLast());
                            stack.push("input");
                            break;
                        case "help":
                            objectTracking.add("help");
                            stack.push("step/help");
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + "with parent: " + parent);
                    }
                    break;
                case "context":
                    switch (qName) {
                        case "boolean":
                            ((Context) objectTracking.getLast()).nodes().add(new ContextBoolean(attributes.get("path")));
                            break;
                        case "list":
                            ((Context) objectTracking.getLast()).nodes().add(new ContextList(attributes.get("path")));
                            break;
                        case "enum":
                            ((Context) objectTracking.getLast()).nodes().add(new ContextEnum(attributes.get("path")));
                            break;
                        case "text":
                            ((Context) objectTracking.getLast()).nodes().add(new ContextText(attributes.get("path")));
                            break;
                        default:
                            throw new IllegalStateException("Invalid Context child element: " +  qName);
                    }
                    objectTracking.add("context/" + qName);
                    stack.push("context/" + qName);
                    break;
                case "input":
                    if (!(objectTracking.getLast() instanceof Input)) {
                        throw new IllegalStateException("Invalid object stack element for Input");
                    }
                    Input currentInput = (Input) objectTracking.getLast();
                    switch (qName) {
                        case "text":
                            addInputText(currentInput, attributes);
                            objectTracking.add(currentInput.nodes().getLast());
                            stack.push("input/text");
                            break;
                        case "boolean":
                            currentInput.nodes().add(new InputBoolean(
                                    attributes.get("label"),
                                    attributes.get("name"),
                                    attributes.get("default"),
                                    attributes.get("prompt"),
                                    parseBoolean(attributes.get("optional") == null ? "false" : attributes.get("optional"))
                            ));
                            objectTracking.add(currentInput.nodes().getLast());
                            stack.push("input/boolean");
                            break;
                        case "enum":
                            currentInput.nodes().add(new InputEnum(
                                    attributes.get("label"),
                                    attributes.get("name"),
                                    attributes.get("default"),
                                    attributes.get("prompt"),
                                    parseBoolean(attributes.get("optional") == null ? "false" : attributes.get("optional"))
                            ));
                            objectTracking.add(currentInput.nodes().getLast());
                            stack.push("input/enum");
                            break;
                        case "list":
                            currentInput.nodes().add(new InputList(
                                    attributes.get("label"),
                                    attributes.get("name"),
                                    attributes.get("default"),
                                    attributes.get("prompt"),
                                    parseBoolean(attributes.get("optional") == null ? "false" : attributes.get("optional")),
                                    attributes.get("min"),
                                    attributes.get("max"),
                                    attributes.get("help")
                            ));
                            objectTracking.add(currentInput.nodes().getLast());
                            stack.push("input/list");
                            break;
                        case "output":
                            currentInput.output(new Output(attributes.get("if")));
                            currentOutput = currentInput.output();
                            objectTracking.add(currentInput.output());
                            stack.push("output");
                            break;
                        case "context":
                            currentInput.contexts().add(new Context());
                            objectTracking.add(currentInput.contexts().getLast());
                            stack.push("context");
                            break;
                        case "exec":
                            currentInput.execs().add(new Exec(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(currentInput.execs().getLast());
                            stack.push("exec");
                            break;
                        case "source":
                            currentInput.sources().add(new Source(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(currentInput.sources().getLast());
                            stack.push("source");
                            break;
                        case "input":
                            currentInput.inputs().add(new Input());
                            currentInput = currentInput.inputs().getLast();
                            objectTracking.add(currentInput.inputs().getLast());
                            stack.push("input");
                            break;
                        case "step":
                            currentInput.steps().add(new Step(attributes.get("label"), attributes.get("if")));
                            currentStep = currentInput.steps().getLast();
                            objectTracking.add(currentInput.steps().getLast());
                            stack.push("step");
                            break;
                        default:
                            throw new IllegalStateException("Invalid Input child element: " +  qName);
                    }
                    break;
                case "input/boolean":
                    if (!(objectTracking.getLast() instanceof InputBoolean)) {
                        throw new IllegalStateException("Invalid object stack element for Boolean");
                    }
                    InputBoolean tempBool = (InputBoolean) objectTracking.getLast();
                    switch (qName) {
                        case "context":
                            tempBool.contexts().add(new Context());
                            objectTracking.add(tempBool.contexts().getLast());
                            stack.push("context");
                            break;
                        case "exec":
                            tempBool.execs().add(new Exec(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(tempBool.execs().getLast());
                            stack.push("exec");
                            break;
                        case "source":
                            tempBool.sources().add(new Source(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(tempBool.sources().getLast());
                            stack.push("source");
                            break;
                        case "input":
                            Input input = new Input();
                            tempBool.inputs().add(input);
                            objectTracking.add(input);
                            stack.push("input");
                            break;
                        case "step":
                            tempBool.steps().add(new Step(attributes.get("label"), attributes.get("if")));
                            currentStep = tempBool.steps().getLast();
                            objectTracking.add(tempBool.steps().getLast());
                            stack.push("step");
                            break;
                        case "output":
                            tempBool.output(new Output(attributes.get("if")));
                            currentOutput = tempBool.output();
                            objectTracking.add(tempBool.output());
                            stack.push("output");
                            break;
                        case "help":
                            objectTracking.add(objectTracking.getLast());
                            stack.push("input/boolean/help");
                            break;
                        default:
                            throw new IllegalStateException("Invalid Context child element: " +  qName);
                    }
                    break;
                case "input/enum":
                    if (!(objectTracking.getLast() instanceof InputEnum)) {
                        throw new IllegalStateException("Invalid object stack element for Enum");
                    }
                    InputEnum tempEnum = (InputEnum) objectTracking.getLast();
                    switch (qName) {
                        case "context":
                            tempEnum.contexts().add(new Context());
                            objectTracking.add(tempEnum.contexts().getLast());
                            stack.push("context");
                            break;
                        case "exec":
                            tempEnum.execs().add(new Exec(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(tempEnum.execs().getLast());
                            stack.push("exec");
                            break;
                        case "source":
                            tempEnum.sources().add(new Source(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(tempEnum.sources().getLast());
                            stack.push("source");
                            break;
                        case "input":
                            tempEnum.inputs().add(new Input());
                            objectTracking.add(tempEnum.inputs().getLast());
                            stack.push("input");
                            break;
                        case "step":
                            tempEnum.steps().add(new Step(attributes.get("label"), attributes.get("if")));
                            currentStep = tempEnum.steps().getLast();
                            objectTracking.add(tempEnum.steps().getLast());
                            stack.push("step");
                            break;
                        case "output":
                            tempEnum.output(new Output(attributes.get("if")));
                            currentOutput = tempEnum.output();
                            objectTracking.add(tempEnum.output());
                            stack.push("output");
                            break;
                        case "help":
                            objectTracking.add("help");
                            stack.push("input/enum/help");
                            break;
                        case "option":
                            tempEnum.options().add(new Option(attributes.get("label"), attributes.get("value")));
                            currentOption = tempEnum.options().getLast();
                            objectTracking.add(tempEnum.options().getLast());
                            stack.push("option");
                            break;
                        default:
                            throw new IllegalStateException("Invalid Context child element: " +  qName);
                    }
                    break;
                case "input/list":
                    if (!(objectTracking.getLast() instanceof InputList)) {
                        throw new IllegalStateException("Invalid object stack element for List");
                    }
                    InputList tempList = (InputList) objectTracking.getLast();
                    switch (qName) {
                        case "context":
                            tempList.contexts().add(new Context());
                            objectTracking.add(tempList.contexts().getLast());
                            stack.push("context");
                            break;
                        case "exec":
                            tempList.execs().add(new Exec(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(tempList.execs().getLast());
                            stack.push("exec");
                            break;
                        case "source":
                            tempList.sources().add(new Source(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(tempList.sources().getLast());
                            stack.push("source");
                            break;
                        case "input":
                            tempList.inputs().add(new Input());
                            objectTracking.add(tempList.inputs().getLast());
                            stack.push("input");
                            break;
                        case "step":
                            tempList.steps().add(new Step(attributes.get("label"), attributes.get("if")));
                            currentStep = tempList.steps().getLast();
                            objectTracking.add(tempList.steps().getLast());
                            stack.push("step");
                            break;
                        case "output":
                            tempList.output(new Output(attributes.get("if")));
                            currentOutput = tempList.output();
                            objectTracking.add(tempList.output());
                            stack.push("output");
                            break;
                        case "help":
                            objectTracking.add(objectTracking.getLast());
                            stack.push("input/list/help");
                            break;
                        case "option":
                            tempList.options().add(new Option(attributes.get("label"), attributes.get("value")));
                            currentOption = tempList.options().getLast();
                            objectTracking.add(tempList.options().getLast());
                            stack.push("option");
                            break;
                        default:
                            throw new IllegalStateException("Invalid Context child element: " +  qName);
                    }
                    break;
                case "option":
                    switch (qName) {
                        case "context":
                            currentOption.contexts().add(new Context());
                            objectTracking.add(currentOption.contexts().getLast());
                            stack.push("context");
                            break;
                        case "exec":
                            currentOption.execs().add(new Exec(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(currentOption.execs().getLast());
                            stack.push("exec");
                            break;
                        case "source":
                            currentOption.sources().add(new Source(attributes.get("url"), attributes.get("src")));
                            objectTracking.add(currentOption.sources().getLast());
                            stack.push("source");
                            break;
                        case "input":
                            currentOption.inputs().add(new Input());
                            objectTracking.add(currentOption.inputs().getLast());
                            stack.push("input");
                            break;
                        case "step":
                            currentOption.steps().add(new Step(attributes.get("label"), attributes.get("if")));
                            currentStep = currentOption.steps().getLast();
                            objectTracking.add(currentOption.steps().getLast());
                            stack.push("step");
                            break;
                        case "output":
                            currentOption.output(new Output(attributes.get("if")));
                            currentOutput = currentOption.output();
                            objectTracking.add(currentOption.output());
                            stack.push("output");
                            break;
                        case "help":
                            objectTracking.add(objectTracking.getLast());
                            stack.push("input/option/help");
                            break;
                        default:
                            throw new IllegalStateException("Invalid option child element: " +  qName);
                    }
                    break;
                case "output":
                    switch (qName) {
                        case "transformation":
                            currentOutput.transformations().add(new Transformation(attributes.get("id")));
                            objectTracking.add(currentOutput.transformations().getLast());
                            stack.push("output/transformation");
                            break;
                        case "file":
                            currentOutput.fileList().add(new FileSet(
                                    readRequiredAttribute("source", qName, attributes),
                                    readRequiredAttribute("target", qName, attributes),
                                    attributes.get("if")));
                            objectTracking.add(currentOutput.fileList().getLast());
                            stack.push("output/file");
                            break;
                        case "files":
                            currentOutput.filesList().add(new FileSets(
                                    attributes.get("transformations"),
                                    attributes.get("if")));
                            objectTracking.add(currentOutput.filesList().getLast());
                            stack.push("output/files");
                            break;
                        case "template":
                            currentOutput.template().add(new Template(
                                    attributes.get("engine"),
                                    attributes.get("source"),
                                    attributes.get("target"),
                                    attributes.get("if")
                            ));
                            objectTracking.add(currentOutput.template().getLast());
                            stack.push("output/template");
                            break;
                        case "templates":
                            currentOutput.templates().add(new Templates(
                                    attributes.get("engine"),
                                    attributes.get("transformations"),
                                    attributes.get("if")
                            ));
                            objectTracking.add(currentOutput.templates().getLast());
                            stack.push("output/templates");
                            break;
                        case "model":
                            currentOutput.model(new Model(attributes.get("if")));
                            objectTracking.add(currentOutput.model());
                            stack.push("model");
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + " with parent: " + parent);
                    }
                    break;
                case "output/files":
                    validateChilds(qName, parent, "directory", "excludes", "includes");
                    objectTracking.add(currentOutput.filesList().getLast());
                    stack.push("output/files/" + qName);
                    break;
                case "output/files/includes":
                    validateChild("include", parent, qName);
                    objectTracking.add("output/files/includes/" + qName);
                    stack.push("output/files/includes/" + qName);
                    break;
                case "output/files/excludes":
                    validateChild("exclude", parent, qName);
                    objectTracking.add("output/files/excludes/" + qName);
                    stack.push("output/files/excludes/" + qName);
                    break;
                case "output/transformation":
                    validateChild("replace", parent, qName);
                    currentOutput.transformations().getLast().replacements().add(new Replacement(
                            readRequiredAttribute("regex", qName, attributes),
                            readRequiredAttribute("replacement", qName, attributes)));
                    objectTracking.add(currentOutput.transformations().getLast().replacements().getLast());
                    stack.push("output/transformation/replace");
                    break;
                case "output/template":
                    validateChild("model", parent, qName);
                    currentOutput.template().getLast().model(new Model(attributes.get("if")));
                    objectTracking.add(currentOutput.template().getLast().model());
                    stack.push("model");
                    break;
                case "output/templates":
                    switch (qName) {
                        case "model":
                            currentOutput.templates().getLast().model(new Model(attributes.get("if")));
                            objectTracking.add(currentOutput.templates().getLast().model());
                            stack.push("model");
                            break;
                        case "directory":
                        case "includes":
                        case "excludes":
                            objectTracking.add(currentOutput.templates().getLast());
                            stack.push("output/templates/" + qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + "with parent: " + parent);
                    }
                    break;
                case "output/templates/includes":
                    validateChild("include", parent, qName);
                    objectTracking.add(parent + "/" + qName);
                    stack.push(parent + "/" + qName);
                    break;
                case "output/templates/excludes":
                    validateChild("exclude", parent, qName);
                    objectTracking.add(parent + "/" + qName);
                    stack.push(parent + "/" + qName);
                    break;
                case "model":
                    if (!(objectTracking.getLast() instanceof  Model)) {
                        throw new IllegalStateException("Invalid object stack element for Model");
                    }
                    Model model = (Model) objectTracking.getLast();
                    switch (qName) {
                        case "value":
                            addModelKeyValue(model, parent, attributes);
                            objectTracking.add(model.keyValues().getLast());
                            stack.push(qName);
                            break;
                        case "list":
                            model.keyLists().add(new ModelKeyList(
                                    readRequiredAttribute("key", qName, attributes),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(model.keyLists().getLast());
                            stack.push("model/" + qName);
                            break;
                        case "map":
                            model.keyMaps().add(new ModelKeyMap(
                                    readRequiredAttribute("key", qName, attributes),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(model.keyMaps().getLast());
                            stack.push("model/" + qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + "with parent: " + parent);
                    }
                    break;
                case "model/list":
                    if (!(objectTracking.getLast() instanceof ModelKeyList)) {
                        throw new ClassCastException("wrong object in stack");
                    }
                    ModelKeyList keyListML = (ModelKeyList) objectTracking.getLast();
                    switch (qName) {
                        case "value":
                            keyListML.values().add(new ValueType(
                                    attributes.get("url"),
                                    attributes.get("file"),
                                    attributes.get("template"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(keyListML.values().getLast());
                            stack.push(qName);
                            break;
                        case "list":
                            keyListML.lists().add(new ListType(
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(keyListML.lists().getLast());
                            stack.push("model/list/" + qName);
                            break;
                        case "map":
                            keyListML.maps().add(new MapType(
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(keyListML.maps().getLast());
                            stack.push("model/list/" + qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + "with parent: " + parent);
                    }
                    break;
                case "model/list/list":
                    if (!(objectTracking.getLast() instanceof ListType)) {
                        throw new ClassCastException("wrong object in stack");
                    }
                    ListType listLL = (ListType) objectTracking.getLast();
                    switch (qName) {
                        case "value":
                            listLL.values().add(new ValueType(
                                    attributes.get("url"),
                                    attributes.get("file"),
                                    attributes.get("template"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(listLL.values().getLast());
                            stack.push(qName);
                            break;
                        case "list":
                            listLL.lists().add(new ListType(
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(listLL.lists().getLast());
                            stack.push("model/list/list");
                            break;
                        case "map":
                            listLL.maps().add(new MapType(
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(listLL.maps().getLast());
                            stack.push("model/list/map");
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + "with parent: " + parent);
                    }
                    break;
                case "model/list/map":
                    if (!(objectTracking.getLast() instanceof MapType)) {
                        throw new ClassCastException("wrong object in stack");
                    }
                    MapType mapLM = (MapType) objectTracking.getLast();
                    switch (qName) {
                        case "value":
                            mapLM.keyValues().add(new ModelKeyValue(
                                    attributes.get("key"),
                                    attributes.get("url"),
                                    attributes.get("file"),
                                    attributes.get("template"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(mapLM.keyValues().getLast());
                            stack.push(qName);
                            break;
                        case "list":
                            mapLM.keyLists().add(new ModelKeyList(
                                    attributes.get("key"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(mapLM.keyLists().getLast());
                            stack.push("model/map/list");
                            break;
                        case "map":
                            mapLM.keyMaps().add(new ModelKeyMap(
                                    attributes.get("key"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(mapLM.keyMaps().getLast());
                            stack.push("model/map");
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + "with parent: " + parent);
                    }
                    break;
                case "model/map/list":
                    if (!(objectTracking.getLast() instanceof ModelKeyList)) {
                        throw new ClassCastException("Wrong object in the stack");
                    }
                    ModelKeyList keyListMML = (ModelKeyList) objectTracking.getLast();
                    switch (qName) {
                        case "value":
                            keyListMML.values().add(new ValueType(
                                    attributes.get("url"),
                                    attributes.get("file"),
                                    attributes.get("template"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if"))
                            );
                            objectTracking.add(keyListMML.values().getLast());
                            stack.push(qName);
                            break;
                        case "list":
                            keyListMML.lists().add(new ListType(
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(keyListMML.lists().getLast());
                            stack.push("model/list/" + qName);
                            break;
                        case "map":
                            keyListMML.maps().add(new MapType(
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(keyListMML.maps().getLast());
                            stack.push("model/list/" + qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + " with parent: " + parent);
                    }
                    break;
                case "model/map":
                    if (!(objectTracking.getLast() instanceof ModelKeyMap)) {
                        throw new ClassCastException("Wrong object in the stack");
                    }
                    ModelKeyMap map = (ModelKeyMap) objectTracking.getLast();
                    switch (qName) {
                        case "value":
                            map.keyValues().add(new ModelKeyValue(
                                    attributes.get("key"),
                                    attributes.get("url"),
                                    attributes.get("file"),
                                    attributes.get("template"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            stack.push(qName);
                            objectTracking.add(map.keyValues().getLast());
                            break;
                        case "list":
                            map.keyLists().add(new ModelKeyList(
                                    attributes.get("key"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(map.keyLists().getLast());
                            stack.push("model/map/" + qName);
                            break;
                        case "map":
                            map.keyMaps().add(new ModelKeyMap(
                                    attributes.get("key"),
                                    parseOrder(attributes.get("order")),
                                    attributes.get("if")
                            ));
                            objectTracking.add(map.keyMaps().getLast());
                            stack.push("model/" + qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid element: " + qName + " with parent: " + parent);
                    }
                    break;
                default:
                    throw new IllegalStateException("Invalid element: " + qName + " with parent: " + parent);
            }
        }
    }

    @Override
    public void endElement(String name) {
        if (name.equals("output") && topLevelOutput) {
            output = currentOutput;
            topLevelOutput = false;
        }
        objectTracking.pollLast();
        stack.pop();
    }

    @Override
    public void elementText(String value) {
        if (stack.isEmpty()) {
            return;
        }
        switch (stack.peek()) {
            case "help":
                help = value;
                break;
            case "context/list/value":
                ContextNode node = context.getLast().nodes().getLast();
                if (!(node instanceof ContextList)) {
                    throw new IllegalStateException("Unable to add 'value' to context node");
                }
                ((ContextList) node).values().add(value);
                break;
            case "context/enum/value":
                node = context.getLast().nodes().getLast();
                if (!(node instanceof ContextEnum)) {
                    throw new IllegalStateException("Unable to add 'value' to context node");
                }
                ((ContextEnum) node).values().add(value);
                break;
            case "context/boolean":
                node = context.getLast().nodes().getLast();
                if (!(node instanceof ContextBoolean)) {
                    throw new IllegalStateException("Unable to add 'value' to context node");
                }
                ((ContextBoolean) node).bool(parseBoolean(value));
                break;
            case "context/text":
                node = context.getLast().nodes().getLast();
                if (!(node instanceof ContextText)) {
                    throw new IllegalStateException("Unable to add 'value' to context node");
                }
                ((ContextText) node).text(value);
                break;
            case "step/help":
                currentStep.help(value);
                break;
            case "input/boolean/help":
                if (!(objectTracking.getLast() instanceof InputBoolean)) {
                    throw new IllegalStateException("Unable to add 'value' to input boolean node");
                }
                ((InputBoolean) objectTracking.getLast()).help(value);
                break;
            case "input/enum/help":
                if (!(objectTracking.getLast() instanceof InputEnum)) {
                    throw new IllegalStateException("Unable to add 'value' to input enum node");
                }
                ((InputEnum) objectTracking.getLast()).help(value);
                break;
            case "input/list/help":
                if (!(objectTracking.getLast() instanceof InputList)) {
                    throw new IllegalStateException("Unable to add 'value' to input list node");
                }
                ((InputList) objectTracking.getLast()).help(value);
                break;
            case "output/files/includes/include":
                currentOutput.filesList().getLast().includes().add(value);
                break;
            case "output/files/excludes/exclude":
                currentOutput.filesList().getLast().excludes().add(value);
                break;
            case "output/files/directory":
                currentOutput.filesList().getLast().directory(value);
                break;
            case "output/templates/directory":
                currentOutput.templates().getLast().directory(value);
                break;
            case "output/templates/includes/include":
                currentOutput.templates().getLast().includes().add(value);
                break;
            case "output/templates/excludes/exclude":
                currentOutput.templates().getLast().excludes().add(value);
                break;
            case "value":
                if (!(objectTracking.getLast() instanceof ValueType)) {
                    throw new ClassCastException("No value at top of stack");
                }
                ((ValueType) objectTracking.getLast()).value(value);
                break;
            default:
        }
    }

    private void addModelKeyValue(Model currentModel, String parent, Map<String, String> attributes) {
        currentModel.keyValues().add(new ModelKeyValue(
                readRequiredAttribute("key", parent, attributes),
                attributes.get("url"),
                attributes.get("file"),
                attributes.get("template"),
                parseOrder(attributes.get("order")),
                attributes.get("if")));
    }

    private void addInputText(Input input, Map<String, String> attributes) {
        input.nodes().add(new InputText(
                attributes.get("label"),
                attributes.get("name"),
                attributes.get("default"),
                attributes.get("prompt"),
                parseBoolean(attributes.get("optional") == null ? "false" : attributes.get("optional")),
                attributes.get("placeholder")
        ));
    }

    private void validateChilds(String child, String parent, String... validChilds) {
        if (!Arrays.stream(validChilds).collect(Collectors.toList()).contains(child)) {
            throw new IllegalStateException(String.format(
                    "Invalid child '%s' for '%s'", child, parent));
        }
    }

    private int parseOrder(String order) {
        return Integer.parseInt(order == null ? "100" : order);
    }

    private boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value == null ? "true" : value);
    }

}
