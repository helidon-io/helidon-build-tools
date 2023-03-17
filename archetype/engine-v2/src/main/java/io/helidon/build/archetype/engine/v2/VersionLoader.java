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

package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.helidon.build.archetype.engine.v2.ast.Version;
import io.helidon.build.common.maven.VersionRange;
import io.helidon.build.common.xml.SimpleXMLParser;
import io.helidon.build.common.xml.SimpleXMLParser.Reader;

/**
 * Helidon versions loader.
 * XML reader for Helidon versions document with caching.
 */
public class VersionLoader {

    private static final Map<FileSystem, List<Version>> VERSIONS = new WeakHashMap<>();

    private VersionLoader() {
    }

    /**
     * Get or load the list of Helidon versions for the given archetype.
     *
     * @param archetype archetype
     * @return list of Helidon versions
     */
    public static List<Version> load(FileSystem archetype) {
        return VERSIONS.computeIfAbsent(archetype, VersionLoader::versions);
    }

    private static List<Version> versions(FileSystem archetype) {
        try {
            Path path = archetype.getPath("versions.xml");
            InputStream is = Files.newInputStream(path);
            return new ReaderImpl().read(is);
        } catch (IOException ex) {
            return List.of();
        }
    }

    private static final class ReaderImpl implements Reader {

        private final List<Version> versions = new ArrayList<>();
        private Version.Builder builder;
        private final LinkedList<String> nameStack = new LinkedList<>();

        private ReaderImpl() {
        }

        List<Version> read(InputStream is) throws IOException {
            SimpleXMLParser parser = SimpleXMLParser.create(is, this);
            parser.parse();
            return versions;
        }

        @Override
        public void startElement(String name, Map<String, String> attributes) {
            if ("version".equals(name)) {
                builder = Version.builder();
            }
            nameStack.push(name);
        }

        @Override
        public void endElement(String name) {
            if ("version".equals(name)) {
                Version version = builder.build();
                versions.add(version);
            }
            nameStack.pop();
        }

        @Override
        public void elementText(String data) {
            String name = nameStack.peek();
            if (name != null) {
                if ("id".equals(name)) {
                    builder.id(data);
                } else if ("supported-cli".equals(name)) {
                    if (data != null && !data.isEmpty()) {
                        builder.supportedCli(VersionRange.createFromVersionSpec(data));
                    }
                }
            }
        }

    }
}
