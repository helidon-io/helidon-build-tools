/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the standalone stager schema.
 */
class StagerSchemaTest {

    @Test
    void testValidate() {
        assertDoesNotThrow(() -> validate("stager.xml"));
    }

    @Test
    void testValidateExhaustive() {
        assertDoesNotThrow(() -> validate("stager-exhaustive.xml"));
    }

    @Test
    void testValidateFactoryFixture() {
        assertDoesNotThrow(() -> validateResource("test-config.xml"));
    }

    @Test
    void testValidateNegative() {
        assertThrows(SAXException.class, () -> validate("stager-invalid.xml"));
    }

    @Test
    void testValidateUnknownElementFails() {
        assertThrows(SAXException.class, () -> validate("stager-unknown-element.xml"));
    }

    @Test
    void testValidateMissingDownloadUrlFails() {
        assertThrows(SAXException.class, () -> validate("stager-missing-download-url.xml"));
    }

    @Test
    void testValidateNestedIncludeFails() {
        assertThrows(SAXException.class, () -> validate("stager-nested-include-invalid.xml"));
    }

    @Test
    void testValidateOutOfOrderFails() {
        assertThrows(SAXException.class, () -> validate("stager-out-of-order.xml"));
    }

    @Test
    void testValidatePropertiesBeforeIncludeFails() {
        assertThrows(SAXException.class, () -> validate("stager-properties-before-include.xml"));
    }

    @Test
    void testValidateIncludeAfterVariablesFails() {
        assertThrows(SAXException.class, () -> validate("stager-include-after-variables.xml"));
    }

    @Test
    void testValidateTemplateVariablesFail() {
        assertThrows(SAXException.class, () -> validate("stager-template-variables-invalid.xml"));
    }

    @Test
    void testElementsAndAttributesHaveDocumentation() throws Exception {
        try (InputStream is = Files.newInputStream(targetDir(StagerSchemaTest.class)
                     .resolve("classes/schema/stager-1.0.xsd"))) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(is);

            assertDocumented(document, "element");
            assertDocumented(document, "attribute");
        }
    }

    static void validate(String path) throws IOException, SAXException {
        validateResource("schema/" + path);
    }

    static void validateResource(String path) throws IOException, SAXException {
        try (InputStream is = Files.newInputStream(targetDir(StagerSchemaTest.class)
                     .resolve("classes/schema/stager-1.0.xsd"));
             InputStream xml = Files.newInputStream(testResourcePath(StagerSchemaTest.class, path))) {
            SchemaFactory factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
            factory.newSchema(new StreamSource(is))
                    .newValidator()
                    .validate(new StreamSource(xml));
        }
    }

    static void assertDocumented(Document document, String declarationName) {
        NodeList declarations = document.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, declarationName);
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < declarations.getLength(); i++) {
            Element declaration = (Element) declarations.item(i);
            if (!hasDocumentation(declaration)) {
                missing.add(declarationName + " " + declaration.getAttribute("name"));
            }
        }
        assertThat(missing.isEmpty(), is(true));
    }

    static boolean hasDocumentation(Element declaration) {
        NodeList children = declaration.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element
                    && W3C_XML_SCHEMA_NS_URI.equals(child.getNamespaceURI())
                    && "annotation".equals(child.getLocalName())) {
                return hasDocumentationElement((Element) child);
            }
        }
        return false;
    }

    static boolean hasDocumentationElement(Element annotation) {
        NodeList children = annotation.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element documentation
                && W3C_XML_SCHEMA_NS_URI.equals(child.getNamespaceURI())
                && "documentation".equals(child.getLocalName())) {
                return "description".equals(documentation.getAttribute("source"))
                        && !documentation.getTextContent().isBlank();
            }
        }
        return false;
    }
}
