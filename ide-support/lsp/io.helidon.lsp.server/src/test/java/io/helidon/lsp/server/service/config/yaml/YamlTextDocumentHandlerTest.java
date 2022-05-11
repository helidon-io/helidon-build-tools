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

import io.helidon.lsp.server.core.LanguageServerContext;
import io.helidon.lsp.server.model.ConfigurationMetadata;
import io.helidon.lsp.server.model.ConfigurationProperty;
import io.helidon.lsp.server.service.config.ConfigurationPropertiesService;
import io.helidon.lsp.server.utils.FileUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class YamlTextDocumentHandlerTest {

    @Mock
    LanguageServerContext languageServerContext;
    @Mock
    ConfigurationPropertiesService propertiesService;
    @Mock
    FileUtils fileUtils;

    @BeforeEach
    public void before() throws URISyntaxException, IOException {
        Mockito.when(languageServerContext.getBean(ConfigurationPropertiesService.class)).thenReturn(propertiesService);
        Mockito.when(languageServerContext.getBean(FileUtils.class)).thenReturn(fileUtils);
        Mockito.when(propertiesService.getConfigMetadataForFile(anyString())).thenReturn(getMetadata());
        Mockito.when(fileUtils.getTextDocContentByURI(anyString())).thenReturn(getFileContent());
        Mockito.when(propertiesService.extractPropertyNames(any())).thenCallRealMethod();
        Mockito.when(propertiesService.keepBottomLevel(any())).thenCallRealMethod();
        Mockito.when(propertiesService.extractPropertyNames(any())).thenCallRealMethod();
    }

    @Test
    public void testCompletionForRoot() {
        Mockito.when(propertiesService.getRootEntries(any())).thenCallRealMethod();
        YamlTextDocumentHandler handler = new YamlTextDocumentHandler(languageServerContext);
        Position position = new Position(1, 0);
        List<String> expected = Arrays.asList("parent5", "parent6");

        List<CompletionItem> completion = handler.completion(getCompletionParams(position));

        assertEquals(2, completion.size());
        assertTrue(expected.containsAll(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet())));
    }

    @Test
    public void testCompletionForFirstLevel() throws URISyntaxException, IOException {
        Mockito.when(propertiesService.getChildProperties(any(), any())).thenCallRealMethod();
        Mockito.when(propertiesService.getTopChildPropertiesPartToPropertyMap(any(), any())).thenCallRealMethod();
        YamlTextDocumentHandler handler = new YamlTextDocumentHandler(languageServerContext);
        Position position = new Position(4, 2);
        List<String> expected = Arrays.asList("child2_3", "child2_4");

        List<CompletionItem> completion = handler.completion(getCompletionParams(position));

        assertEquals(2, completion.size());
        assertTrue(expected.containsAll(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet())));
    }

    @Test
    public void testCompletionForThirdLevel() throws URISyntaxException, IOException {
        Mockito.when(propertiesService.getChildProperties(any(), any())).thenCallRealMethod();
        Mockito.when(propertiesService.getTopChildPropertiesPartToPropertyMap(any(), any())).thenCallRealMethod();
        YamlTextDocumentHandler handler = new YamlTextDocumentHandler(languageServerContext);
        Position position = new Position(8, 6);
        List<String> expected = Arrays.asList("child2_2_1_3", "child2_2_1_4");

        List<CompletionItem> completion = handler.completion(getCompletionParams(position));

        assertEquals(2, completion.size());
        assertTrue(expected.containsAll(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet())));
    }

    private CompletionParams getCompletionParams(Position position) {
        return new CompletionParams(
                new TextDocumentIdentifier("/path/to/file"),
                position
        );
    }

    private List<String> getFileContent() {
        return Arrays.asList(
                "parent1: value1",
                "",
                "parent2:",
                "  child2_1: value2_1",
                "  ",
                "  child2_2:",
                "    child2_2_1:",
                "      child2_2_1_1: value2_2_1_1",
                "       ",
                "      child2_2_1_2: [value1, value2, value3]",
                "parent3:",
                "  child3_1:",
                "    - value1",
                "    - value2",
                "    ",
                "parent4:",
                "  child4_1: value4_1",
                "  child4_2:",
                "    value4_2",
                "  child4_3:",
                "  - value4_3_1",
                "  - value4_3_2",
                "   "
        );
    }

    private List<ConfigurationMetadata> getMetadata() {
        List<ConfigurationMetadata> result = new ArrayList<>();

        ConfigurationMetadata metadata1 = new ConfigurationMetadata();
        List<ConfigurationProperty> properties1 = new ArrayList<>();
        metadata1.setProperties(properties1);
        result.add(metadata1);
        addProperty("parent1", properties1);
        addProperty("parent2.child2_1", properties1);
        addProperty("parent2.child2_2.child2_2_1.child2_2_1_1", properties1);
        addProperty("parent2.child2_2.child2_2_1.child2_2_1_2", properties1);
        addProperty("parent2.child2_2.child2_2_1.child2_2_1_3", properties1);//new
        addProperty("parent2.child2_2.child2_2_1.child2_2_1_4", properties1);//new
        addProperty("parent3.child3_1", properties1);
        addProperty("parent3.child3_2", properties1);//new
        addProperty("parent2.child2_3.child3_1_1", properties1);//new
        addProperty("parent2.child2_4.child3_1_2", properties1);//new

        ConfigurationMetadata metadata2 = new ConfigurationMetadata();
        List<ConfigurationProperty> properties2 = new ArrayList<>();
        metadata2.setProperties(properties2);
        result.add(metadata2);
        addProperty("parent4.child4_1", properties2);
        addProperty("parent4.child4_2", properties2);
        addProperty("parent4.child4_3", properties2);
        addProperty("parent5", properties2);//new
        addProperty("parent6.child6_1", properties2);//new

        return result;
    }

    private void addProperty(String propName, List<ConfigurationProperty> properties) {
        ConfigurationProperty property = new ConfigurationProperty();
        property.setName(propName);
        properties.add(property);
    }
}