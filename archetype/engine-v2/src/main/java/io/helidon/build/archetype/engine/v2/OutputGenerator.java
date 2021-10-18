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

package io.helidon.build.archetype.engine.v2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeException;
import io.helidon.build.archetype.engine.v2.archive.ZipArchetype;
import io.helidon.build.archetype.engine.v2.descriptor.ListType;
import io.helidon.build.archetype.engine.v2.descriptor.MapType;
import io.helidon.build.archetype.engine.v2.descriptor.Model;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.descriptor.Replacement;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;
import io.helidon.build.archetype.engine.v2.interpreter.ASTNode;
import io.helidon.build.archetype.engine.v2.interpreter.ContextBooleanAST;
import io.helidon.build.archetype.engine.v2.interpreter.ContextNodeAST;
import io.helidon.build.archetype.engine.v2.interpreter.ContextTextAST;
import io.helidon.build.archetype.engine.v2.interpreter.FileSetAST;
import io.helidon.build.archetype.engine.v2.interpreter.FileSetsAST;
import io.helidon.build.archetype.engine.v2.interpreter.Flow;
import io.helidon.build.archetype.engine.v2.interpreter.ListTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.MapTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyListAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyMapAST;
import io.helidon.build.archetype.engine.v2.interpreter.ModelKeyValueAST;
import io.helidon.build.archetype.engine.v2.interpreter.OutputAST;
import io.helidon.build.archetype.engine.v2.interpreter.TemplateAST;
import io.helidon.build.archetype.engine.v2.interpreter.TemplatesAST;
import io.helidon.build.archetype.engine.v2.interpreter.TransformationAST;
import io.helidon.build.archetype.engine.v2.interpreter.ValueTypeAST;
import io.helidon.build.archetype.engine.v2.interpreter.Visitable;
import io.helidon.build.archetype.engine.v2.template.TemplateModel;

/**
 * Generate Output files from interpreter.
 */
public class OutputGenerator {

    private final TemplateModel model;
    private final Archetype archetype;
    private final Map<String, String> properties;
    private final List<OutputAST> nodes;
    private final List<TransformationAST> transformations;
    private final List<TemplateAST> template;
    private final List<TemplatesAST> templates;
    private final List<FileSetAST> file;
    private final List<FileSetsAST> files;

    /**
     * OutputGenerator constructor.
     *
     * @param result Flow.Result from interpreter
     */
    OutputGenerator(Flow.Result result) {
        Objects.requireNonNull(result, "Flow result is null");

        this.nodes = getOutputNodes(result.outputs());
        this.model = createUniqueModel();
        this.archetype = result.archetype();
        this.properties = parseContextProperties(result.context());

        this.transformations = nodes.stream()
                .flatMap(output -> output.children().stream())
                .filter(o -> o instanceof TransformationAST)
                .map(t -> (TransformationAST) t)
                .collect(Collectors.toList());

        this.template = nodes.stream()
                .flatMap(output -> output.children().stream())
                .filter(o -> o instanceof TemplateAST)
                .map(t -> (TemplateAST) t)
                .filter(t -> t.engine().equals("mustache"))
                .collect(Collectors.toList());

        this.templates = nodes.stream()
                .flatMap(output -> output.children().stream())
                .filter(o -> o instanceof TemplatesAST)
                .map(t -> (TemplatesAST) t)
                .filter(t -> t.engine().equals("mustache"))
                .collect(Collectors.toList());

        this.file = nodes.stream()
                .flatMap(output -> output.children().stream())
                .filter(o -> o instanceof FileSetAST)
                .map(t -> (FileSetAST) t)
                .collect(Collectors.toList());

        this.files = nodes.stream()
                .flatMap(output -> output.children().stream())
                .filter(o -> o instanceof FileSetsAST)
                .map(t -> (FileSetsAST) t)
                .collect(Collectors.toList());
    }

