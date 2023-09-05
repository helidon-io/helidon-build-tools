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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static io.helidon.build.common.Unchecked.unchecked;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Archetype V2 schema utility.
 */
class Schema {

    /**
     * Default schema location.
     */
    static final String DEFAULT_LOCATION = "https://helidon.io/archetype/2.0 https://helidon.io/xsd/archetype-2.0.xsd";

    /**
     * Default schema namespace.
     */
    static final String DEFAULT_NAMESPACE = "https://helidon.io/archetype/2.0";

    /**
     * Default schema version.
     */
    static final String DEFAULT_VERSION = "2.0";

    /**
     * XML root element.
     */
    static final String ROOT_ELEMENT = "archetype-script";

    /**
     * Default archetype schema.
     */
    static final String DEFAULT_RESOURCE_NAME = "schemas/archetype-2.0.xsd";

    private final Validator validator;


    /**
     * Create a new validator.
     *
     * @param is schema input stream, must be non {@code null}
     */
    Schema(InputStream is) {
        Objects.requireNonNull(is, "schema input stream is null");
        SchemaFactory factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        try {
            javax.xml.validation.Schema schema = factory.newSchema(new StreamSource(is));
            validator = schema.newValidator();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validate the XML file at the given input stream.
     *
     * @param file file to validate
     */
    @SuppressWarnings("unchecked")
    void validate(Path file) {
        validate(unchecked(() -> Files.newInputStream(file)));
    }

    /**
     * Validate the XML file at the given input stream.
     *
     * @param supplier input stream supplier
     */
    void validate(Supplier<InputStream> supplier) throws ValidationException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(supplier.get());
            if (doc.getDocumentElement().getNodeName().equals(ROOT_ELEMENT)) {
                validator.validate(new StreamSource(supplier.get()));
            }
        } catch (SAXParseException ex) {
            throw new ValidationException(ex);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    void validate(Path file, Document doc) throws ValidationException {
        try {
            if (doc.getDocumentElement().getNodeName().equals(ROOT_ELEMENT)) {
                validator.validate(new StreamSource(Files.newInputStream(file)));
            }
        } catch (SAXParseException ex) {
            throw new ValidationException(ex);
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validation exception.
     */
    static final class ValidationException extends RuntimeException {

        private final int lineNo;
        private final int colNo;

        private ValidationException(SAXParseException cause) {
            super(cause.getMessage(), cause);
            lineNo = cause.getLineNumber();
            colNo = cause.getColumnNumber();
        }

        ValidationException(String message) {
            super(message);
            lineNo = -1;
            colNo = -1;
        }

        /**
         * Get the column number.
         *
         * @return column number
         */
        public int colNo() {
            return colNo;
        }

        /**
         * Get the line number.
         *
         * @return line number
         */
        public int lineNo() {
            return lineNo;
        }
    }

    static class Info {
        private final String version;
        private final String location;
        private final String namespace;

        Info(String version, String location, String namespace) {
            this.version = version;
            this.location = location;
            this.namespace = namespace;
        }

        public String namespace() {
            return namespace;
        }

        String version() {
            return version;
        }

        String schemaLocation() {
            return location;
        }
    }
}
