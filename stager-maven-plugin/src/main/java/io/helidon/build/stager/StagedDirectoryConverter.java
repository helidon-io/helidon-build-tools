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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Configuration converter for {@link StagedDirectory}.
 */
final class StagedDirectoryConverter {

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

    private StagedDirectoryConverter() {
    }

    /**
     * Convert a {@link PlexusConfiguration} instance to a list of {@link StagedDirectory}.
     *
     * @param configuration plexus configuration
     * @return list of {@link StagedDirectory}
     */
    static List<StagedDirectory> fromConfiguration(PlexusConfiguration configuration) {
        PlexusConfigNode parent = new PlexusConfigNode(configuration, null);
        VisitorImpl visitor = new VisitorImpl();
        parent.visit(visitor);
        return visitor.nestedDirectories(parent);
    }

    private static final class VisitorImpl implements Consumer<PlexusConfigNode> {

        private Map<PlexusConfigNode, Map<String, List<Object>>> allMappings = new LinkedHashMap<>();

        @Override
        public void accept(PlexusConfigNode node) {
            Map<String, List<Object>> mappings = allMappings.get(node);
            if (mappings == null) {
                mappings = new LinkedHashMap<>();
                allMappings.put(node, mappings);
            }
            Map<String, List<Object>> parentMappings = allMappings.get(node.parent());
            if (parentMappings == null) {
                parentMappings = new LinkedHashMap<>();
                allMappings.put(node.parent(), parentMappings);
            }
            List<Object> elements = new LinkedList<>();
            if (TASK_NAMES.contains(node.name())) {
                addTasks(node, node.name(), List.of(processTask(node)));
            } else {
                switch (node.name()) {
                    case "directory":
                        addNested(node, "directory", List.of(processDirectory(node)));
                        break;
                    case "unpack-artifacts":
                        addTasks(node, "unpack-artifact",
                                listAs(mappings.get("unpack-artifact"), UnpackArtifactTask.class));
                        break;
                    case "copy-artifacts":
                        addTasks(node, "copy-artifact",
                                listAs(mappings.get("copy-artifact"), CopyArtifactTask.class));
                        break;
                    case "symlinks":
                        addTasks(node, "symlink", listAs(mappings.get("symlink"), SymlinkTask.class));
                        break;
                    case "downloads":
                        addTasks(node, "download", listAs(mappings.get("download"), DownloadTask.class));
                        break;
                    case "archives":
                        addTasks(node, "archive", listAs(mappings.get("archive"), ArchiveTask.class));
                        break;
                    case "templates":
                        addTasks(node, "template", listAs(mappings.get("template"), TemplateTask.class));
                        break;
                    case "files":
                        addTasks(node, "file", listAs(mappings.get("file"), FileTask.class));
                        break;
                    case "iterators":
                        addNested(node, processTaskIterators(node));
                        break;
                    case "variables":
                        addNested(node, new Variables(listAs(mappings.get("variable"), Variable.class)));
                        break;
                    case "variable":
                        addNested(node, processVariable(node));
                        break;
                    case "value":
                        addNested(node, processVariableValue(node));
                        break;
                    default:
                        throw new IllegalStateException("Unsupported element: " + node.name());
                }
            }
        }

        void addNested(PlexusConfigNode node, String name, List<? extends Object> objects) {
            Map<String, List<Object>> parentMappings = allMappings.get(node.parent());
            if (parentMappings == null) {
                parentMappings = new LinkedHashMap<>();
                allMappings.put(node.parent(), parentMappings);
            }
            List<Object> elements = parentMappings.get(name);
            if (elements == null) {
                elements = new LinkedList<>();
                parentMappings.put(name, elements);
            }
            elements.addAll(objects);
        }

        void addNested(PlexusConfigNode node, Object object) {
            addNested(node, node.name(), List.of(object));
        }

        void addTasks(PlexusConfigNode node, String name, List<? extends StagingTask> tasks) {
            addNested(node, name, tasks);
        }

        StagedDirectory processDirectory(PlexusConfigNode node) {
            return new StagedDirectory(node.attributes().get("target"), nestedTasks(node));
        }

