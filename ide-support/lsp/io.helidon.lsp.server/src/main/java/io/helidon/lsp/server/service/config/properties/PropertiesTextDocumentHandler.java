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
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.lsp.server.service.ContentManager;
import io.helidon.lsp.server.service.TextDocumentHandler;
import io.helidon.lsp.server.service.config.ConfigurationPropertiesService;
import io.helidon.lsp.server.service.metadata.ConfigMetadata;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;

/**
 * Proposes completion for the given position in the meta configuration file in Java Properties format.
 */
public class PropertiesTextDocumentHandler implements TextDocumentHandler {

    private static final PropertiesTextDocumentHandler INSTANCE = new PropertiesTextDocumentHandler();
    private static final Logger LOGGER = Logger.getLogger(PropertiesTextDocumentHandler.class.getName());
    private static final Pattern KEY_PATTERN = Pattern.compile("\\s*(?<key>[\\w\\-.]+)[:=]\\s*");
    private static final String SEPARATOR = "=";

    private ConfigurationPropertiesService propertiesService;
    private ContentManager contentManager;

    private PropertiesTextDocumentHandler() {
        init();
    }

    /**
     * Get the instance of the class.
     *
     * @return instance of the class.
     */
    public static PropertiesTextDocumentHandler instance() {
        return INSTANCE;
    }

    private void init() {
        contentManager = ContentManager.instance();
        propertiesService = ConfigurationPropertiesService.instance();
    }

    /**
     * Set propertiesService.
     *
     * @param propertiesService propertiesService.
     */
    public void propertiesService(ConfigurationPropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    @Override
    public List<CompletionItem> completion(CompletionParams completionParams) {
        List<CompletionItem> completionItems = new ArrayList<>();
        String fileUri = completionParams.getTextDocument().getUri();
        try {
            Map<String, ConfigMetadata> configMetadata = propertiesService.metadataForFile(fileUri);
            List<String> fileContent = contentManager.read(fileUri);
            Position position = completionParams.getPosition();
            String currentLine = fileContent.get(position.getLine());
            String baseForCompletion = currentLine.substring(0, position.getCharacter());
            String currentKey = currentKey(baseForCompletion);
            String filter = currentKey != null ? currentKey : baseForCompletion;

            Map<String, ConfigMetadata> proposedMetadata =
                    configMetadata.entrySet().stream()
                                  .filter(entry -> entry.getKey().startsWith(filter)
                                          && !entry.getKey().equals(filter)
                                          && entry.getValue().content() == null
                                  )
                                  .collect(Collectors.toMap(
                                          Map.Entry::getKey,
                                          Map.Entry::getValue
                                  ));

            if (currentKey == null) {
                return completionItemsForKey(proposedMetadata, baseForCompletion);
            } else {
                return completionItemsForValue(configMetadata.get(currentKey));
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Exception when trying to get auto-completion data for CompletionParams position in " + fileUri,
                    e);
        }
        return completionItems;
    }

    private List<CompletionItem> completionItemsForValue(ConfigMetadata proposedMetadata) {
        return prepareCompletionForAllowedValues(proposedMetadata);
    }

    private List<CompletionItem> completionItemsForKey(Map<String, ConfigMetadata> proposedMetadata,
                                                       String baseForCompletion) {
        if (proposedMetadata == null) {
            return List.of();
        }
        List<CompletionItem> result = new ArrayList<>();
        proposedMetadata.forEach((key, value) -> {
                    CompletionItem item = new CompletionItem();
                    item.setKind(CompletionItemKind.Snippet);
                    item.setLabel(key);
                    item.setInsertText(StringUtils.difference(baseForCompletion, key) + SEPARATOR);
                    item.setDocumentation(prepareInfoForKey(value));
                    item.setDetail(value.description());
                    item.setInsertTextFormat(InsertTextFormat.Snippet);
                    result.add(item);
                }
        );
        return result;
    }

    private String currentKey(String input) {
        Matcher matcher = KEY_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group("key");
        }
        return null;
    }

    private Properties loadPropertiesFile(String fileUri) throws IOException, URISyntaxException {
        InputStream input = new FileInputStream(new URI(fileUri).getPath());
        Properties properties = new Properties();
        properties.load(input);
        input.close();
        return properties;
    }

}
