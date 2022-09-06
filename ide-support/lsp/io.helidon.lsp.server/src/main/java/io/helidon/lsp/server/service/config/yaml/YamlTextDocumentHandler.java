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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.lsp.server.core.LanguageServerContext;
import io.helidon.lsp.server.model.ConfigurationMetadata;
import io.helidon.lsp.server.model.ConfigurationProperty;
import io.helidon.lsp.server.service.TextDocumentHandler;
import io.helidon.lsp.server.service.config.ConfigurationPropertiesService;
import io.helidon.lsp.server.service.config.PropsDocument;
import io.helidon.lsp.server.service.metadata.ConfigMetadata;
import io.helidon.lsp.server.utils.FileUtils;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;

/**
 * Proposes completion for the given position in the meta configuration file in Java Yaml format.
 */
public class YamlTextDocumentHandler implements TextDocumentHandler {

    private static final Logger LOGGER = Logger.getLogger(YamlTextDocumentHandler.class.getName());
    private static final String SEPARATOR = ":";
    private final LanguageServerContext languageServerContext;
    private final YamlParser yamlParser = new YamlParser();
    private ConfigurationPropertiesService propertiesService;
    private FileUtils fileUtils;

    /**
     * Create a new instance.
     *
     * @param languageServerContext languageServerContext
     */
    public YamlTextDocumentHandler(LanguageServerContext languageServerContext) {
        this.languageServerContext = languageServerContext;
        init();
    }

    private void init() {
        propertiesService =
                (ConfigurationPropertiesService) languageServerContext.getBean(ConfigurationPropertiesService.class);
        fileUtils = (FileUtils) languageServerContext.getBean(FileUtils.class);
    }

    @Override
    public List<CompletionItem> completion(CompletionParams position) {
        List<CompletionItem> completionItems = new ArrayList<>();
        try {
            //get config data
            String fileUri = position.getTextDocument().getUri();
            Map<String, ConfigMetadata> configMetadataForFile = propertiesService.metadataForFile(fileUri);

            Map<String, String> proposeComletion = proposeCompletion(position, configMetadataForFile);

            proposeComletion.forEach((key, value) -> {
                        CompletionItem item = new CompletionItem();
                        item.setKind(CompletionItemKind.Snippet);
                        item.setLabel(key);
                        item.setInsertTextFormat(InsertTextFormat.Snippet);
                        AtomicReference<String> proposedValue = new AtomicReference<>();
                        //TODO add calculating for proposedValue
//                        if (propertiesService.keepBottomLevel(value).equals(key)) {
//                            Optional<ConfigurationProperty> configurationProperty = configMetadataForFile
//                                    .stream()
//                                    .map(ConfigurationMetadata::getProperties)
//                                    .flatMap(Collection::stream)
//                                    .filter(property -> property.getName().equals(value))
//                                    .findFirst();
//                            configurationProperty.ifPresent(property -> {
//                                proposedValue.set("");
//                                item.setDocumentation(getDocumentation(property));
//                                String rawValue = getValue(
//                                        property,
//                                        configMetadataForFile
//                                                .stream()
//                                                .filter(metadata -> metadata.getProperties().contains(property))
//                                                .findFirst().orElse(new ConfigurationMetadata())
//                                );
//                                proposedValue.set(processRawValue(rawValue));
//                            });
//                        } else {
//                            proposedValue.set("");
//                        }
                        item.setInsertText(key + SEPARATOR + " " + proposedValue.get());
                        completionItems.add(item);
                    }
            );
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return completionItems;
    }

    //todo ??? OLD / I am not sure that is still valid ??? remove after change defaultValue type from String to Object in ConfigurationProperty
    private String processRawValue(String rawValue) {
        if (rawValue.contains("\",\"")) {
            return "[" + rawValue + "]";
        }
        return rawValue;
    }

    /**
     * Propose completion for the current position in the file in form of Map
     * that binds proposed completion and string representation for the corresponding yaml node
     * (<child, root.parent.child>).
     *
     * @param position              Current cursor position in file.
     * @param configMetadataForFile Configuration Metadata for current project.
     * @return Map that binds proposed completion and string representation for the corresponding yaml node
     * @throws IOException        IOException
     * @throws URISyntaxException URISyntaxException
     */
    private Map<String, String> proposeCompletion(
            CompletionParams position,
            Map<String, ConfigMetadata> configMetadataForFile
    ) throws IOException, URISyntaxException {
        //TODO implement
        String fileUri = position.getTextDocument().getUri();
        List<String> propsFileContent = fileUtils.getTextDocContentByURI(fileUri);
        return Map.of();
    }
}
