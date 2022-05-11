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

package io.helidon.lsp.server.service.config.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.lsp.server.core.LanguageServerContext;
import io.helidon.lsp.server.model.ConfigurationMetadata;
import io.helidon.lsp.server.service.TextDocumentHandler;
import io.helidon.lsp.server.service.config.ConfigurationPropertiesService;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;

/**
 * Proposes completion for the given position in the meta configuration file in Java Properties format.
 */
public class PropertiesTextDocumentHandler implements TextDocumentHandler {

    private static final Logger LOGGER = Logger.getLogger(PropertiesTextDocumentHandler.class.getName());
    private static final String SEPARATOR = "=";
    private final LanguageServerContext languageServerContext;
    private ConfigurationPropertiesService configurationPropertiesService;

    /**
     * Create a new instance.
     *
     * @param languageServerContext languageServerContext.
     */
    public PropertiesTextDocumentHandler(LanguageServerContext languageServerContext) {
        this.languageServerContext = languageServerContext;
        init();
    }

    private void init() {
        configurationPropertiesService = (ConfigurationPropertiesService) languageServerContext
                .getBean(ConfigurationPropertiesService.class);
    }

    @Override
    public List<CompletionItem> completion(CompletionParams position) {
        List<CompletionItem> completionItems = new ArrayList<>();
        try {
            String fileUri = position.getTextDocument().getUri();
            List<ConfigurationMetadata> metadataList =
                    configurationPropertiesService.getConfigMetadataForFile(fileUri);

            Properties existedProperties = loadPropertiesFile(fileUri);
            for (ConfigurationMetadata metadata : metadataList) {
                if (metadata.getProperties() == null) {
                    continue;
                }
                metadata.getProperties().stream()
                        .filter(property -> !existedProperties.containsKey(property.getName()))
                        .forEach(property -> {
                            CompletionItem item = new CompletionItem();
                            item.setKind(CompletionItemKind.Snippet);
                            item.setLabel(property.getName());
                            item.setDetail(property.getType());
                            item.setInsertTextFormat(InsertTextFormat.Snippet);
                            item.setDocumentation(getDocumentation(property));
                            String value = getValue(property, metadata);
                            item.setInsertText(property.getName() + SEPARATOR + value);
                            completionItems.add(item);
                        });
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Exception when trying to get auto-completion data for CompletionParams position " + position,
                    e);
        }
        return completionItems;
    }

    private Properties loadPropertiesFile(String fileUri) throws IOException, URISyntaxException {
        InputStream input = new FileInputStream(new URI(fileUri).getPath());
        Properties properties = new Properties();
        properties.load(input);
        input.close();
        return properties;
    }
}
