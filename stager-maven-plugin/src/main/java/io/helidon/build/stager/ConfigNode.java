/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.stager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * A config node that holds a node with its parent and a list of mapped children objects.
 */
public class ConfigNode {

    private static final List<Class<? extends StagingTask>> ALL_TASKS = List.of(
            ArchiveTask.class,
            CopyArtifactTask.class,
            DownloadTask.class,
            FileTask.class,
            SymlinkTask.class,
            TemplateTask.class,
            UnpackArtifactTask.class);
    private static final List<String> TASK_NAMES = ALL_TASKS.stream()
            .map(c -> taskName(c))
            .collect(Collectors.toList());

    private final PlexusConfiguration orig;
    private final ConfigNode parent;
    private final LinkedList<MappedConfigNode> mappedChildren;

    ConfigNode(PlexusConfiguration orig, ConfigNode parent) {
        this.orig = Objects.requireNonNull(orig, "orig is null");
        this.parent = parent;
        this.mappedChildren = new LinkedList<>();
    }

    /**
     * Get the parent node.
     *
     * @return parent, may be {@code null}
     */
    ConfigNode parent() {
        return parent;
    }

    /**
     * Get the nested children.
     *
     * @return list of children config nodes
     */
    List<ConfigNode> children() {
        List<ConfigNode> children = new LinkedList<>();
        for (PlexusConfiguration child : orig.getChildren()) {
            children.add(new ConfigNode(child, this));
        }
        return children;
    }

    /**
     * Visit this config node.
     *
     * @param visitor simple visitor
     */
    void visit(Consumer<ConfigNode> visitor) {
        LinkedList<ConfigNode> stack = new LinkedList<>();
        for (PlexusConfiguration child : orig.getChildren()) {
            stack.push(new ConfigNode(child, parent));
        }
        ConfigNode parent = this;
        while (!stack.isEmpty()) {
            ConfigNode node = stack.peek();
            if (node.equals(parent)) {
                // leaving node
                parent = node.parent();
                stack.pop();
                visitor.accept(node);
            } else {
                List<ConfigNode> children = node.children();
                if (!children.isEmpty()) {
                    // entering node
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                } else {
                    // leaf
                    parent = node.parent();
                    stack.pop();
                    visitor.accept(node);
                }
            }
        }
    }

    /**
     * Add a mapped / converted object for a child.
     * @param node child node
     * @param mappedObject mapped object
     */
    void addMappedChild(ConfigNode node, Object mappedObject) {
        mappedChildren.addLast(new MappedConfigNode(node, mappedObject));
    }

    /**
     * Convert this config node.
     *
     * @return MappedConfigNode
     */
    Object convert() {
        Map<String, String> attrs = attributes();
        String nodeName = orig.getName();
        Object mappedObject;
        if (TASK_NAMES.contains(nodeName)) {
            return asTask();
        }
        switch (nodeName) {
            case "unpack-artifacts":
                return convertMappedNodes(mappedChildren, UnpackArtifactTask.class);
            case "copy-artifacts":
                return convertMappedNodes(mappedChildren, CopyArtifactTask.class);
            case "symlinks":
                return convertMappedNodes(mappedChildren, SymlinkTask.class);
            case "downloads":
                return convertMappedNodes(mappedChildren, DownloadTask.class);
            case "archives":
                return convertMappedNodes(mappedChildren, ArchiveTask.class);
            case "templates":
                return convertMappedNodes(mappedChildren, TemplateTask.class);
            case "files":
                return convertMappedNodes(mappedChildren, FileTask.class);
            case "iterators":
                return asTaskIterators();
            case "variables":
                return asVariables();
            case "variable":
                return asVariable();
            case "value":
                return asVariableValue();
            default:
                throw new IllegalStateException("Unsupported element: " + nodeName);
        }
    }

    /**
     * Convert this node to an instance of {@link StagedDirectory}.
     *
     * @return StagedDirectory
     */
    StagedDirectory asStagedDirectory() {
        return new StagedDirectory(attributes().get("target"), tasks());
    }

    /**
     * Convert this node to an instance of {@link StagingTask}.
     *
     * @return StagingTask
     */
    StagingTask asTask() {
        Map<String, String> attrs = attributes();
        List<Map<String, String>> taskIts = taskIterators();
        String target = attrs.get("target");
        String nodeName = orig.getName();
        switch (nodeName) {
            case "unpack-artifact":
                return new UnpackArtifactTask(taskIts, gav(attrs), target, attrs.get("includes"),
                        attrs.get("excludes"));
            case "copy-artifact":
                return new CopyArtifactTask(taskIts, gav(attrs), target);
            case "symlink":
                return new SymlinkTask(taskIts, attrs.get("source"), target);
            case "download":
                return new DownloadTask(taskIts, target, attrs.get("url"));
            case "archive":
                return new ArchiveTask(taskIts, tasks(), target, attrs.get("includes"), attrs.get("excludes"));
            case "template":
                return new TemplateTask(taskIts, target, attrs.get("source"), templateVariables());
            case "file":
                return new FileTask(taskIts, target, orig.getValue(), attrs.get("source"));
            default:
                throw new IllegalStateException("Unknown task: " + nodeName);
        }
    }

