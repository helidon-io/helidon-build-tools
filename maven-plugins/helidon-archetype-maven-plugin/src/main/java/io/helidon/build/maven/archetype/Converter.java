/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

/**
 * Utility to convert maven plugin config DOM to archetype script XML file.
 */
public final class Converter {

    private Converter() {
    }

    /**
     * Convert the given plexus config node to a valid archetype v2 description.
     *
     * @param config plexus config node
     * @param output output file
     * @param schemaNamespace namespace for the archetype schema
     * @param schemaLocation location of the archetype schema
     * @return the output file
     * @throws IOException if an IO error occurs
     */
    static Path convert(PlexusConfiguration config, Path output, String schemaNamespace,
                        String schemaLocation) throws IOException {
        FileWriter writer = new FileWriter(output.toFile());
        Xpp3DomWriter.write(writer, convert(config, schemaNamespace, schemaLocation));
        writer.flush();
        writer.close();
        return output;
    }

    /**
     * Convert the given plexus config node to a valid archetype v2 description with the default namespace and location for the
     * archetype schema.
     *
     * @param config plexus config node
     * @param output output file
     * @return the output file
     * @throws IOException if an IO error occurs
     */
    static Path convert(PlexusConfiguration config, Path output) throws IOException {
        FileWriter writer = new FileWriter(output.toFile());
        Xpp3DomWriter.write(writer, convert(config, Schema.DEFAULT_NAMESPACE, Schema.DEFAULT_LOCATION));
        writer.flush();
        writer.close();
        return output;
    }

    private static Xpp3Dom convert(PlexusConfiguration config, String schemaNamespace, String schemaLocation) {
        Xpp3Dom rootElt = new Xpp3Dom(Schema.ROOT_ELEMENT);
        rootElt.setAttribute("xmlns", schemaNamespace);
        rootElt.setAttribute("xmlns:xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
        rootElt.setAttribute("xsi:schemaLocation", schemaLocation);
        Deque<Node> stack = new ArrayDeque<>();
        for (PlexusConfiguration child : config.getChildren()) {
            stack.add(new Node(rootElt, child));
        }
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            Xpp3Dom newNode = node.convert();
            node.parent.addChild(newNode);
            for (PlexusConfiguration child : node.orig.getChildren()) {
                stack.add(new Node(newNode, child));
            }
        }
        return rootElt;
    }

    private static final class Node {

        private final Xpp3Dom parent;
        private final PlexusConfiguration orig;

        Node(Xpp3Dom parent, PlexusConfiguration orig) {
            this.parent = parent;
            this.orig = orig;
        }

        Xpp3Dom convert() {
            Xpp3Dom newNode = new Xpp3Dom(orig.getName());
            for (String attr : orig.getAttributeNames()) {
                newNode.setAttribute(attr, orig.getAttribute(attr));
            }
            newNode.setValue(orig.getValue());
            return newNode;
        }
    }
}
