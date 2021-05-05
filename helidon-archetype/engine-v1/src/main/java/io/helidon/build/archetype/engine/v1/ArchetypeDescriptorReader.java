/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v1;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Choice;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.FileSet;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.FileSets;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.FlowNode;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Input;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.InputFlow;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Property;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Replacement;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Select;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.TemplateSets;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Transformation;
import io.helidon.build.common.xml.SimpleXMLParser;

/**
 * {@link ArchetypeDescriptor} reader.
 */
final class ArchetypeDescriptorReader implements SimpleXMLParser.Reader {

    private String modelVersion;
    private String name;
    private final LinkedList<Property> properties = new LinkedList<>();
    private final LinkedList<Transformation> transformations = new LinkedList<>();
    private TemplateSets templateSets;
    private FileSets fileSets;
    private final InputFlow inputFlow = new InputFlow();
    private final LinkedList<String> stack;
    private final Map<String, Property> propertiesMap;
    private final Map<String, Transformation> transformationsMap;

    private ArchetypeDescriptorReader() {
        stack = new LinkedList<>();
        propertiesMap = new HashMap<>();
        transformationsMap = new HashMap<>();
    }

    /**
     * Read the descriptor from the given input stream.
     * @param is input stream
     * @return descriptor, never {@code null}
     */
    static ArchetypeDescriptor read(InputStream is) {
        try {
            ArchetypeDescriptorReader reader = new ArchetypeDescriptorReader();
            SimpleXMLParser.parse(is, reader);
            return new ArchetypeDescriptor(reader.modelVersion, reader.name, reader.properties, reader.transformations,
                    reader.templateSets, reader.fileSets, reader.inputFlow);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public void startElement(String qName, Map<String, String> attributes) {
        String parent = stack.peek();
        if (parent == null) {
            if (!"archetype-descriptor".equals(qName)) {
                throw new IllegalStateException("Invalid root element '" + qName + "'");
            }
            modelVersion = readRequiredAttribute("modelVersion", qName, attributes);
            this.name = readRequiredAttribute("name", qName, attributes);
            stack.push("archetype-descriptor");
        } else {
            switch (parent) {
                case "archetype-descriptor":
                    switch (qName) {
                        case "properties":
                        case "input-flow":
                        case "transformations":
                            stack.push(qName);
                            break;
                        case "template-sets":
                            templateSets = new TemplateSets(transformationRefs(attributes, qName));
                            stack.push(qName);
                            break;
                        case "file-sets":
                            fileSets = new FileSets(transformationRefs(attributes, qName));
                            stack.push(qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid top-level element: " + qName);
                    }
                    break;
                case "properties":
                    validateChild("property", parent, qName);
                    Property prop = new Property(
                            // TODO validate property id (dot separated alphanumerical)
                            readRequiredAttribute("id", qName, attributes),
                            attributes.get("value"),
                            Boolean.parseBoolean(Optional.ofNullable(attributes.get("exported")).orElse("true")),
                            Boolean.parseBoolean(attributes.get("readonly")));
                    properties.add(prop);
                    propertiesMap.put(prop.id(), prop);
                    stack.push("properties/property");
                    break;
                case "transformations":
                    validateChild("transformation", parent, qName);
                    Transformation transformation = new Transformation(readRequiredAttribute("id", qName, attributes));
                    transformations.add(transformation);
                    transformationsMap.put(transformation.id(), transformation);
                    stack.push("transformations/transformation");
                    break;
                case "transformations/transformation":
                    validateChild("replace", parent, qName);
                    transformations.getLast().replacements().add(new Replacement(
                            readRequiredAttribute("regex", qName, attributes),
                            readRequiredAttribute("replacement", qName, attributes)));
                    stack.push("transformations/transformation/replace");
                    break;
                case "template-sets":
                    validateChild("template-set", parent, qName);
                    templateSets.templateSets().add(new FileSet(
                            transformationRefs(attributes, qName),
                                    propertyRefs(attributes, "if", qName),
                                    propertyRefs(attributes, "unless", qName)));
                    stack.push("template-sets/template-set");
                    break;
                case "template-sets/template-set":
                    switch (qName) {
                        case "directory":
                            stack.push("template-sets/template-set/directory");
                            break;
                        case "includes":
                            stack.push("template-sets/template-set/includes");
                            break;
                        case "excludes":
                            stack.push("template-sets/template-set/excludes");
                            break;
                        default:
                            throw new IllegalStateException("Invalid template-set child node: '" + qName + "'");
                    }
                    break;
                case "template-sets/template-set/includes":
                    stack.push("template-sets/template-set/includes/include");
                    break;
                case "template-sets/template-set/excludes":
                    stack.push("template-sets/template-set/excludes/exclude");
                    break;
                case "file-sets":
                    validateChild("file-set", parent, qName);
                    fileSets.fileSets().add(new FileSet(
                            transformationRefs(attributes, qName),
                                    propertyRefs(attributes, "if", qName),
                                    propertyRefs(attributes, "unless", qName)));
                    stack.push("file-sets/file-set");
                    break;
                case "file-sets/file-set":
                    switch (qName) {
                        case "directory":
                            stack.push("file-sets/file-set/directory");
                            break;
                        case "includes":
                            stack.push("file-sets/file-set/includes");
                            break;
                        case "excludes":
                            stack.push("file-sets/file-set/excludes");
                            break;
                        default:
                            throw new IllegalStateException("Invalid file-set child node: '" + qName + "'");
                    }
                    break;
                case "file-sets/file-set/includes":
                    stack.push("file-sets/file-set/includes/include");
                    break;
                case "file-sets/file-set/excludes":
                    stack.push("file-sets/file-set/excludes/exclude");
                    break;
                case "input-flow":
                    switch (qName) {
                        case "select":
                            inputFlow.nodes().add(new Select(
                                    readRequiredAttribute("text", qName, attributes),
                                    propertyRefs(attributes, "if", qName),
                                    propertyRefs(attributes, "unless", qName)));
                            stack.push("input-flow/select");
                            break;
                        case "input":
                            inputFlow.nodes().add(new Input(
                                    propertyRef(readRequiredAttribute("property", qName, attributes), qName),
                                    attributes.get("default"),
                                    readRequiredAttribute("text", qName, attributes),
                                    propertyRefs(attributes, "if", qName),
                                    propertyRefs(attributes, "unless", qName)));
                            stack.push("input-flow/input");
                            break;
                        default:
                            throw new IllegalStateException("Invalid input flow child node: '" + qName + "'");
                    }
                    break;
                case "input-flow/select":
                    validateChild("choice", parent, qName);
                    FlowNode lastFlowNode = inputFlow.nodes().getLast();
                    if (!(lastFlowNode instanceof Select)) {
                        throw new IllegalStateException("Unable to add 'choice' to flow node");
                    }
                    ((Select) lastFlowNode).choices().add(new Choice(
                            propertyRef(readRequiredAttribute("property", qName, attributes), qName),
                            readRequiredAttribute("text", qName, attributes),
                            propertyRefs(attributes, "if", qName),
                            propertyRefs(attributes, "unless", qName)));
                    stack.push("input-flow/select/choice");
                    break;
                default:
                    throw new IllegalStateException("Invalid element: " + qName);
            }
        }
    }

    @Override
    public void endElement(String name) {
        stack.pop();
    }

    @Override
    public void elementText(String value) {
        if (stack.isEmpty()) {
            return;
        }
        switch (stack.peek()) {
            case "template-sets/template-set/directory":
                templateSets.templateSets().getLast().directory(value);
                break;
            case "template-sets/template-set/includes/include":
                templateSets.templateSets().getLast().includes().add(value);
                break;
            case "template-sets/template-set/excludes/exclude":
                templateSets.templateSets().getLast().excludes().add(value);
                break;
            case "file-sets/file-set/directory":
                fileSets.fileSets().getLast().directory(value);
                break;
            case "file-sets/file-set/includes/include":
                fileSets.fileSets().getLast().includes().add(value);
                break;
            case "file-sets/file-set/excludes/exclude":
                fileSets.fileSets().getLast().excludes().add(value);
                break;
            default:
        }
    }

    private Property propertyRef(String propName, String qName) {
        Property ref = propertiesMap.get(propName);
        if (ref == null) {
            throw new IllegalStateException("Unknown property reference: '" + propName + "' in element: '" + qName + "'");
        }
        return ref;
    }

    private List<Property> propertyRefs(Map<String, String> attr, String attrName, String qName) {
        String refNames = attr.get(attrName);
        if (refNames == null) {
            return Collections.emptyList();
        }
        List<Property> refs = new LinkedList<>();
        for (String refName : refNames.split(",")) {
            refs.add(propertyRef(refName, qName));
        }
        return refs;
    }

    private List<Transformation> transformationRefs(Map<String, String> attr, String qName) {
        String refNames = attr.get("transformations");
        if (refNames == null) {
            return Collections.emptyList();
        }
        List<Transformation> refs = new LinkedList<>();
        for (String refName : refNames.split(",")) {
            Transformation ref = transformationsMap.get(refName);
            if (ref == null) {
                throw new IllegalStateException("Unknown transformation reference: '" + refName + "' in element: '"
                        + qName + "'");
            }
            refs.add(ref);
        }
        return refs;
    }
}
