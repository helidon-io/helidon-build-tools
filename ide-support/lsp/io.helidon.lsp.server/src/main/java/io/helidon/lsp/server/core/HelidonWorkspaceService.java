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

import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import io.helidon.build.archetype.v2.json.ScriptSerializer;
import io.helidon.build.common.VirtualFileSystem;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Implementation of the WorkspaceService interface that is used in LSP communication between the server and clients.
 */
public class HelidonWorkspaceService implements WorkspaceService {

    private static final Logger LOGGER = Logger.getLogger(HelidonTextDocumentService.class.getName());

    /**
     * Create a new instance.
     */
    public HelidonWorkspaceService() {
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        String command = params.getCommand();
        try {
            Supplier<Object> result = () -> "";
            if (command != null && command.equals("helidon.archetype.v2.json")) {
                JsonObject jsonObject = prepareArchetypeJson();
                result = () -> jsonObject != null ? jsonObject.toString() : "";
            }
            return CompletableFuture.supplyAsync(result);
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Exception when trying to execute " + command, e);
            throw new RuntimeException(e);
        }
    }

    private JsonObject prepareArchetypeJson() throws URISyntaxException {
        Path archetypeDir = archetypeDir();
        FileSystem fs = VirtualFileSystem.create(archetypeDir);
        return ScriptSerializer.serialize(fs);
    }

    private Path archetypeDir() throws URISyntaxException {
        Path codeSource = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        return codeSource.getParent().resolve("archetype");
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {

    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {
        //happen when watched files are saved
        LOGGER.log(
                Level.FINEST,
                () -> "didChangeWatchedFiles(), save the files "
                        + didChangeWatchedFilesParams
                        .getChanges().stream()
                        .map(FileEvent::getUri)
                        .collect(Collectors.joining(", ", "[", "]"))
        );
    }
}
