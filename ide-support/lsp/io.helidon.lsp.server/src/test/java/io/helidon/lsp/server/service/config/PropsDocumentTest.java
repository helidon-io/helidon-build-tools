/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PropsDocumentTest {

    @Test
    public void testGetLeavesInSimplePropsDoc() {
        PropsDocument simplePropsDocument = getSimplePropsDocument();
        Set<String> expectedValue = Set.of(
                "child1.child1_1",
                "child1.child1_2.child1_2_1",
                "child1.child1_2.child1_2_2",
                "child2.child2_1",
                "child2.child2_2"
        );

        Set<String> leaves = simplePropsDocument.getLeaves();

        assertEquals(expectedValue, leaves);
    }

    @Test
    public void testToStringInSimplePropsDoc() {
        PropsDocument simplePropsDocument = getSimplePropsDocument();
        Map.Entry<?, ?> rootParent = (Map.Entry<?, ?>) simplePropsDocument.entrySet().toArray()[0];
        Map.Entry<?, ?> expectedParent = (Map.Entry<?, ?>) ((Map<?, ?>) rootParent.getValue())
                .entrySet().toArray()[1];
        Object child = ((Map<?, ?>) expectedParent.getValue()).entrySet().toArray()[0];
        String expectedResult = "child1.child1_2.child1_2_1";

        String result = simplePropsDocument.toString(child);

        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetParentNodeInSimplePropsDoc_otherChildObject() {
        PropsDocument simplePropsDocument = getSimplePropsDocument();
        Object child = new Object();

        Map.Entry<String, Object> parent = simplePropsDocument.getParentNode(child);

        assertNull(parent);
    }

    @Test
    public void testGetParentNodeInSimplePropsDoc_otherChildEntry() {
        PropsDocument simplePropsDocument = getSimplePropsDocument();
        Object child = Map.entry("key", "value");

        Map.Entry<String, Object> parent = simplePropsDocument.getParentNode(child);

        assertNull(parent);
    }

    @Test
    public void testGetParentNodeInSimplePropsDoc() {
        PropsDocument simplePropsDocument = getSimplePropsDocument();
        Map.Entry<?, ?> rootParent = (Map.Entry<?, ?>) simplePropsDocument.entrySet().toArray()[0];
        Map.Entry<?, ?> expectedParent = (Map.Entry<?, ?>) ((Map<?, ?>) rootParent.getValue())
                .entrySet().toArray()[1];
        Object child = ((Map<?, ?>) expectedParent.getValue()).entrySet().toArray()[0];

        Map.Entry<String, Object> parent = simplePropsDocument.getParentNode(child);

        assertEquals(expectedParent, parent);
    }

    private PropsDocument getSimplePropsDocument() {
        PropsDocument propsDocument = new PropsDocument();
        LinkedHashMap<String, Object> firstChild = new LinkedHashMap<>();
        firstChild.put("child1_1", "child1_1-value");
        LinkedHashMap<String, String> firstChild1_2 = new LinkedHashMap<>();
        firstChild1_2.put("child1_2_1", "child1_2_1-value");
        firstChild1_2.put("child1_2_2", "child1_2_2-value");
        firstChild.put("child1_2", firstChild1_2);
        propsDocument.put("child1", firstChild);
        LinkedHashMap<String, String> secondChild = new LinkedHashMap<>();
        secondChild.put("child2_1", "child1_1-value");
        secondChild.put("child2_2", "child1_2-value");
        propsDocument.put("child2", secondChild);
        return propsDocument;
    }

    @Test
    public void testPropsToMap() throws JsonProcessingException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        String value = "title=Home Page\n" +
                "site.host=localhost\n" +
                "site.port=8080";
        PropsDocument propsDocument = mapper.readValue(value, PropsDocument.class);
        assertTrue(propsDocument.size() > 1);
    }

    @Test
    public void testMapToProps() throws IOException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        PropsDocument simplePropsDocument = getSimplePropsDocument();
        Properties properties = mapper.writeValueAsProperties(simplePropsDocument);
        String expectedKey1 = "child1.child1_2.child1_2_2";
        String expectedValue1 = "child1_2_2-value";
        assertNotNull(properties);
        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            if (entry.getKey().toString().equals(expectedKey1)) {
                assertEquals(expectedValue1, entry.getValue().toString());
                return;
            }
        }
        fail();
    }
}