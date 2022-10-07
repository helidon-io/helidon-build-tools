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

package io.helidon.lsp.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import io.helidon.lsp.server.core.HelidonLanguageServer;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Launcher for Helidon language server.
 */
public class HelidonLanguageServerLauncher {

    private static final Logger LOGGER = Logger.getLogger(HelidonLanguageServerLauncher.class.getName());

    private HelidonLanguageServerLauncher() {
    }

    /**
     * Main method for the application that starts Helidon language server.
     *
     * @param args arguments
     * @throws ExecutionException   ExecutionException
     * @throws InterruptedException InterruptedException
     * @throws IOException          IOException
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            Path logFolder = Paths.get(tmpDir,"vscode-helidon","logs","server");
            Files.createDirectories(logFolder);
            LogManager.getLogManager().readConfiguration(new FileInputStream("logging.properties"));
        } catch (SecurityException | IOException e1) {
            LOGGER.warning(e1.getMessage());
        }
        // start the language server
        startServer(Integer.parseInt(args[0]));
    }

    /**
     * Start the language server.
     *
     * @param port System input stream
     * @throws ExecutionException   Unable to start the server
     * @throws InterruptedException Unable to start the server
     */
    private static void startServer(int port) throws ExecutionException, InterruptedException, IOException {
        // Initialize the language server
        HelidonLanguageServer languageServer = new HelidonLanguageServer();

        ServerSocket serverSocket = new ServerSocket(port);
        LOGGER.info("Helidon language server started at port - " + port);
        Socket socket = serverSocket.accept();

        // Create JSON RPC launcher for LS instance.
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(languageServer, socket.getInputStream(),
                socket.getOutputStream());
        // Get the client that request to launch the LS.
        LanguageClient client = launcher.getRemoteProxy();
        // Set the client to language server
        languageServer.connect(client);
        LOGGER.info("Client connected to the Helidon language server");
        // Start the listener for JsonRPC
        Future<?> startListening = launcher.startListening();
        // Get the computed result from LS.
        startListening.get();
        LOGGER.info("Helidon language server stopped");
    }
}
