/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.cli.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

import io.helidon.build.common.maven.VersionRange;
import io.helidon.build.common.xml.SimpleXMLParser;
import io.helidon.build.common.xml.SimpleXMLParser.Reader;

/**
 * Loader for {@link ArchetypesData}.
 * XML reader for archetypes data document.
 */
public class ArchetypesDataLoader {

    private static final Map<FileSystem, ArchetypesData> ARCHETYPES_DATA_MAP = new WeakHashMap<>();

    private ArchetypesDataLoader() {
    }

    /**
     * Get the data about archetype versions for the given archetype.
     *
     * @param archetype archetype
     * @return data about archetype versions
     */
    public static ArchetypesData load(FileSystem archetype) {
        return ARCHETYPES_DATA_MAP.computeIfAbsent(archetype, ArchetypesDataLoader::archetypesData);
    }

    private static ArchetypesData archetypesData(FileSystem archetype) {
        try {
            Path path = archetype.getPath("versions.xml");
            InputStream is = Files.newInputStream(path);
            return new ReaderImpl().read(is);
        } catch (IOException ex) {
            return ArchetypesData.builder().build();
        }
    }

    private static final class ReaderImpl implements Reader {

        private ArchetypesData.Builder builder = ArchetypesData.builder();
        private final LinkedList<String> nameStack = new LinkedList<>();

        private ReaderImpl() {
        }

        ArchetypesData read(InputStream is) throws IOException {
            SimpleXMLParser parser = SimpleXMLParser.create(is, this);
            parser.parse();
            return builder.build();
        }

        @Override
        public void startElement(String name, Map<String, String> attributes) {
            if (nameStack.isEmpty() && !"data".equals(name)) {
                throw new SimpleXMLParser.XMLReaderException(String.format("Invalid root element '%s'. Must be 'data'", name));
            }
            String parentName = nameStack.peek();
            if (parentName != null) {
                switch (name) {
                    case "rules":
                    case "archetypes":
                        checkParent("data", parentName, name);
                        break;
                    case "versions":
                        checkParent("archetypes", parentName, name);
                        break;
                    case "rule":
                        checkParent("rules", parentName, name);
                        processRule(attributes);
                        break;
                    default:
                        break;
                }
            }
            nameStack.push(name);
        }

        private void processRule(Map<String, String> attributes) {
            var archRule = VersionRange.createFromVersionSpec(attributes.get("archetype"));
            var cliRule = VersionRange.createFromVersionSpec(attributes.get("cli"));
            builder.addRule(new ArchetypesData.Rule(archRule, cliRule));
        }

        private void checkParent(String expectedParent, String parent, String current) {
            if (!expectedParent.equals(parent)) {
                throw new SimpleXMLParser.XMLReaderException(
                        String.format("Invalid parent element '%s' for element '%s'. Must be '%s'",
                                parent, current, expectedParent));
            }
        }

        @Override
        public void endElement(String name) {
            nameStack.pop();
        }

        @Override
        public void elementText(String data) {
            String name = nameStack.peek();
            if (name != null) {
                if ("version".equals(name)) {
                    builder.addVersion(data);
                }
            }
        }

    }
}