    /**
     * Get the nested {@link StagingTask}.
     *
     * @return list of {@link StagingTask}
     */
    List<StagingTask> tasks() {
        List<StagingTask> tasks = new LinkedList<>();
        for (MappedConfigNode mapped : mappedChildren) {
            String nodeName = mapped.node().orig.getName();
            if ("iterators".equals(nodeName)) {
                continue;
            }
            if (TASK_NAMES.contains(nodeName)) {
                tasks.add(objectAs(mapped.mappedObject, StagingTask.class));
            } else {
                throw new IllegalStateException("Unknown task: " + nodeName);
            }
        }
        return tasks;
    }

    /**
     * Convert this config node to a {@link VariableValue}.
     *
     * @return VariableValue
     */
    VariableValue asVariableValue() {
        if (mappedChildren.isEmpty()) {
            return new VariableValue.SimpleValue(orig.getValue());
        }
        return new VariableValue.ListValue(convertMappedNodes(mappedChildren, VariableValue.class));
    }

    /**
     * Convert this config node to an instance of {@link Variables}.
     *
     * @return Variables
     */
    Variables asVariables() {
        return new Variables(convertMappedNodes(mappedChildren, Variable.class));
    }

    /**
     * Convert this config node to an instance of {@link Variable}.
     *
     * @return Variable
     */
    Variable asVariable() {
        Map<String, String> attrs = attributes();
        String varName = attrs.get("name");
        if (mappedChildren.isEmpty()) {
            return new Variable(varName, new VariableValue.SimpleValue(attrs.get("value")));
        }
        List<VariableValue> values = convertMappedNodes(mappedChildren, VariableValue.class);
        return new Variable(varName, new VariableValue.ListValue(values));
    }

    /**
     * Get the nested template variables.
     *
     * @return map of {@link VariableValue} keyed by their variable names.
     */
    Map<String, VariableValue> templateVariables() {
        List<Object> variablesObjects = mappedChildrenObjects("variables");
        if (variablesObjects.size() > 1) {
            throw new IllegalStateException(
                    "templates elements can have zero or one nested variables element");
        }
        if (variablesObjects.isEmpty()) {
            return Map.of();
        }
        return objectAs(variablesObjects.get(0), Variables.class).asMap();
    }

    /**
     * Convert this config node to an instance of {@link TaskIterators}.
     *
     * @return TaskIterators
     */
    TaskIterators asTaskIterators() {
        return new TaskIterators(convertMappedNodes(mappedChildren, Variables.class));
    }

    /**
     * Get the nested task iterators as a list of map.
     *
     * @return list of maps
     */
    List<Map<String, String>> taskIterators() {
        List<Object> iteratorsObjects = mappedChildrenObjects("iterators");
        if (iteratorsObjects.size() > 1) {
            throw new IllegalStateException(
                    "task elements can have zero or one iterators element");
        }
        if (iteratorsObjects.isEmpty()) {
            return List.of();
        }
        return objectAs(iteratorsObjects.get(0), TaskIterators.class).asList();
    }

    private Map<String, String> attributes() {
        Map<String, String> attributes = new HashMap<>();
        for (String attrName : orig.getAttributeNames()) {
            attributes.put(attrName, orig.getAttribute(attrName));
        }
        return attributes;
    }

    private List<Object> mappedChildrenObjects(String name) {
        LinkedList<Object> result = new LinkedList<>();
        for (MappedConfigNode mappedChild : mappedChildren) {
            if (name.equals(mappedChild.node.orig.getName())) {
                result.add(mappedChild.mappedObject);
            }
        }
        return result;
    }

    private <T> List<T> convertMappedChildren(Class<T> type) {
        LinkedList<T> result = new LinkedList<>();
        for (MappedConfigNode o : mappedChildren) {
            result.add(o.as(type));
        }
        return result;
    }

    private static ArtifactGAV gav(Map<String, String> attrs) {
        return new ArtifactGAV(attrs.get("groupId"), attrs.get("artifactId"), attrs.get("version"), attrs.get("type"),
                attrs.get("classifier"));
    }

    private static String taskName(Class<? extends StagingTask> taskClass) {
        String className = taskClass.getSimpleName().replace("Task", "");
        String taskName = "";
        for (int i=0; i < taskName.length(); i++) {
            char c = className.charAt(i);
            if (i == 0) {
                taskName += Character.toLowerCase(c);
            } else if (Character.isLowerCase(c)) {
                taskName += c;
            } else {
                taskName += "-" + Character.toLowerCase(c);
            }
        }
        return taskName;
    }

    private static <T> T objectAs(Object obj, Class<T> type) throws IllegalArgumentException {
        if (obj == null) {
            return null;
        } else if (type.isAssignableFrom(obj.getClass())) {
            return (T) obj;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Not an instance of %s: %s", type.getSimpleName(), obj.getClass().getSimpleName()));
        }
    }

    private static <T> List<T> convertMappedNodes(List<MappedConfigNode> mappedNodes, Class<T> type)
            throws IllegalArgumentException {

        LinkedList<T> result = new LinkedList<>();
        for (MappedConfigNode o : mappedNodes) {
            result.add(o.as(type));
        }
        return result;
    }

    private static final class MappedConfigNode {

        private final ConfigNode node;
        private final Object mappedObject;

        MappedConfigNode(ConfigNode node, Object mappedObject) {
            this.node = node;
            this.mappedObject = mappedObject;
        }

        ConfigNode node() {
            return node;
        }

        Object mappedObject() {
            return mappedObject;
        }

        <T> T as(Class<T> type) {
            return objectAs(mappedObject, type);
        }
    }
}
