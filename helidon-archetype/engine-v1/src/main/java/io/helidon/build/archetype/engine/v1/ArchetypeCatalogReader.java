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
import java.util.LinkedList;
import java.util.Map;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;

/**
 * {@link ArchetypeCatalog} reader.
 */
final class ArchetypeCatalogReader implements SimpleXMLParser.Reader {

    private String modelVersion;
    private String name;
    private String groupId;
    private String version;
    private final LinkedList<ArchetypeEntry> entries;
    private final LinkedList<String> stack;

    private ArchetypeCatalogReader() {
        entries = new LinkedList<>();
        stack = new LinkedList<>();
    }

    /**
     * Read the catalog from the given input stream.
     *
     * @param is input stream
     * @return catalog, never {@code null}
     */
    static ArchetypeCatalog read(InputStream is) {
        try {
            ArchetypeCatalogReader reader = new ArchetypeCatalogReader();
            SimpleXMLParser.parse(is, reader);
            return new ArchetypeCatalog(reader.modelVersion, reader.name, reader.groupId, reader.version, reader.entries);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public void startElement(String qName, Map<String, String> attributes) {
        String parent = stack.peek();
        if (parent == null) {
            if (!"archetype-catalog".equals(qName)) {
                throw new IllegalStateException("Invalid root element '" + qName + "'");
            }
            modelVersion = readRequiredAttribute("modelVersion", qName, attributes);
            name = readRequiredAttribute("name", qName, attributes);
            groupId = readRequiredAttribute("groupId", qName, attributes);
            version = readRequiredAttribute("version", qName, attributes);
            stack.push("archetype-catalog");
        } else {
            switch (parent) {
                case "archetype-catalog":
                    switch (qName) {
                        case "archetype":
                            validateChild("archetype", parent, qName);
                            entries.add(new ArchetypeEntry(
                                    readAttribute("groupId", qName, attributes, groupId),
                                    readRequiredAttribute("artifactId", qName, attributes),
                                    readAttribute("version", qName, attributes, version),
                                    readRequiredAttribute("name", qName, attributes),
                                    readRequiredAttribute("title", qName, attributes),
                                    readRequiredAttribute("summary", qName, attributes),
                                    attributes.get("description"),
                                    readAttributeList("tags", qName, attributes)
                            ));
                            stack.push(qName);
                            break;
                        default:
                            throw new IllegalStateException("Invalid top-level element: " + qName);
                    }
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
    public void elementText(String data) {
        // all data is in the attributes
    }
}
