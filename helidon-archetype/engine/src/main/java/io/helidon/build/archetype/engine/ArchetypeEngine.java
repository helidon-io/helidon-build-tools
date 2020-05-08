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
package io.helidon.build.archetype.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeDescriptor.Conditional;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.FileSet;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.FileSets;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Property;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Replacement;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.TemplateSets;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Transformation;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * Archetype engine.
 */
public final class ArchetypeEngine {

    /**
     * Constant for the archetype descriptor path.
     */
    public static final String DESCRIPTOR_RESOURCE_NAME = "META-INF/helidon-archetype.xml";

    /**
     * Constant for the archetype resources manifest path.
     */
    public static final String RESOURCES_LIST = "META-INF/helidon-archetype-resources.txt";

    private final MustacheFactory mf;
    private final ClassLoader cl;
    private final ArchetypeDescriptor descriptor;
    private final Map<String, String> properties;
    private final Map<String, List<Transformation>> templates;
    private final Map<String, List<Transformation>> files;

    /**
     * Create a new archetype engine instance.
     * @param archetype archetype file
     * @param properties user properties
     * @throws MalformedURLException if an error occurred converting the file to a URL
     */
    public ArchetypeEngine(File archetype, Map<String, String> properties) throws MalformedURLException {
        this(new URLClassLoader(new URL[] {archetype.toURI().toURL()}), properties);
    }

    /**
     * Create a new archetype engine instance.
     * @param cl class loader used to load the archetype
     * @param properties a mutable map of properties referenced by this engine
     */
    public ArchetypeEngine(ClassLoader cl, Map<String, String> properties) {
        this.cl = Objects.requireNonNull(cl, "class-loader is null");
        this.mf = new DefaultMustacheFactory();
        this.descriptor = loadDescriptor(cl);
        Objects.requireNonNull(properties, "properties is null");
        descriptor.properties().stream()
                .filter(p -> p.defaultValue().isPresent() && !properties.containsKey(p.id()))
                .forEach(p -> properties.put(p.id(), p.defaultValue().get()));
        this.properties = properties;
        List<SourcePath> paths = loadResourcesList(cl);
        this.templates = resolveFileSets(descriptor.templateSets().map(TemplateSets::templateSets).orElseGet(LinkedList::new),
                descriptor.templateSets().map(TemplateSets::transformations).orElseGet(Collections::emptyList), paths,
                properties);
        this.files = resolveFileSets(descriptor.fileSets().map(FileSets::fileSets).orElseGet(LinkedList::new),
                descriptor.fileSets().map(FileSets::transformations).orElseGet(Collections::emptyList), paths, properties);
    }

    /**
     * Return archetype descriptor.
     *
     * @return Archetype descriptor.
     */
    public ArchetypeDescriptor descriptor() {
        return descriptor;
    }

    private static ArchetypeDescriptor loadDescriptor(ClassLoader cl) {
        InputStream descIs = cl.getResourceAsStream(DESCRIPTOR_RESOURCE_NAME);
        if (descIs == null) {
            throw new IllegalStateException(DESCRIPTOR_RESOURCE_NAME + " not found in class-path");
        }
        return ArchetypeDescriptor.read(descIs);
    }

    private static List<SourcePath> loadResourcesList(ClassLoader cl) {
        InputStream rListIs = cl.getResourceAsStream(RESOURCES_LIST);
        if (rListIs == null) {
            throw new IllegalStateException(RESOURCES_LIST + " not found in class-path");
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(rListIs))) {
            return br.lines().map(SourcePath::new).collect(Collectors.toList());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Map<String, List<Transformation>> resolveFileSets(List<FileSet> fileSets, List<Transformation> transformations,
            List<SourcePath> paths, Map<String, String> properties) {

        Map<String, List<Transformation>> resolved = new HashMap<>();
        for (FileSet fileSet : fileSets) {
            if (evaluateConditional(fileSet, properties)) {
                List<Transformation> allTransformations = new LinkedList<>(transformations);
                allTransformations.addAll(fileSet.transformations());
                for (SourcePath path : SourcePath.filter(paths, fileSet.includes(), fileSet.excludes())) {
                    String filteredPath = path.asString();
                    String dir = fileSet.directory().orElse(null);
                    if (dir == null || dir.isEmpty()) {
                        continue;
                    }
                    String dirPath = new SourcePath(dir).asString();
                    if (filteredPath.startsWith(dirPath)) {
                        resolved.put(path.asString(), allTransformations);
                    }
                }
            }
        }
        return resolved;
    }

    /**
     * Transform a string with transformations.
     * @param input input to be transformed
     * @param transformations transformed to apply
     * @param properties properties values
     * @return transformation result
     */
    static String transform(String input, List<Transformation> transformations, Map<String, String> properties) {
        String output = input;
        List<Replacement> replacements = transformations.stream()
                .flatMap((t) -> t.replacements().stream())
                .collect(Collectors.toList());
        for (Replacement rep : replacements) {
            String replacement = PropertyEvaluator.evaluate(rep.replacement(), properties);
            output = output.replaceAll(rep.regex(), replacement);
        }
        return output;
    }

    /**
     * Resolve a {@link Conditional} object.
     * @param conditional object to resolve
     * @param props properties used to resolve the value of the declared properties
     * @return evaluation results
     */
    static boolean evaluateConditional(Conditional conditional, Map<String, String> props) {
        List<Property> ifProps = conditional.ifProperties();
        if (ifProps == null) {
            ifProps = Collections.emptyList();
        }
        for (Property prop : ifProps) {
            if (!Boolean.valueOf(props.get(prop.id()))) {
                return false;
            }
        }
        List<Property> unlessProps = conditional.unlessProperties();
        if (unlessProps == null) {
            unlessProps = Collections.emptyList();
        }
        for (Property prop : unlessProps) {
            if (Boolean.valueOf(props.get(prop.id()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Run the archetype.
     * @param outputDirectory output directory
     */
    public void generate(File outputDirectory) {
        for (Entry<String, List<Transformation>> entry : templates.entrySet()) {
            String resourcePath = entry.getKey().substring(1);
            InputStream is = cl.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IllegalStateException(resourcePath + " not found in class-path");
            }
            Mustache m = mf.compile(new InputStreamReader(is), resourcePath);
            File outputFile = new File(outputDirectory, transform(resourcePath, entry.getValue(), properties));
            outputFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(outputFile)) {
                m.execute(writer, properties).flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        for (Entry<String, List<Transformation>> entry : files.entrySet()) {
            String resourcePath = entry.getKey().substring(1);
            InputStream is = cl.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IllegalStateException(resourcePath + " not found in class-path");
            }
            File outputFile = new File(outputDirectory, transform(resourcePath, entry.getValue(), properties));
            outputFile.getParentFile().mkdirs();
            try {
                Files.copy(is, outputFile.toPath());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
