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

package io.helidon.lsp.server.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.helidon.lsp.server.core.LanguageServerContext;
import io.helidon.lsp.server.service.config.properties.PropertiesTextDocumentHandler;
import io.helidon.lsp.server.service.config.yaml.YamlTextDocumentHandler;

/**
 * Factory for the TextDocumentHandler implementations.
 */
public class TextDocumentHandlerFactory {

    private static final Map<String, TextDocumentHandler> FILE_EXTENSION_TO_HANDLER_MAP = new HashMap<>();
    private static final TextDocumentHandler EMPTY_TEXT_DOCUMENT_HANDLER = new EmptyTextDocumentHandler();

    private TextDocumentHandlerFactory() {
    }

    /**
     * Get TextDocumentHandler instance for the file.
     *
     * @param fileName              File name.
     * @param languageServerContext languageServerContext.
     * @return TextDocumentHandler instance for the file.
     */
    public static TextDocumentHandler getByFileExtension(String fileName, LanguageServerContext languageServerContext) {
        final String fileExtension = getFileExtension(fileName);
        if (fileExtension.equalsIgnoreCase("properties")) {
            if (!FILE_EXTENSION_TO_HANDLER_MAP.containsKey("properties")) {
                FILE_EXTENSION_TO_HANDLER_MAP.put("properties", new PropertiesTextDocumentHandler(languageServerContext));
            }
        }
        if (fileExtension.equalsIgnoreCase("yaml") || fileExtension.equalsIgnoreCase("yml")) {
            if (!FILE_EXTENSION_TO_HANDLER_MAP.containsKey("yaml")) {
                FILE_EXTENSION_TO_HANDLER_MAP.put("yaml", YamlTextDocumentHandler.instance());
            }
        }
        return FILE_EXTENSION_TO_HANDLER_MAP.getOrDefault(fileExtension, EMPTY_TEXT_DOCUMENT_HANDLER);
    }

    /**
     * Get file extension for the given file.
     *
     * @param fileName File name.
     * @return File extension for the given file.
     */
    public static String getFileExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(name -> name.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1).toLowerCase())
                .orElse("");
    }
}
