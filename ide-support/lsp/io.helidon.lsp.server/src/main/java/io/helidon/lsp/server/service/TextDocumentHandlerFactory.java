package io.helidon.lsp.server.service;

import io.helidon.lsp.server.core.LanguageServerContext;
import io.helidon.lsp.server.service.properties.PropertiesTextDocumentHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TextDocumentHandlerFactory {

    private static final Map<String, TextDocumentHandler> fileExtensionToHandlerMap = new HashMap<>();
    private static final TextDocumentHandler emptyTextDocumentHandler = new EmptyTextDocumentHandler();

    public static TextDocumentHandler getByFileExtension(String fileName, LanguageServerContext languageServerContext) {
        final String fileExtension = getFileExtension(fileName);
        if (fileExtension.equals("properties")) {
            if (!fileExtensionToHandlerMap.containsKey("properties")) {
                fileExtensionToHandlerMap.put("properties", new PropertiesTextDocumentHandler(languageServerContext));
            }
        }
        return fileExtensionToHandlerMap.getOrDefault(fileExtension, emptyTextDocumentHandler);
    }

    public static String getFileExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(name -> name.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1).toLowerCase())
                .orElse("");
    }
}
