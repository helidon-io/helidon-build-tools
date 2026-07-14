/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.helidon.build.common.SubstitutionVariables;
import io.helidon.build.common.SubstitutionVariables.NotFoundAction;
import io.helidon.build.common.xml.XMLElement;

/**
 * Stager configuration preprocessor.
 */
final class ConfigProcessor {

    private final XMLElement root;
    private final Path baseDir;
    private final Function<String, String> propertyResolver;

    private ConfigProcessor(XMLElement root, Path baseDir, Function<String, String> propertyResolver) {
        this.root = root;
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir").toAbsolutePath().normalize();
        this.propertyResolver = propertyResolver;
    }

    /**
     * Read and preprocess the stager configuration.
     * @param root XML root
     * @param baseDir base directory
     * @param propertyResolver property resolver
     * @return effective XMLElement
     */
    static XMLElement process(XMLElement root, Path baseDir, Function<String, String> propertyResolver) {
        validate(root);
        ConfigProcessor processor = new ConfigProcessor(root, baseDir, propertyResolver);
        return processor.process();
    }

    private XMLElement process() throws IllegalStateException {
        // use a copy
        XMLElement config = XMLElement.builder(root);

        // pre-process includes
        config.visit(new IncludeVisitor());

        // interpolate properties
        interpolate(config);

        // build the final config
        XMLElement.Builder directories = XMLElement.builder().name("directories");

        // merge variables
        List<XMLElement> variables = new ArrayList<>();
        variables.addAll(config.childrenAt("variables", "variable"));
        variables.addAll(config.childrenAt("directories", "variables", "variable"));
        Map<String, XMLElement> mergedVariables = new LinkedHashMap<>();
        for (XMLElement e : variables) {
            mergedVariables.put(e.attribute("name"), e);
        }

        // add variables
        if (!variables.isEmpty()) {
            XMLElement.Builder merged = XMLElement.builder().name("variables");
            for (XMLElement variable : mergedVariables.values()) {
                variable.parent(merged);
                merged.children().add(variable);
            }
            merged.parent(directories);
            directories.children().add(merged);
        }

        // inline directories
        for (XMLElement directory : config.childrenAt("directories", "directory")) {
            directory.parent(directories);
            directories.children().add(directory);
        }
        return directories;
    }

    private static void validate(XMLElement root) {
        int previous = -1;
        String previousName = null;
        for (XMLElement child : root.children()) {
            int order = switch (child.name()) {
                case "include" -> 0;
                case "properties" -> 1;
                case "variables" -> 2;
                case "directories" -> 3;
                default -> -1;
            };
            if (order >= 0) {
                if (order < previous) {
                    throw new IllegalStateException(
                            "Invalid element order: <%s> must appear before <%s> in %s"
                                    .formatted(child.name(), previousName, child.location()));
                }
                previous = order;
                previousName = child.name();
            }
        }
    }

    private void interpolate(XMLElement config) {
        // collect properties
        Map<String, String> properties = new LinkedHashMap<>();
        for (XMLElement propertiesElt : config.children("properties")) {
            for (XMLElement property : propertiesElt.children()) {
                if ("property".equals(property.name())) {
                    properties.put(property.attribute("name"), property.attribute("value"));
                } else {
                    properties.put(property.name(), property.value());
                }
            }
        }

        SubstitutionVariables substitution = SubstitutionVariables.of(NotFoundAction.AsIs, k -> {
            String value = properties.get(k);
            if (value == null) {
                value = propertyResolver.apply(k);
            }
            return value;
        });
        config.visit(new XMLElement.Visitor() {
            @Override
            public void visitElement(XMLElement elt) {
                elt.value(substitution.resolve(elt.value()));
                elt.attributes().replaceAll((key, value) -> substitution.resolve(value));
            }
        });
    }

    private class IncludeVisitor implements XMLElement.Visitor {

        private final Deque<IncludeFrame> includeFrames = new ArrayDeque<>();

        @Override
        public void visitElement(XMLElement elt) {
            // pre-process includes
            if (isInclude(elt)) {
                String source = resolveSource(elt);
                Path file = resolveFile(source, elt);
                XMLElement resolved = XMLElement.read(file, source, false);
                validate(resolved);

                // add resolved elements as children of the "include" node
                // include node is "inlined" during post visit
                for (XMLElement e : resolved.children()) {
                    e.parent(elt);
                    elt.children().add(e);
                }
            }
        }

        @Override
        public void postVisitElement(XMLElement elt) {
            if (isInclude(elt)) {
                includeFrames.removeLast();

                // inline children and remove the "include" node
                List<XMLElement> siblings = elt.parent().children();
                int index = siblings.indexOf(elt);
                siblings.remove(index);
                List<XMLElement> children = elt.children();
                for (int i = 0; i < children.size(); i++) {
                    XMLElement e = children.get(i);
                    e.parent(elt.parent());
                    siblings.add(index + i, e);
                }
            }
        }

        String resolveSource(XMLElement elt) {
            String src = elt.attribute("src", null);
            if (src == null || src.isBlank()) {
                throw new IllegalStateException(
                        "Missing required 'src' attribute for include in " + elt.location());
            }
            Path path = includePath(src);
            if (includeFrames.stream().anyMatch(it -> it.path.equals(path))) {
                throw new IllegalStateException(
                        "Include cycle detected: %s -> %s"
                                .formatted(includeChain(), elt.location()));
            }
            includeFrames.addLast(new IncludeFrame(elt, path));
            return path.toString();
        }

        Path includePath(String src) {
            IncludeFrame frame = includeFrames.peekLast();
            Path path = null;
            if (frame != null) {
                path = frame.path.getParent();
            }
            if (path == null) {
                path = Path.of("");
            }
            return path.resolve(src).normalize();
        }

        Path resolveFile(String source, XMLElement elt) {
            Path includeFile;
            Path path = Path.of(source);
            if (path.isAbsolute()) {
                includeFile = path.normalize();
            } else {
                includeFile = baseDir.resolve(path).normalize();
            }
            if (!Files.isRegularFile(includeFile) || !Files.isReadable(includeFile)) {
                throw new IllegalStateException(
                        "Missing or unreadable include '%s' referenced from %s"
                                .formatted(source, elt.location()));
            }
            return includeFile;
        }

        String includeChain() {
            return includeFrames.stream()
                    .map(it -> it.elt.location().toString())
                    .collect(Collectors.joining(" -> "));
        }

        static boolean isInclude(XMLElement elt) {
            if (!"include".equals(elt.name())) {
                return false;
            }
            XMLElement parent = elt.parent();
            if (parent == null) {
                return false;
            }
            while (parent.parent() != null) {
                if (!"include".equals(parent.name())) {
                    return false;
                }
                parent = parent.parent();
            }
            return true;
        }

        record IncludeFrame(XMLElement elt, Path path) {
        }
    }
}