    private Map<String, String> parseContextProperties(Map<String, ContextNodeAST> context) {
        if (context == null) {
            return new HashMap<>();
        }

        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, ContextNodeAST> entry : context.entrySet()) {
            ContextNodeAST node = entry.getValue();
            if (node instanceof ContextBooleanAST) {
                resolved.put(entry.getKey(), String.valueOf(((ContextBooleanAST) node).bool()));
            }
            if (node instanceof ContextTextAST) {
                resolved.put(entry.getKey(), ((ContextTextAST) node).text());
            }
        }
        return resolved;
    }

    /**
     * Generate output files.
     *
     * @param outputDirectory Output directory where the files will be generated
     */
    public void generate(File outputDirectory) throws IOException {
        Objects.requireNonNull(outputDirectory, "output directory is null");

        for (TemplateAST templateAST : template) {
            File outputFile = new File(outputDirectory, templateAST.target());
            outputFile.getParentFile().mkdirs();
            try (InputStream inputStream = archetype.getInputStream(templateAST.source())) {
                if (templateAST.engine().equals("mustache")) {
                    MustacheHandler.renderMustacheTemplate(
                            inputStream,
                            templateAST.source(),
                            new FileOutputStream(outputFile),
                            model);
                } else {
                    Files.copy(
                            inputStream,
                            outputFile.toPath());
                }
            }
        }

        for (TemplatesAST templatesAST : templates) {
            Path rootDirectory = Path.of(templatesAST.location().currentDirectory()).resolve(templatesAST.directory());
            TemplateModel templatesModel = createTemplatesModel(templatesAST);

            for (String include : resolveIncludes(templatesAST)) {
                String outPath = transform(
                        targetPath(templatesAST.directory(), include),
                        templatesAST.transformation());
                File outputFile = new File(outputDirectory, outPath);
                outputFile.getParentFile().mkdirs();
                try (InputStream inputStream = archetype.getInputStream(rootDirectory.resolve(include).toString())) {
                    MustacheHandler.renderMustacheTemplate(
                            inputStream,
                            outPath,
                            new FileOutputStream(outputFile),
                            templatesModel);
                }
            }
        }

        for (FileSetAST fileAST : file) {
            File outputFile = new File(outputDirectory, fileAST.target());
            outputFile.getParentFile().mkdirs();
            try (InputStream inputStream = archetype.getInputStream(fileAST.source())) {
                Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        for (FileSetsAST filesAST : files) {
            Path rootDirectory = Path.of(filesAST.location().currentDirectory()).resolve(filesAST.directory());
            for (String include : resolveIncludes(filesAST)) {
                String outPath = processTransformation(
                        targetPath(filesAST.directory(), include),
                        filesAST.transformations());
                File outputFile = new File(outputDirectory, outPath);
                outputFile.getParentFile().mkdirs();
                try (InputStream inputStream = archetype.getInputStream(rootDirectory.resolve(include).toString())) {
                    Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private String targetPath(String directory, String filePath) {
        String resolved = directory.replaceFirst("files", "");
        return Path.of(resolved)
                .resolve(filePath)
                .toString();
    }

    private List<String> resolveIncludes(TemplatesAST templatesAST) {
        return resolveIncludes(
                Path.of(templatesAST.location().currentDirectory()).resolve(templatesAST.directory()).toString(),
                templatesAST.includes(),
                templatesAST.excludes());
    }

    private List<String> resolveIncludes(FileSetsAST filesAST) {
        return resolveIncludes(
                Path.of(filesAST.location().currentDirectory()).resolve(filesAST.directory()).toString(),
                filesAST.includes(),
                filesAST.excludes());
    }

    private List<String> resolveIncludes(String directory, List<String> includes, List<String> excludes) {
        List<String> excludesPath = getPathsFromDirectory(directory, excludes);
        List<String> includesPath = getPathsFromDirectory(directory, includes);
        return includesPath.stream()
                .filter(s -> !excludesPath.contains(s))
                .collect(Collectors.toList());
    }

    private List<String> getPathsFromDirectory(String directory, List<String> paths) {
        List<String> resolved = new LinkedList<>();

        for (String path : paths) {
            if (path.contains("**/*")) {
                try {
                    String extension = path.substring(path.lastIndexOf("."));
                    resolved.addAll(archetype.getPaths().stream()
                            .map(s -> getPath(directory, s))
                            .filter(Objects::nonNull)
                            .filter(s -> !Path.of(s).toUri().toString().contains("../"))
                            .filter(s -> s.contains(extension))
                            .collect(Collectors.toList()));
                } catch (IndexOutOfBoundsException e) {
                    resolved.addAll(archetype.getPaths().stream()
                            .map(s -> getPath(directory, s))
                            .filter(Objects::nonNull)
                            .filter(s -> !Path.of(s).toUri().toString().contains("../"))
                            .collect(Collectors.toList()));
                }
            } else {
                if (checkFullPath(path, directory)) {
                    resolved.add(path);
                }
            }
        }
        return resolved;
    }

    private boolean checkFullPath(String include, String directory) {
        if (archetype instanceof ZipArchetype) {
            include = Path.of("/" + directory).resolve(include).toString();
        }
        String path = getPath(directory, include);
        return path != null;
    }

    private String getPath(String directory, String file) {
        String path;
        try {
            if (archetype instanceof ZipArchetype) {
                directory = "/" + directory;
                path = file.startsWith(directory)
                        ? archetype.getPath(file).toString().substring(directory.length() + 1)
                        : null;
            } else {
                path = archetype.getPath(directory).relativize(Path.of(file)).toString();
            }
        } catch (ArchetypeException e) {
            return null;
        }
        return path;
    }

    private TemplateModel createTemplatesModel(TemplatesAST templatesAST) {
        TemplateModel templatesModel = new TemplateModel();
        Optional<ModelAST> modelAST = templatesAST.children().stream()
                .filter(o -> o instanceof ModelAST)
                .map(m -> (ModelAST) m)
                .findFirst();
        templatesModel.mergeModel(model.model());
        modelAST.ifPresent(ast -> templatesModel.mergeModel(convertASTModel(ast)));
        return templatesModel;
    }

    private String transform(String input, String transformation) {
        return transformation == null ? input
                : processTransformation(input, Arrays.asList(transformation.split(",")));
    }

    private String processTransformation(String output, List<String> applicable) {
        if (applicable.isEmpty()) {
            return output;
        }

        List<Replacement> replacements = transformations.stream()
                .filter(t -> applicable.contains(t.id()))
                .flatMap((t) -> t.replacements().stream())
                .collect(Collectors.toList());

        for (Replacement rep : replacements) {
            String replacement = evaluate(rep.replacement(), properties);
            output = output.replaceAll(rep.regex(), replacement);
        }
        return output;
    }

    /**
     * Resolve a property of the form <code>${prop}</code>.
     *
     * @param input input to be resolved
     * @param properties properties values
     * @return resolved property
     */
    private String evaluate(String input, Map<String, String> properties) {
        int start = input.indexOf("${");
        int end = input.indexOf("}", start);
        int index = 0;
        String resolved = null;
        while (start >= 0 && end > 0) {
            if (resolved == null) {
                resolved = input.substring(index, start);
            } else {
                resolved += input.substring(index, start);
            }
            String propName = input.substring(start + 2, end);

            int matchStart = 0;
            do {
                matchStart = propName.indexOf("/", matchStart + 1);
            } while (matchStart > 0 && propName.charAt(matchStart - 1) == '\\');
            int matchEnd = matchStart;
            do {
                matchEnd = propName.indexOf("/", matchEnd + 1);
            } while (matchStart > 0 && propName.charAt(matchStart - 1) == '\\');

            String regexp = null;
            String replace = null;
            if (matchStart > 0 && matchEnd > matchStart) {
                regexp = propName.substring(matchStart + 1, matchEnd);
                replace = propName.substring(matchEnd + 1);
                propName = propName.substring(0, matchStart);
            }

            String propValue = properties.get(propName);
            if (propValue == null) {
                propValue = "";
            } else if (regexp != null && replace != null) {
                propValue = propValue.replaceAll(regexp, replace);
            }

            resolved += propValue;
            index = end + 1;
            start = input.indexOf("${", index);
            end = input.indexOf("}", index);
        }
        if (resolved != null) {
            return resolved + input.substring(index);
        }
        return input;
    }

    /**
     * Consume a list of output nodes from interpreter and create a unique template model.
     *
     * @return Unique template model
     */
    TemplateModel createUniqueModel() {
        Objects.requireNonNull(nodes, "outputNodes is null");

        TemplateModel templateModel = new TemplateModel();
        List<ModelAST> models = nodes.stream()
                .flatMap(output -> output.children().stream())
                .filter(o -> o instanceof ModelAST)
                .map(o -> (ModelAST) o)
                .collect(Collectors.toList());

        for (ModelAST node : models) {
            templateModel.mergeModel(convertASTModel(node));
        }
        return templateModel;
    }

    private Model convertASTModel(ModelAST model) {
        Model modelDescriptor = new Model("true");
        convertKeyElements(modelDescriptor.keyValues(),
                modelDescriptor.keyLists(),
                modelDescriptor.keyMaps(),
                model.children());
        return modelDescriptor;
    }

    private Collection<? extends ModelKeyMap> convertASTKeyMaps(List<ModelKeyMapAST> astMaps) {
        LinkedList<ModelKeyMap> maps = new LinkedList<>();

        for (ModelKeyMapAST map : astMaps) {
            ModelKeyMap keyMap = new ModelKeyMap(map.key(), map.order(), "true");
            convertKeyElements(keyMap.keyValues(), keyMap.keyLists(), keyMap.keyMaps(), map.children());
            maps.add(keyMap);
        }

        return maps;
    }

    private Collection<? extends ModelKeyList> convertASTKeyLists(List<ModelKeyListAST> astLists) {
        LinkedList<ModelKeyList> lists = new LinkedList<>();

        for (ModelKeyListAST list : astLists) {
            ModelKeyList keyList = new ModelKeyList(list.key(), list.order(), "true");
            convertElements(keyList.values(), keyList.lists(), keyList.maps(), list.children());
            lists.add(keyList);
        }

        return lists;
    }

    private Collection<? extends ModelKeyValue> convertASTKeyValues(List<ModelKeyValueAST> astValues) {
        LinkedList<ModelKeyValue> values = new LinkedList<>();

        for (ModelKeyValueAST value : astValues) {
            ModelKeyValue keyValue = new ModelKeyValue(
                    value.key(),
                    value.url(),
                    value.file(),
                    value.template(),
                    value.order(),
                    "true");
            keyValue.value(value.value());
            values.add(keyValue);
        }
        return values;
    }

    private Collection<? extends ValueType> convertASTValues(List<ValueTypeAST> astValues) {
        LinkedList<ValueType> values = new LinkedList<>();

        for (ValueTypeAST value : astValues) {
            ValueType valueType = new ValueType(
                    value.url(),
                    value.file(),
                    value.template(),
                    value.order(),
                    "true");
            valueType.value(value.value());
            values.add(valueType);
        }
        return values;
    }

    private Collection<? extends ListType> convertASTLists(List<ListTypeAST> astList) {
        LinkedList<ListType> lists = new LinkedList<>();

        for (ListTypeAST list : astList) {
            ListType listType = new ListType(list.order(), "true");
            convertElements(listType.values(), listType.lists(), listType.maps(), list.children());
            lists.add(listType);
        }

        return lists;
    }

    private Collection<? extends MapType> convertASTMaps(List<MapTypeAST> astMap) {
        LinkedList<MapType> maps = new LinkedList<>();

        for (MapTypeAST map : astMap) {
            MapType mapType = new MapType(map.order(), "true");
            convertKeyElements(mapType.keyValues(), mapType.keyLists(), mapType.keyMaps(), map.children());
            maps.add(mapType);
        }

        return maps;
    }

    private void convertKeyElements(LinkedList<ModelKeyValue> modelKeyValues,
                                    LinkedList<ModelKeyList> modelKeyLists,
                                    LinkedList<ModelKeyMap> modelKeyMaps,
                                    LinkedList<Visitable> children) {

        modelKeyValues.addAll(convertASTKeyValues(children.stream()
                .filter(v -> v instanceof ModelKeyValueAST)
                .map(v -> (ModelKeyValueAST) v)
                .collect(Collectors.toList()))
        );

        modelKeyLists.addAll(convertASTKeyLists(children.stream()
                .filter(v -> v instanceof ModelKeyListAST)
                .map(v -> (ModelKeyListAST) v)
                .collect(Collectors.toList()))
        );

        modelKeyMaps.addAll(convertASTKeyMaps(children.stream()
                .filter(v -> v instanceof ModelKeyMapAST)
                .map(v -> (ModelKeyMapAST) v)
                .collect(Collectors.toList()))
        );
    }

    private void convertElements(LinkedList<ValueType> values,
                                 LinkedList<ListType> lists,
                                 LinkedList<MapType> maps,
                                 LinkedList<Visitable> children) {

        values.addAll(convertASTValues(children.stream()
                .filter(v -> v instanceof ValueTypeAST)
                .map(v -> (ValueTypeAST) v)
                .collect(Collectors.toList()))
        );

        lists.addAll(convertASTLists(children.stream()
                .filter(v -> v instanceof ListTypeAST)
                .map(v -> (ListTypeAST) v)
                .collect(Collectors.toList()))
        );

        maps.addAll(convertASTMaps(children.stream()
                .filter(v -> v instanceof MapTypeAST)
                .map(v -> (MapTypeAST) v)
                .collect(Collectors.toList()))
        );
    }

    private List<OutputAST> getOutputNodes(List<ASTNode> nodes) {
        return nodes.stream()
                .filter(o -> o instanceof OutputAST)
                .map(o -> (OutputAST) o)
                .collect(Collectors.toList());
    }

}