        StagingTask processTask(PlexusConfigNode node) {
            Map<String, String> attrs = node.attributes();
            TaskIterators taskIterators = nestedTaskIterators(node);
            switch (node.name()) {
                case "unpack-artifact":
                    return new UnpackArtifactTask(taskIterators,
                            new ArtifactGAV(attrs),
                            attrs.get("target"),
                            attrs.get("includes"),
                            attrs.get("excludes"));
                case "copy-artifact":
                    return new CopyArtifactTask(taskIterators,
                            new ArtifactGAV(attrs),
                            attrs.get("target"));
                case "symlink":
                    return new SymlinkTask(taskIterators,
                            attrs.get("source"),
                            attrs.get("target"));
                case "download":
                    return new DownloadTask(taskIterators,
                            attrs.get("url"),
                            attrs.get("target"));
                case "archive":
                    return new ArchiveTask(taskIterators,
                            nestedTasks(node),
                            attrs.get("target"),
                            attrs.get("includes"),
                            attrs.get("excludes"));
                case "template":
                    return new TemplateTask(taskIterators,
                            attrs.get("source"),
                            attrs.get("target"),
                            nestedTemplateVariables(node));
                case "file":
                    return new FileTask(taskIterators,
                            attrs.get("target"),
                            node.value(),
                            attrs.get("source"));
                default:
                    throw new IllegalStateException("Unknown task: " + node.name());
            }
        }

        VariableValue processVariableValue(PlexusConfigNode node) {
            Map<String, List<Object>> mappings = allMappings.get(node);
            List<Object> nestedVariables = mappings.get("variable");
            if (nestedVariables == null || nestedVariables.isEmpty()) {
                return new VariableValue.SimpleValue(node.value());
            }
            return new VariableValue.MapValue(listAs(nestedVariables, Variable.class));
        }

        Variable processVariable(PlexusConfigNode node) {
            Map<String, String> attrs = node.attributes();
            String varName = attrs.get("name");
            Map<String, List<Object>> mappings = allMappings.get(node);
            List<Object> values = mappings.get("value");
            if (values == null || values.isEmpty()) {
                return new Variable(varName, new VariableValue.SimpleValue(attrs.get("value")));
            }
            return new Variable(varName, new VariableValue.ListValue(listAs(values, VariableValue.class)));
        }

        TaskIterators processTaskIterators(PlexusConfigNode node) {
            Map<String, List<Object>> mappings = allMappings.get(node);
            if (mappings == null || mappings.isEmpty()) {
                return new TaskIterators(List.of());
            }
            return new TaskIterators(listAs(mappings.get("variables"), Variables.class).stream()
                    .map(TaskIterator::new).collect(Collectors.toList()));
        }

        List<StagingTask> nestedTasks(PlexusConfigNode node) {
            List<StagingTask> tasks = new LinkedList<>();
            Map<String, List<Object>> mappings = allMappings.get(node);
            for (Map.Entry<String, List<Object>> entry : mappings.entrySet()) {
                if (TASK_NAMES.contains(entry.getKey())) {
                    tasks.addAll(listAs(entry.getValue(), StagingTask.class));
                }
            }
            return tasks;
        }

        List<StagedDirectory> nestedDirectories(PlexusConfigNode node) {
            List<StagingTask> tasks = new LinkedList<>();
            Map<String, List<Object>> mappings = allMappings.get(node);
            List<Object> directories = mappings.get("directory");
            if (directories == null) {
                return List.of();
            }
            return listAs(directories, StagedDirectory.class);
        }

        Map<String, Object> nestedTemplateVariables(PlexusConfigNode node) {
            Map<String, List<Object>> mappings = allMappings.get(node);
            List<Object> variables = mappings.get("variables");
            if (variables.size() > 1) {
                throw new IllegalStateException(
                        "templates elements can have zero or one nested variables element");
            }
            if (variables.isEmpty()) {
                return Map.of();
            }
            return objectAs(variables.get(0), Variables.class)
                    .stream()
                    .collect(Collectors.toMap(Variable::name, v -> v.value().unwrap()));
        }

        TaskIterators nestedTaskIterators(PlexusConfigNode node) {
            Map<String, List<Object>> mappings = allMappings.get(node);
            List<Object> iteratorsObjects = mappings.get("iterators");
            if (iteratorsObjects == null || iteratorsObjects.isEmpty()) {
                return new TaskIterators();
            }
            if (iteratorsObjects.size() > 1) {
                throw new IllegalStateException(
                        "task elements can have zero or one iterators element");
            }
            return objectAs(iteratorsObjects.get(0), TaskIterators.class);
        }
    }

    @SuppressWarnings("unchecked")
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

    private static <T> List<T> listAs(List<Object> list, Class<T> type) throws IllegalArgumentException {
        LinkedList<T> result = new LinkedList<>();
        for (Object o : list) {
            result.add(objectAs(o, type));
        }
        return result;
    }

    private static String taskName(Class<? extends StagingTask> taskClass) {
        String className = taskClass.getSimpleName().replace("Task", "");
        String taskName = "";
        for (int i = 0; i < className.length(); i++) {
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
}
