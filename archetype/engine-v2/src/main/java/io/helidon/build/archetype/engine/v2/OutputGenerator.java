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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;
import io.helidon.build.archetype.engine.v2.descriptor.ListType;
import io.helidon.build.archetype.engine.v2.descriptor.MapType;
import io.helidon.build.archetype.engine.v2.descriptor.Model;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyList;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyMap;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.descriptor.Replacement;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;
import io.helidon.build.archetype.engine.v2.interpreter.ASTNode;
import io.helidon.build.archetype.engine.v2.interpreter.FileSetAST;
import io.helidon.build.archetype.engine.v2.interpreter.FileSetsAST;
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
    private final List<OutputAST> nodes;
    private final List<TransformationAST> transformations;
    private final List<TemplateAST> template;
    private final List<TemplatesAST> templates;
    private final List<FileSetAST> file;
    private final List<FileSetsAST> files;

    /**
     * OutputGenerator constructor.
     *
     * @param outputNodes   Output node from interpreter
     */
    OutputGenerator(List<ASTNode> outputNodes) {
        Objects.requireNonNull(outputNodes, "Output nodes are null");

        this.nodes = getOutputNodes(outputNodes);
        this.model = createUniqueModel();

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

    /**
     * Generate output files.
     *
     * @param outputDirectory   Output directory where the files will be generated
     */
    public void generate(File outputDirectory, Archetype archetype) throws IOException {
        Objects.requireNonNull(outputDirectory, "output directory is null");

        for (TemplateAST templateAST : template) {
            File outputFile = new File(outputDirectory, templateAST.target());
            outputFile.getParentFile().mkdirs();
            if (templateAST.engine().equals("mustache")) {
                MustacheHandler.renderMustacheTemplate(
                        new FileInputStream(archetype.getFile(templateAST.source()).toFile()),
                        templateAST.source(),
                        new FileOutputStream(outputFile),
                        model);
            } else {
                Files.copy(new FileInputStream(archetype.getFile(templateAST.source()).toFile()), outputFile.toPath());
            }
        }

        for (TemplatesAST templatesAST : templates) {
            Path root = Path.of(templatesAST.directory());
            TemplateModel templatesModel = createTemplatesModel(templatesAST);
            List<String> includes = new LinkedList<>(templatesAST.includes());
            for (String include : templatesAST.includes()) {
                if (include.contains("**/*")) {
                    String extension = include.substring(include.lastIndexOf("."));
                    try {
                        URL url = OutputGenerator.class.getClassLoader().getResource(root.toString());
                        if (url != null) {
                            List<String> includePaths = readMultipleInclude(new File(url.toURI()), extension);
                            includes.addAll(includePaths.stream()
                                    .filter(e -> !templatesAST.excludes().contains(e))
                                    .collect(Collectors.toList())
                            );
                        }
                    } catch (URISyntaxException e) {
                        throw new IOException("Cannot find the templates directory " + root);
                    }
                }
                includes.remove(include);
            }

            for (String include : includes) {
                if (templatesAST.excludes().contains(include)) {
                    continue;
                }
                String includePath = root.resolve(include).toString();
                String outPath = transform(include, templatesAST.transformation());
                File outputFile = new File(outputDirectory, outPath);
                outputFile.getParentFile().mkdirs();
                MustacheHandler.renderMustacheTemplate(
                        new FileInputStream(archetype.getFile(includePath).toFile()),
                        outPath,
                        new FileOutputStream(outputFile),
                        templatesModel);
            }
        }

        for (FileSetAST fileAST : file) {
            File outputFile = new File(outputDirectory, fileAST.target());
            outputFile.getParentFile().mkdirs();
            Files.copy(new FileInputStream(archetype.getFile(fileAST.source()).toFile()), outputFile.toPath());
        }

        for (FileSetsAST filesAST : files) {
            Path root = Path.of(filesAST.directory());
            for (String include : filesAST.includes()) {
                if (filesAST.excludes().contains(include)) {
                    continue;
                }
                String outPath = processTransformation(include, filesAST.transformations());
                File outputFile = new File(outputDirectory, outPath);
                outputFile.getParentFile().mkdirs();
                Files.copy(new FileInputStream(archetype.getFile(root.resolve(include).toString()).toFile()), outputFile.toPath());
            }
        }
    }

    private List<String> readMultipleInclude(File directory, String extension) {
        return Arrays.stream(directory.listFiles())
                .filter(f -> f.getName().contains(extension))
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
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
        List<String> applicable = Arrays.asList(transformation.split(","));
        return processTransformation(input, applicable);
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
            output = output.replaceAll(rep.regex(), rep.replacement());
        }
        return output;
    }

    /**
     * Consume a list of output nodes from interpreter and create a unique template model.
     *
     * @return              Unique template model
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
        convertKeyElements(modelDescriptor.keyValues(), modelDescriptor.keyLists(), modelDescriptor.keyMaps(), model.children());
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
                .filter(v ->  v instanceof ModelKeyValueAST)
                .map(v -> (ModelKeyValueAST) v)
                .collect(Collectors.toList()))
        );

        modelKeyLists.addAll(convertASTKeyLists(children.stream()
                .filter(v ->  v instanceof ModelKeyListAST)
                .map(v -> (ModelKeyListAST) v)
                .collect(Collectors.toList()))
        );

        modelKeyMaps.addAll(convertASTKeyMaps(children.stream()
                .filter(v ->  v instanceof ModelKeyMapAST)
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
