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

import io.helidon.lsp.common.Dependency;
import io.helidon.lsp.server.management.MavenSupport;
import io.helidon.lsp.server.service.config.ConfigurationPropertiesService;
import io.helidon.lsp.server.service.metadata.ConfigMetadata;
import io.helidon.lsp.server.service.metadata.ConfiguredType;
import io.helidon.lsp.server.service.metadata.MetadataProvider;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class YamlTextDocumentHandlerTest {

    private YamlTextDocumentHandler handler = YamlTextDocumentHandler.instance();
    @Mock
    ConfigurationPropertiesService propertiesService;
    @Mock
    MavenSupport mavenSupport;
    @Spy
    MetadataProvider provider = MetadataProvider.instance();

    @BeforeEach
    public void before() throws URISyntaxException, IOException {
        handler.propertiesService(propertiesService);
        Map<String, ConfigMetadata> stringConfigMetadataMap = metadataForFile();
        Mockito.when(propertiesService.metadataForFile(any())).thenReturn(stringConfigMetadataMap);
    }

    @Test
    void testCompletionLabels() throws URISyntaxException, IOException {
        List<CompletionItem> completion = completionItems(new Position(19, 4), "test-config.yaml");
        assertThat(completion.size(), is(6));
        assertThat(completion.stream().anyMatch(item -> item.getLabel().equals("virtual-enforced")), is(true));
        assertThat(completion.stream().anyMatch(item -> item.getLabel().equals("keep-alive-minutes")), is(true));

        completion = completionItems(new Position(19, 2), "test-config.yaml");
        assertThat(completion.size(), is(9));
        assertThat(completion.stream().anyMatch(item -> item.getLabel().equals("environment.executor-service")), is(false));
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(9));

        completion = completionItems(new Position(25, 4), "test-config.yaml");
        assertThat(completion.size(), is(9));//13-4
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(9));

        completion = completionItems(new Position(25, 2), "test-config.yaml");
        assertThat(completion.size(), is(13));//15-2
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(13));

        completion = completionItems(new Position(40, 4), "test-config.yaml");
        assertThat(completion.size(), is(10));//13-3
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(10));
    }

    @Test
    public void testCompletionForAllowedValues() throws URISyntaxException {
        List<CompletionItem> completion = completionItems(new Position(34, 19), "test-config.yaml");
        assertThat(completion.size(), is(3));
        assertThat(completion.stream()
                             .map(CompletionItem::getLabel).
                             collect(Collectors.toSet()),
                hasItems("REQUIRE", "OPTIONAL", "NONE"));
    }

    @Test
    public void testInsertText() throws URISyntaxException {
        List<CompletionItem> completion = completionItems(new Position(25, 4), "test-config.yaml");
        CompletionItem completionItem = completion.stream().filter(item -> item.getLabel().equals("host")).findFirst().orElse(null);
        assertThat(completionItem.getInsertText(), is("host:"));

        completion = completionItems(new Position(44, 6), "test-config.yaml");
        completionItem = completionItemByLabel("private-key", completion);
        assertThat(completionItem.getInsertText(), is("private-key:\n        "));

        completion = completionItems(new Position(50, 4), "test-config.yaml");
        completionItem = completionItemByLabel("services", completion);
        assertThat(completionItem.getInsertText(), is("services:\n    - "));
    }

    @Test
    public void testDefaultValues() throws URISyntaxException {
        List<CompletionItem> completion = completionItems(new Position(19, 2), "test-config.yaml");
        CompletionItem completionItem = completionItemByLabel("default-authorization-provider", completion);
        assertThat(completionItem.getDetail().contains("Default value"), is(false));
        completionItem = completionItemByLabel("provider-policy.class-name", completion);
        assertThat(completionItem.getDetail().contains("Default value"), is(false));
        completionItem = completionItemByLabel("provider-policy.type", completion);
        assertThat(completionItem.getDetail().contains("Default value: FIRST"), is(true));

        completion = completionItems(new Position(47, 2), "test-config.yaml");
        completionItem = completionItemByLabel("max-upgrade-content-length", completion);
        assertThat(completionItem.getDetail().contains("Default value: 65536"), is(true));
    }

    @Test
    public void testAllowedValues() throws URISyntaxException {
        List<CompletionItem> completion = completionItems(new Position(44, 6), "test-config.yaml");
        CompletionItem completionItem = completionItemByLabel("client-auth", completion);
        assertThat(completion.size(), is(6));//7-1
        assertThat(completionItem.getDetail().contains("Allowed values: "), is(true));
        assertThat(completionItem.getDetail().contains("REQUIRE (Authentication is required.)"), is(true));
        assertThat(completionItem.getDetail().contains("OPTIONAL (Authentication is optional.)"), is(true));
        assertThat(completionItem.getDetail().contains("NONE (Authentication is not required.)"), is(true));
    }

    @Test
    public void testCompletionIncorrectConfig() throws URISyntaxException, IOException {
        List<CompletionItem> completion = completionItems(new Position(19, 4), "test-incorrect-config.yaml");
        assertThat(completion.size(), is(0));

        completion = completionItems(new Position(19, 0), "test-incorrect-config.yaml");
        assertThat(completion.size(), is(3));
    }

    private CompletionItem completionItemByLabel(String label, List<CompletionItem> completion) {
        return completion.stream()
                         .filter(item -> item.getLabel().equals(label))
                         .findFirst().orElse(null);
    }

    private List<CompletionItem> completionItems(Position position, String fileName) throws URISyntaxException {
        CompletionParams completionParams = new CompletionParams(
                new TextDocumentIdentifier(getClass().getClassLoader().getResource(fileName).toURI().toString()),
                position
        );
        return handler.completion(completionParams);
    }

    private Map<String, ConfigMetadata> metadataForFile() throws IOException {
        ConfigurationPropertiesService service = ConfigurationPropertiesService.instance();
        service.mavenSupport(mavenSupport);
        service.metadataProvider(provider);
        Set<Dependency> dependencies = getDependencies();
        Mockito.when(mavenSupport.getDependencies(anyString())).thenReturn(dependencies);
        JsonReaderFactory readerFactory = Json.createReaderFactory(Map.of());
        for (Dependency dependency : dependencies) {
            InputStream is = new FileInputStream(dependency.path());
            JsonReader reader = readerFactory.createReader(is, StandardCharsets.UTF_8);
            List<ConfiguredType> configuredTypes = provider.processMetadataJson(reader.readArray());
            Mockito.doReturn(configuredTypes).when(provider).readMetadata(dependency.path());
        }

        return service.metadataForPom("pom.xml");
    }

    private Set<Dependency> getDependencies() {
        File metadataDirectory = new File("src/test/resources/metadata");
        File[] files = metadataDirectory.listFiles();
        Set<Dependency> dependencies = new HashSet<>();

        for (File file : files) {
            if (file.getName().endsWith("json")) {
                Dependency dependency = new Dependency(null, null, null, null, null, file.getPath());
                dependencies.add(dependency);
            }
        }

        return dependencies;
    }
}