/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.io.Writer;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.common.xml.XMLGenerator;

/**
 * Script-writer.
 */
public class XMLScriptWriter implements Script.Writer {
    private final XMLGenerator generator;

    /**
     * Create a new instance.
     *
     * @param writer writer
     * @param pretty pretty
     */
    public XMLScriptWriter(Writer writer, boolean pretty) {
        this.generator = new XMLGenerator(writer, pretty);
    }

    @Override
    public void close() {
        generator.close();
    }

    @Override
    public void writeScript(Node node) {
        generator.prolog().startElement("archetype-script");
        generator.attribute("xmlns", "https://helidon.io/archetype/2.0");
        generator.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        generator.attribute("xsi:schemaLocation", "https://helidon.io/archetype/2.0 https://helidon.io/xsd/archetype-2.0.xsd");
        Map<String, Node> methods = node.script().methods();
        if (!methods.isEmpty()) {
            generator.startElement("methods");
            methods.keySet().stream().sorted().forEach(k -> {
                generator.startElement("method");
                generator.attribute("name", k);
                writeDirectives(methods.get(k));
                generator.endElement();
            });
            generator.endElement();
        }
        writeDirectives(node);
        generator.endElement();
    }

    private void writeDirectives(Node block) {
        block.visit(new Node.Visitor() {
            @Override
            public boolean visit(Node node) {
                if (node != block && node.kind() != Kind.CONDITION) {
                    generator.startElement(node.kind().token());
                    node.attributes().forEach((k, v) -> {
                        switch (k) {
                            // written as a child element
                            case "help":
                            case "directory":
                                return;
                            default:
                                generator.attribute(k, Value.toString(v));
                        }
                    });
                    Node parent = node.parent();
                    if (parent.kind() == Kind.CONDITION) {
                        generator.attribute("if", parent.expression().literal());
                    }
                    writeValue(node);
                    node.attribute("directory").asString().ifPresent(v -> writeElement("directory", v));
                    node.attribute("help").asString().ifPresent(v -> writeElement("help", v));
                }
                return true;
            }

            @Override
            public void postVisit(Node node) {
                if (node != block && node.kind() != Kind.CONDITION) {
                    generator.endElement();
                }
            }
        });
    }

    private void writeValue(Node node) {
        switch (node.kind()) {
            case INPUT_OPTION:
                node.value().ifPresent(v -> generator.attribute("value", v));
                break;
            case PRESET_BOOLEAN:
            case VARIABLE_BOOLEAN:
                node.value().asBoolean().ifPresent(generator::value);
                break;
            case PRESET_LIST:
            case VARIABLE_LIST:
                for (String v : node.value().asList().orElse(List.of())) {
                    writeElement("value", v);
                }
                break;
            case REGEX:
            case FILE:
            case INCLUDE:
            case EXCLUDE:
            case MODEL_VALUE:
            case PRESET_ENUM:
            case VARIABLE_ENUM:
            case PRESET_TEXT:
            case VARIABLE_TEXT:
                node.value().asString().ifPresent(v -> {
                    if (v.isEmpty()
                        || v.matches("^\\s+")
                        || v.matches("\\s+$")
                        || v.contains("\n")
                        || v.contains("&")
                        || v.contains(">")
                        || v.contains("<")
                        || v.contains("'")
                        || v.contains("\"")) {
                        generator.cdata(v);
                    } else {
                        generator.value(v);
                    }
                });
                break;
            default:
        }
    }

    private void writeElement(String name, String value) {
        generator.startElement(name);
        generator.value(value);
        generator.endElement();
    }
}
