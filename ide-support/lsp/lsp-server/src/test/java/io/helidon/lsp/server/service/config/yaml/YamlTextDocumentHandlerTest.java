/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
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

import io.helidon.lsp.server.service.config.CompletionTestBase;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

class YamlTextDocumentHandlerTest extends CompletionTestBase {

    private final YamlTextDocumentHandler handler = YamlTextDocumentHandler.instance();

    @BeforeEach
    public void before() {
        super.before();
        handler.propertiesService(propertiesService);
    }

    @Test
    void testCompletionLabels() {
        List<CompletionItem> completion = completionItems(new Position(19, 4), "test-config.yaml");
        assertThat(completion.size(), is(6));
        assertThat(completion.stream().anyMatch(item -> item.getLabel().equals("virtual-enforced")), is(true));
        assertThat(completion.stream().anyMatch(item -> item.getLabel().equals("keep-alive-minutes")), is(true));

        completion = completionItems(new Position(50, 0), "test-config.yaml");
        assertThat(completion.size(), is(0));

        completion = completionItems(new Position(17, 6), "test-config-list.yaml");
        assertThat(completion.size(), is(15));
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(15));
        assertThat(completion.stream().anyMatch(item -> item.getLabel().equals("name")), is(true));

        completion = completionItems(new Position(19, 2), "test-config.yaml");
        assertThat(completion.size(), is(9));
        assertThat(completion.stream().anyMatch(item -> item.getLabel().equals("environment.executor-service")), is(false));
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(9));

        completion = completionItems(new Position(25, 4), "test-config.yaml");
        assertThat(completion.size(), is(11));//15-4
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(11));

        completion = completionItems(new Position(25, 2), "test-config.yaml");
        assertThat(completion.size(), is(15));//17-2
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(15));

        completion = completionItems(new Position(40, 4), "test-config.yaml");
        assertThat(completion.size(), is(12));//15-3
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(12));
    }

    @Test
    void testCompletionForAllowedValues() {
        List<CompletionItem> completion = completionItems(new Position(34, 19), "test-config.yaml");
        assertThat(completion.size(), is(3));
        assertThat(completion.stream()
                             .map(CompletionItem::getLabel).
                             collect(Collectors.toSet()),
                hasItems("REQUIRE", "OPTIONAL", "NONE"));
    }

    @Test
    void testInsertText() {
        List<CompletionItem> completion = completionItems(new Position(25, 4), "test-config.yaml");
        CompletionItem completionItem = completion.stream().filter(item -> item.getLabel().equals("host")).findFirst()
                                                  .orElse(null);
        assertThat(completionItem.getInsertText(), is("host: "));

        completion = completionItems(new Position(44, 6), "test-config.yaml");
        completionItem = completionItemByLabel("private-key", completion);
        assertThat(completionItem.getInsertText(), is("private-key:\n        "));

        completion = completionItems(new Position(50, 4), "test-config.yaml");
        completionItem = completionItemByLabel("services", completion);
        assertThat(completionItem.getInsertText(), is("services:\n  - "));
    }

    @Test
    void testDefaultValues() {
        List<CompletionItem> completion = completionItems(new Position(19, 2), "test-config.yaml");
        CompletionItem completionItem = completionItemByLabel("default-authorization-provider", completion);
        assertThat(completionItem.getDocumentation().getLeft().contains("Default value"), is(false));
        completionItem = completionItemByLabel("provider-policy.class-name", completion);
        assertThat(completionItem.getDocumentation().getLeft().contains("Default value"), is(false));
        completionItem = completionItemByLabel("provider-policy.type", completion);
        assertThat(completionItem.getDocumentation().getLeft().contains("Default value: FIRST"), is(true));

        completion = completionItems(new Position(47, 2), "test-config.yaml");
        completionItem = completionItemByLabel("max-upgrade-content-length", completion);
        assertThat(completionItem.getDocumentation().getLeft().contains("Default value: 65536"), is(true));
    }

    @Test
    void testAllowedValues() {
        List<CompletionItem> completion = completionItems(new Position(44, 6), "test-config.yaml");
        CompletionItem completionItem = completionItemByLabel("client-auth", completion);
        assertThat(completion.size(), is(6));//7-1
        assertThat(completionItem.getDocumentation().getLeft().contains("Allowed values: "), is(true));
        assertThat(completionItem.getDocumentation().getLeft().contains("REQUIRE (Authentication is required.)"), is(true));
        assertThat(completionItem.getDocumentation().getLeft().contains("OPTIONAL (Authentication is optional.)"), is(true));
        assertThat(completionItem.getDocumentation().getLeft().contains("NONE (Authentication is not required.)"), is(true));
    }

    @Test
    void testCompletionIncorrectConfig() {
        List<CompletionItem> completion = completionItems(new Position(19, 0), "test-incorrect-config.yaml");
        assertThat(completion.size(), is(2));

        completion = completionItems(new Position(19, 4), "test-incorrect-config.yaml");
        assertThat(completion.size(), is(0));
    }

    private List<CompletionItem> completionItems(Position position, String fileName) {
        return completionItems(position, fileName, handler);
    }
}