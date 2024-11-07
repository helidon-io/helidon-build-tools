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

package io.helidon.lsp.server.service.config.properties;

import java.util.List;
import java.util.stream.Collectors;

import io.helidon.lsp.server.service.config.CompletionTestBase;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

class PropertiesTextDocumentHandlerTest extends CompletionTestBase {

    private final PropertiesTextDocumentHandler handler = PropertiesTextDocumentHandler.instance();

    @BeforeEach
    public void before() {
        super.before();
        handler.propertiesService(propertiesService);
    }

    @Test
    void testCompletionLabels() {
        List<CompletionItem> completion = completionItems(new Position(17, 3), "test-config.properties");
        int expectedAmount = 121;
        assertThat(completion.size(), is(expectedAmount));
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(expectedAmount));

        completion = completionItems(new Position(18, 8), "test-config.properties");
        assertThat(completion.size(), is(expectedAmount));
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(expectedAmount));

        completion = completionItems(new Position(20, 21), "test-config.properties");
        expectedAmount = 18;
        assertThat(completion.size(), is(expectedAmount));
        assertThat(matchLabel("security.environment.executor-service.core-pool-size", completion), is(true));
        assertThat(matchLabel("security.environment.server-time.year", completion), is(true));
        assertThat(completion.stream().map(CompletionItem::getLabel).collect(Collectors.toSet()).size(), is(expectedAmount));

        completion = completionItems(new Position(21, 37), "test-config.properties");
        assertThat(completion.size(), is(9));
        assertThat(matchLabel("security.environment.executor-service.core-pool-size", completion), is(true));
        assertThat(matchLabel("security.environment.executor-service.is-daemon", completion), is(true));

        completion = completionItems(new Position(22, 58), "test-config.properties");
        assertThat(completion.size(), is(0));

        completion = completionItems(new Position(22, 56), "test-config.properties");
        assertThat(completion.size(), is(0));
    }

    @Test
    void testCompletionForAllowedValues() {
        List<CompletionItem> completion = completionItems(new Position(25, 31), "test-config.properties");
        assertThat(completion.size(), is(3));
        assertThat(completion.stream()
                             .map(CompletionItem::getLabel).
                             collect(Collectors.toSet()),
                hasItems("REQUIRE", "OPTIONAL", "NONE"));

        completion = completionItems(new Position(26, 25), "test-config.properties");
        assertThat(completion.size(), is(2));
        assertThat(completion.stream()
                             .map(CompletionItem::getLabel).
                             collect(Collectors.toSet()),
                hasItems("true", "false"));
    }

    @Test
    void testInsertText() {
        List<CompletionItem> completion = completionItems(new Position(21, 37), "test-config.properties");
        CompletionItem completionItem =
                completion.stream()
                          .filter(item -> item.getLabel().equals("security.environment.executor-service.core-pool-size"))
                          .findFirst().orElse(null);
        assertThat(completionItem.getInsertText(), is(".core-pool-size="));

        completion = completionItems(new Position(20, 21), "test-config.properties");
        completionItem =
                completion.stream()
                          .filter(item -> item.getLabel().equals("security.environment.server-time.year"))
                          .findFirst().orElse(null);
        assertThat(completionItem.getInsertText(), is("server-time.year="));
    }

    @Test
    void testDefaultValues() {
        List<CompletionItem> completion = completionItems(new Position(24, 14), "test-config.properties");
        CompletionItem completionItem = completionItemByLabel("server.sockets.tls.private-key.pem.key.resource.content-plain",
                completion);
        assertThat(completionItem.getDocumentation().getLeft().contains("Default value"), is(false));

        completion = completionItems(new Position(21, 37), "test-config.properties");
        completionItem = completionItemByLabel("security.environment.executor-service.keep-alive-minutes", completion);
        assertThat(completionItem.getDocumentation().getLeft().contains("Default value: 3"), is(true));
    }

    @Test
    void testAllowedValues() {
        List<CompletionItem> completion = completionItems(new Position(25, 26), "test-config.properties");
        CompletionItem completionItem = completionItemByLabel("server.sockets.tls.client-auth", completion);
        assertThat(completion.size(), is(1));
        assertThat(completionItem.getDocumentation().getLeft().contains("Allowed values: "), is(true));
        assertThat(completionItem.getDocumentation().getLeft().contains("REQUIRE (Authentication is required.)"), is(true));
        assertThat(completionItem.getDocumentation().getLeft().contains("OPTIONAL (Authentication is optional.)"), is(true));
        assertThat(completionItem.getDocumentation().getLeft().contains("NONE (Authentication is not required.)"), is(true));
    }

    private List<CompletionItem> completionItems(Position position, String fileName) {
        return completionItems(position, fileName, handler);
    }

    private boolean matchLabel(String label, List<CompletionItem> completion) {
        return completion.stream().anyMatch(item -> item.getLabel().equals(label));
    }
}
