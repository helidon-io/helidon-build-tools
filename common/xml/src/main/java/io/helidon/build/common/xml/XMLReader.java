/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.common.xml;

import java.util.Map;

/**
 * XML Reader.
 */
public interface XMLReader {

    /**
     * Receive notification of the start of an element.
     *
     * @param name       the element name
     * @param attributes the element attributes
     * @throws io.helidon.build.common.xml.XMLReaderException if any error occurs
     */
    default void startElement(String name, Map<String, String> attributes) {
    }

    /**
     * Receive notification of the end of an element.
     *
     * @param name the element name
     * @throws io.helidon.build.common.xml.XMLReaderException if any error occurs
     */
    default void endElement(String name) {
    }

    /**
     * Receive notification of text data inside an element.
     *
     * @param data the text data
     * @throws io.helidon.build.common.xml.XMLReaderException if any error occurs
     */
    default void elementText(String data) {
    }

    /**
     * Continue action, can be overridden to stop parsing.
     *
     * @return {@code true} to keep parsing, {@code false} to stop parsing
     */
    default boolean keepParsing() {
        return true;
    }

    /**
     * Receive notification of content data inside a processing instruction element.
     *
     * @param data   the content data of a processing instruction
     * @param target the name of an application to which the instruction is directed
     * @throws io.helidon.build.common.xml.XMLReaderException if any error occurs
     */
    default void processingInstruction(String target, String data) {
    }
}
