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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;

/**
 * SAX helper class.
 */
final class SAXHelper {

    private SAXHelper() {
    }

    /**
     * Validate that a child element has a given name.
     * @param child expected child name
     * @param parent parent name
     * @param qName element name to be compared
     * @throws IllegalStateException if the child name does not match qName
     */
    static void validateChild(String child, String parent, String qName) throws IllegalStateException {
        if (!child.equals(qName)) {
            throw new IllegalStateException("Invalid child for '" + parent + "' node: '" + qName + "'");
        }
    }

    /**
     * Read an attribute and fallback to a default value if not present.
     * @param name attribute name
     * @param qName element name
     * @param attr attributes
     * @param defaultValue the fallback value, may be {@code null}
     * @return attribute value, may be {@code null} if fallback is null
     */
    static String readAttribute(String name, String qName, Attributes attr, String defaultValue) {
        String value = attr.getValue(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Read a required attribute.
     * @param name attribute name
     * @param qName element name
     * @param attr attributes
     * @return attribute value, never {@code null}
     * @throws IllegalStateException if the attribute is not found
     */
    static String readRequiredAttribute(String name, String qName, Attributes attr)
            throws IllegalStateException {

        String value = attr.getValue(name);
        if (value == null) {
            throw new IllegalStateException("Missing required attribute '" + name + "' for element: '" + qName + "'");
        }
        return value;
    }

    /**
     * Read an attribute as a comma separate list.
     * @param name attribute name
     * @param qName element name
     * @param attr attributes
     * @return list of values, empty if the attribute is not found
     */
    static List<String> readAttributeList(String name, String qName, Attributes attr) {
        String value = attr.getValue(name);
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> values = new LinkedList<>();
        for (String item : value.split(",")) {
            values.add(item);
        }
        return values;
    }
}
