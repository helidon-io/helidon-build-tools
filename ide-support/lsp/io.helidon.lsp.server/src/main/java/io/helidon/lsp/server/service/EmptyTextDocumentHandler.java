package io.helidon.lsp.server.service;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;

import java.util.Collections;
import java.util.List;

public class EmptyTextDocumentHandler implements TextDocumentHandler {
    @Override
    public List<CompletionItem> completion(CompletionParams position) {
        return Collections.emptyList();
    }
}
