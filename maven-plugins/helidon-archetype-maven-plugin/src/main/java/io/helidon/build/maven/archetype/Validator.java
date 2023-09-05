/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.helidon.build.archetype.engine.v2.util.ArchetypeValidator;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static io.helidon.build.maven.archetype.Schema.ROOT_ELEMENT;

/**
 * Validation operations.
 */
class Validator {

    private static final Pattern SCHEMA_PATTERN =
            Pattern.compile("(/archetype/)(?<version>[\\w-.]+)(\\s+\\S+/)(?<location>[\\w-.]+)");
    private static final String SCHEMA_LOCATION_ATTR_NAME = "xsi:schemaLocation";
    private static final String SCHEMA_NAMESPACE_ATTR_NAME = "xmlns";

    private Validator() {
    }

    static Schema.Info validateSchema(Map<String, Schema> schemaMap, Path file) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(Files.newInputStream(file));
            if (doc.getDocumentElement().getNodeName().equals(ROOT_ELEMENT)) {
                String schemaLocation = doc.getDocumentElement().getAttribute(SCHEMA_LOCATION_ATTR_NAME);
                String schemaNamespace = doc.getDocumentElement().getAttribute(SCHEMA_NAMESPACE_ATTR_NAME);
                Matcher matcher = SCHEMA_PATTERN.matcher(schemaLocation);
                String schemaVersion = null;
                String schemaFile = null;
                if (matcher.find()) {
                    schemaVersion = matcher.group("version");
                    schemaFile = matcher.group("location");
                }
                if (schemaVersion == null || schemaFile == null) {
                    var message = String.format("File %s does not contain required data in xsi:schemaLocation : version - %s, "
                            + "location - %s", file, schemaVersion, schemaFile);
                    throw new Schema.ValidationException(message);
                }
                schemaMap.get(schemaFile).validate(file, doc);
                return new Schema.Info(schemaVersion, schemaLocation, schemaNamespace);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }



    /**
     * Validate archetype regular expression.
     *
     * @param script archetype script
     * @return list of errors
     */
    static List<String> validateArchetype(Path script) {
        List<String> errors = ArchetypeValidator.validate(script);
        List<String> regexErrors = RegexValidator.validate(script);
        errors.addAll(regexErrors);
        return errors;
    }

    /**
     * Validate generated files using {@link io.helidon.build.maven.archetype.config.Validation} configuration.
     *
     * @param directory   generated file directory
     * @param validations validations
     * @throws MojoExecutionException if validation mismatch
     */
    static void validateProject(File directory,
                                List<io.helidon.build.maven.archetype.config.Validation> validations)
            throws MojoExecutionException {
        if (Objects.isNull(validations)) {
            return;
        }
        for (io.helidon.build.maven.archetype.config.Validation validation : validations) {
            validation.validate(directory);
        }
    }

}
