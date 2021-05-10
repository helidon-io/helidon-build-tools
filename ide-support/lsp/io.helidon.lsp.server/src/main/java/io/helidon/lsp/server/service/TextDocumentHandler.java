package io.helidon.lsp.server.service;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;

import java.util.List;

public interface TextDocumentHandler {

    List<CompletionItem> completion(CompletionParams position);
}
