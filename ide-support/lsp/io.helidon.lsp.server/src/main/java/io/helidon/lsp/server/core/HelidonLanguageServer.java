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

package io.helidon.lsp.server.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.LogManager;

import io.helidon.lsp.server.service.config.ConfigurationPropertiesService;
import io.helidon.lsp.server.utils.FileUtils;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Helidon Language Server.
 */
public class HelidonLanguageServer implements LanguageServer, LanguageClientAware {
    private final TextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;
    private LanguageServerContext languageServerContext;
    private LanguageClient client;
    private int errorCode = 1;

    /**
     * Create a new instance.
     */
    public HelidonLanguageServer() {
        initContext();
        this.textDocumentService = new HelidonTextDocumentService(languageServerContext);
        this.workspaceService = new HelidonWorkspaceService(languageServerContext);
    }

    private void initContext() {
        languageServerContext = new LanguageServerContext();
        ConfigurationPropertiesService configurationPropertiesService = new ConfigurationPropertiesService();
        FileUtils fileUtils = new FileUtils();
        languageServerContext.setBean(ConfigurationPropertiesService.class, configurationPropertiesService);
        languageServerContext.setBean(FileUtils.class, fileUtils);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
        languageServerContext.setWorkspaceFolders(initializeParams.getWorkspaceFolders());

        // Initialize the InitializeResult for this LS.
        final InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());

        // Set the capabilities of the LS to inform the client.
        initializeResult.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
        CompletionOptions completionOptions = new CompletionOptions();
        initializeResult.getCapabilities().setCompletionProvider(completionOptions);
        return CompletableFuture.supplyAsync(() -> initializeResult);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        // If shutdown request comes from client, set the error code to 0.
        errorCode = 0;
        return null;
    }

    @Override
    public void exit() {
        // Kill the LS on exit request from client.
        System.exit(errorCode);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        // Return the endpoint for language features.
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        // Return the endpoint for workspace functionality.
        return this.workspaceService;
    }

    @Override
    public void connect(LanguageClient languageClient) {
        // Get the client which started this LS.
        this.client = languageClient;
    }
}
