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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.lsp.server.service.TextDocumentHandler;
import io.helidon.lsp.server.service.config.ConfigurationPropertiesService;
import io.helidon.lsp.server.service.metadata.ConfigMetadata;
import io.helidon.lsp.server.utils.FileUtils;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;

/**
 * Proposes completion for the given position in the meta configuration file in Java Yaml format.
 */
public class YamlTextDocumentHandler implements TextDocumentHandler {

    private static final YamlTextDocumentHandler INSTANCE = new YamlTextDocumentHandler();
    private static final Logger LOGGER = Logger.getLogger(YamlTextDocumentHandler.class.getName());
    private static final Pattern KEY_PATTERN = Pattern.compile("\\s*[-\\[]*\\s*(?<key>\\w+)\\s*:.*");
    private static final String SEPARATOR = ":";

    private final YamlParser yamlParser = new YamlParser();
    private ConfigurationPropertiesService propertiesService;
    private FileUtils fileUtils;

    /**
     * Create a new instance.
     */
    private YamlTextDocumentHandler() {
        init();
    }

    public static YamlTextDocumentHandler instance() {
        return INSTANCE;
    }

    private void init() {
        propertiesService = ConfigurationPropertiesService.instance();
        fileUtils = FileUtils.instance();
    }

    public void propertiesService(ConfigurationPropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    @Override
    public List<CompletionItem> completion(CompletionParams completionParams) {
        try {
            //get config data
            String fileUri = completionParams.getTextDocument().getUri();
            Map<String, ConfigMetadata> configMetadataForFile = propertiesService.metadataForFile(fileUri);

            CompletionDetails completionDetails = proposeCompletionDetails(completionParams, configMetadataForFile);
            String metadataKind = Optional.ofNullable(completionDetails.parentConfig)
                                          .map(ConfigMetadata::kind)
                                          .map(Enum::toString)
                                          .orElse("default");
            if (metadataKind.equals("LIST")) {
                processListMetadata(completionDetails);
            } else {
                if (currentKey(completionDetails) == null) {
                    processMetadataByDefault(completionDetails);
                }
            }
            return prepareCompletionItems(completionDetails);
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return List.of();
    }

    private List<CompletionItem> prepareCompletionItems(CompletionDetails completionDetails) {
        String currentKey = currentKey(completionDetails);
        if (currentKey != null) {
            ConfigMetadata configMetadata = completionDetails.proposedMetadata.values().stream()
                                                                              .findFirst()
                                                                              .orElse(null);
            return prepareCompletionItemsForValue(configMetadata);
        } else {
            return prepareCompletionItemsForKey(completionDetails.proposedMetadata);
        }
    }

    private List<CompletionItem> prepareCompletionItemsForKey(Map<String, ConfigMetadata> proposedMetadata) {
        if (proposedMetadata == null) {
            return List.of();
        }
        List<CompletionItem> result = new ArrayList<>();
        proposedMetadata.forEach((key, value) -> {
                    CompletionItem item = new CompletionItem();
                    item.setKind(CompletionItemKind.Snippet);
                    item.setLabel(value.key());
                    item.setInsertText(value.key() + SEPARATOR + suffixForInsertText(value));
                    item.setDocumentation(value.description());
                    item.setDetail(prepareDetailsForKey(value));
                    item.setInsertTextFormat(InsertTextFormat.Snippet);
                    result.add(item);
                }
        );
        return result;
    }

    private String suffixForInsertText(ConfigMetadata value) {
        if (value.kind() != null) {
            if (value.kind() == ConfiguredOption.Kind.LIST) {
                return "\n" + String.format("%-" + value.level() * 2 + "s", "") + "- ";
            }
            if (value.kind() == ConfiguredOption.Kind.VALUE) {
                if (value.content() == null || value.content().size() == 0) {
                    return "";
                }
            }
        }
        return "\n" + String.format("%-" + (value.level() + 1) * 2 + "s", "");
    }

    private List<CompletionItem> prepareCompletionItemsForValue(ConfigMetadata configMetadata) {
        return prepareCompletionForAllowedValues(configMetadata);
    }

    private String currentKey(CompletionDetails completionDetails) {
        String currentLine = completionDetails.fileContent.get(completionDetails.position.getLine());
        Matcher matcher = KEY_PATTERN.matcher(currentLine.substring(0, completionDetails.position.getCharacter()));
        if (matcher.find()) {
            return matcher.group("key");
        }
        return null;
    }

    private void processMetadataByDefault(CompletionDetails details) {
        Collection<String> children = childNodes(details.parentLineResultEntry, details.yamlFileResult).values();
        details.proposedMetadata = details.proposedMetadata
                .entrySet()
                .stream()
                .filter((entry) -> !children.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void processListMetadata(CompletionDetails completionDetails) {
        int startListElementLine = startListElementLine(completionDetails);
        int endListElementLine = endListElementLine(completionDetails);
        if (startListElementLine == -1 || endListElementLine == 0) {
            completionDetails.proposedMetadata = new LinkedHashMap<>();
        }
        Set<String> yamlResultsToExclusion = completionDetails.yamlFileResult
                .entrySet().stream()
                .filter(entry -> entry.getKey().line() >= startListElementLine && entry.getKey().line() < endListElementLine)
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        completionDetails.proposedMetadata =
                completionDetails.proposedMetadata.entrySet()
                                                  .stream()
                                                  .filter((entry) -> !yamlResultsToExclusion.contains(entry.getKey()))
                                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                          Map.Entry::getValue));
    }

    private int startListElementLine(CompletionDetails completionDetails) {
        int parentLine =
                completionDetails.parentLineResultEntry == null ? -1 : completionDetails.parentLineResultEntry.getKey().line();
        int currentLine = completionDetails.position.getLine();
        int startListElementLine = -1;
        String pattern = String.format("^\\s{%s,}-.*", completionDetails.parentLineResultEntry.getKey().indent());
        for (int i = currentLine; i > parentLine; i--) {
            if (completionDetails.fileContent.get(i).matches(pattern)) {
                startListElementLine = i;
                break;
            }
        }
        return startListElementLine;
    }

    private int endListElementLine(CompletionDetails completionDetails) {
        int currentLine = completionDetails.position.getLine();
        String pattern = String.format("^\\s{%s,}-.*", completionDetails.parentLineResultEntry.getKey().indent());
        LineResult nextElement = null;
        if (completionDetails.parentLineResultEntry != null) {
            Iterator<LineResult> keyIterator = completionDetails.yamlFileResult.keySet().iterator();
            while (keyIterator.hasNext()) {
                LineResult current = keyIterator.next();
                if (current.equals(completionDetails.parentLineResultEntry.getKey())) {
                    while (keyIterator.hasNext()) {
                        current = keyIterator.next();
                        if (current.indent() <= completionDetails.parentLineResultEntry.getKey().indent()) {
                            nextElement = current;
                            break;
                        }
                    }
                }
            }
        }
        int endListElementLine = 0;
        for (int i = currentLine + 1; i < completionDetails.fileContent.size(); i++) {
            if (completionDetails.fileContent.get(i).matches(pattern)) {
                endListElementLine = i;
                break;
            }
        }
        if (nextElement != null) {
            if (nextElement.line() < endListElementLine || endListElementLine == 0) {
                return nextElement.line();
            }
            ;
        }
        return endListElementLine;
    }

    /**
     * Propose completion for the current position in the file.
     *
     * @param completionParams      Current cursor position information in the file.
     * @param configMetadataForFile Configuration Metadata for current project.
     * @return Set with configuration metadata that is proposal for the current position.
     * @throws IOException        IOException
     * @throws URISyntaxException URISyntaxException
     */
    private CompletionDetails proposeCompletionDetails(
            CompletionParams completionParams,
            Map<String, ConfigMetadata> configMetadataForFile
    ) throws IOException, URISyntaxException {
        CompletionDetails result = new CompletionDetails();
        String fileUri = completionParams.getTextDocument().getUri();
        Position position = completionParams.getPosition();
        result.position = position;
        result.fileContent = fileUtils.getTextDocContentByURI(fileUri);
        LinkedHashMap<LineResult, String> yamlFileResult = yamlParser.parse(result.fileContent);
        result.yamlFileResult = yamlFileResult;

        if (position.getCharacter() == 0 || position.getLine() == 0) {
            result.proposedMetadata = configMetadataByLevel(0, configMetadataForFile);
            return result;
        }

        Map.Entry<LineResult, String> parentLineResultEntry = parentNode(result);
        if (parentLineResultEntry == null) {
            result.proposedMetadata = configMetadataByLevel(0, configMetadataForFile);
            return result;
        }
        result.parentLineResultEntry = parentLineResultEntry;
        result.parentConfig = configMetadataForFile.get(parentLineResultEntry.getValue());
        if (currentKey(result) != null) {
            String path = yamlFileResult.entrySet().stream()
                                        .filter(entry -> entry.getKey().line() == position.getLine())
                                        .map(Map.Entry::getValue)
                                        .findFirst()
                                        .orElse(null);
            if (path != null) {
                result.proposedMetadata = configMetadataForFile
                        .entrySet()
                        .stream()
                        .filter((entry) -> entry.getKey().equals(path))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        } else {
            result.proposedMetadata = configMetadataForFile
                    .entrySet()
                    .stream()
                    .filter((entry) -> entry.getKey().equals(parentLineResultEntry.getValue() + "." + entry.getValue().key()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return result;
    }

    private Map<String, ConfigMetadata> configMetadataByLevel(int level, Map<String, ConfigMetadata> configMetadata) {
        return configMetadata.entrySet().stream()
                             .filter(value -> value.getValue().level() == level)
                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<LineResult, String> parentNode(CompletionDetails details) {
        Map.Entry<LineResult, String> parent = null;
        int currentLineResultIndent =
                details.yamlFileResult.entrySet().stream()
                                      .filter(entry -> entry.getKey().line() == details.position.getLine())
                                      .findFirst()
                                      .map(entry -> entry.getKey().indent())
                                      .orElse(details.position.getCharacter());
        int currentLineIndent = currentKey(details) != null ? currentLineResultIndent : details.position.getCharacter();
        for (Map.Entry<LineResult, String> entry : details.yamlFileResult.entrySet()) {
            LineResult value = entry.getKey();
            if (value.line() >= details.position.getLine()) {
                return parent;
            }
            if (currentLineIndent > value.indent() && parent == null) {
                parent = entry;
                continue;
            }
            if (parent != null && value.indent() < currentLineIndent) {
                parent = entry;
            }
        }
        return parent;
    }

    private LinkedHashMap<LineResult, String> childNodes(
            Map.Entry<LineResult, String> parent, Map<LineResult, String> yamlFileResult
    ) {
        LinkedHashMap<LineResult, String> result = new LinkedHashMap<>();
        if (parent == null) {
            return result;
        }
        int childIntent = 0;
        boolean started = false;
        for (Map.Entry<LineResult, String> entry : yamlFileResult.entrySet()) {
            if (entry.getKey().equals(parent.getKey())) {
                started = true;
                continue;
            }
            if (!started) {
                continue;
            }
            if (entry.getKey().indent() <= parent.getKey().indent()) {
                return result;
            }
            if (childIntent == 0) {
                childIntent = entry.getKey().indent();
            } else {
                if (childIntent < entry.getKey().indent()) {
                    continue;
                }
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static class CompletionDetails {

        Map<String, ConfigMetadata> proposedMetadata;
        Position position;
        List<String> fileContent;
        LinkedHashMap<LineResult, String> yamlFileResult;
        Map.Entry<LineResult, String> parentLineResultEntry;
        ConfigMetadata parentConfig;
    }
}
