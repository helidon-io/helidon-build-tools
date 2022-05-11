/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.service.config.yaml;

import io.helidon.lsp.server.service.config.PropsDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class YamlParserTest {

    @Test
    public void testGetSiblings() throws IOException, URISyntaxException {
        YamlParser yamlParser = new YamlParser();
        PropsDocument yaml = getYaml();
        List<String> fileContent = getFileContent();
        yamlParser.setBinding(yaml, new LinkedList<>(fileContent));
        LinkedHashMap<Map.Entry<String, Object>, PropsDocument.FileBinding> binding = yaml.getBinding();
        for (Map.Entry<Map.Entry<String, Object>, PropsDocument.FileBinding> entry : binding.entrySet()) {
            if (entry.getKey().getKey().equals("field1")) {
                Set<Map.Entry<String, Object>> siblings = yaml.getSiblings(entry.getKey());
                assertEquals(2, siblings.size());
                return;
            }
        }
        fail();
    }

    @Test
    public void testSetBinding() throws IOException, URISyntaxException {
        YamlParser yamlParser = new YamlParser();
        Map<String, Object> map = new LinkedHashMap<>();
        PropsDocument yaml = getYaml();
        List<String> fileContent = getFileContent();
        yamlParser.setBinding(yaml, new LinkedList<>(fileContent));
        LinkedHashMap<Map.Entry<String, Object>, PropsDocument.FileBinding> binding = yaml.getBinding();
        for (Map.Entry<Map.Entry<String, Object>, PropsDocument.FileBinding> entry : binding.entrySet()) {
            if (entry.getKey().getKey().equals("parent")) {
                assertEquals(0, entry.getValue().getLevel());
                assertEquals(15, entry.getValue().getRow());
                assertEquals(0, entry.getValue().getColumn());
            }
            if (entry.getKey().getKey().equals("second")) {
                assertEquals(2, entry.getValue().getLevel());
                assertEquals(18, entry.getValue().getRow());
                assertEquals(4, entry.getValue().getColumn());
            }
            if (entry.getKey().getKey().equals("field1")) {
                assertEquals(3, entry.getValue().getLevel());
                assertEquals(26, entry.getValue().getRow());
                assertEquals(6, entry.getValue().getColumn());
            }
            return;
        }
        fail();
    }

    private PropsDocument getYaml() throws IOException, URISyntaxException {
        YamlParser yamlParser = new YamlParser();
        List<String> strings = getFileContent();
        return yamlParser.parse(strings);
    }

    private List<String> getFileContent() throws IOException, URISyntaxException {
        URL resource = YamlParserTest.class.getResource("/test.yaml");
        Path path = Paths.get(resource.toURI());
        return Files.readAllLines(path);
    }

    @Test
    public void testParseYaml() throws URISyntaxException, IOException {
        YamlParser yamlParser = new YamlParser();
        URL resource = YamlParserTest.class.getResource("/test.yaml");
        Path path = Paths.get(resource.toURI());
        List<String> strings = Files.readAllLines(path);
        int expectedParentNodesCount = 2;

        PropsDocument propsDocument = yamlParser.parse(strings);

        assertEquals(expectedParentNodesCount, propsDocument.keySet().size());
    }
}