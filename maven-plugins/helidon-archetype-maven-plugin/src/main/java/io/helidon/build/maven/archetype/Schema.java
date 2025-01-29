/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Archetype V2 schema utility.
 */
class Schema {

    private static final String RESOURCE_NAME = "schema-2.0.xsd";

    private final javax.xml.validation.Schema schema;

    /**
     * Create a new instance.
     */
    Schema() {
        try {
            URL url = getClass().getClassLoader().getResource(RESOURCE_NAME);
            if (url == null) {
                throw new IllegalStateException("Unable to resolve resource: " + RESOURCE_NAME);
            }
            InputStream is = url.openStream();
            SchemaFactory factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
            schema = factory.newSchema(new StreamSource(is));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Validate the given XML file.
     *
     * @param is       input stream
     * @param sourceId source id
     * @return errors
     */
    List<String> validate(InputStream is, String sourceId) {
        try {
            Validator validator = schema.newValidator();
            List<String> errors = new ArrayList<>();
            validator.setErrorHandler(new ErrorHandlerImpl(errors, sourceId));
            validator.validate(new StreamSource(is));
            return errors;
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record ErrorHandlerImpl(List<String> errors, String file) implements ErrorHandler {

        @Override
        public void warning(SAXParseException ex) {
            errors.add(toString(ex));
        }

        @Override
        public void error(SAXParseException ex) {
            errors.add(toString(ex));
        }

        @Override
        public void fatalError(SAXParseException ex) {
            errors.add(toString(ex));
        }

        String toString(SAXParseException ex) {
            return String.format("Schema validation error: file=%s, position=%s:%s, error=%s",
                    file,
                    ex.getLineNumber(),
                    ex.getColumnNumber(),
                    ex.getMessage());
        }
    }
}
