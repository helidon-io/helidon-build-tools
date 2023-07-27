/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.common.FileUtils;
import io.helidon.lsp.server.util.LanguageClientLogUtil;

/**
 * Stores the last changes in the traceable project files.
 */
public class ContentManager {

    private static final Logger LOGGER = Logger.getLogger(ContentManager.class.getName());
    private static final ContentManager INSTANCE = new ContentManager();

    private final Map<String, String> fileToTmpMap = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    private Path filesFolder;

    private ContentManager() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        filesFolder = Paths.get(tmpDir, "vscode-helidon", "server", "files");
        try {
            filesFolder = Files.createDirectories(filesFolder);
        } catch (IOException e) {
            String message = String.format("Cannot create directory %s", filesFolder.toString());
            LOGGER.log(Level.SEVERE, message);
            LanguageClientLogUtil.logMessage(message, e);
            filesFolder = Paths.get(tmpDir);
        }
    }

    /**
     * Get the instance of the class.
     *
     * @return instance of the class.
     */
    public static ContentManager instance() {
        return INSTANCE;
    }

    /**
     * Read content of the file.
     *
     * @param fileUri file name.
     * @return content of the file.
     * @throws IOException if an IO error occurs
     * @throws URISyntaxException if a URI error occurs
     */
    public List<String> read(String fileUri) throws IOException, URISyntaxException {
        LOGGER.finest("read() started with thread " + Thread.currentThread().getName());
        try {
            lock.lock();  // block until condition holds
            String tempFile = tempFile(fileUri);
            return FileUtils.readAllLines(new URI(tempFile));
        } finally {
            lock.unlock();
            LOGGER.finest("read() finished with thread " + Thread.currentThread().getName());
        }
    }

    /**
     * Register file in the instance of this class.
     *
     * @param fileName file name.
     * @return true if file was registered.
     */
    public boolean register(String fileName) {
        try {
            lock.lock();  // block until condition holds
            LOGGER.finest("register() started with thread " + Thread.currentThread().getName());
            if (fileName == null) {
                return false;
            }
            String tempFile = tempFile(fileName);
            return !tempFile.equals(fileName);
        } finally {
            lock.unlock();
            LOGGER.finest("register() finished with thread " + Thread.currentThread().getName());
        }
    }

    private String tempFile(String fileUri) {
        String tempFile = fileToTmpMap.computeIfAbsent(fileUri, this::createNewTempFile);
        if (tempFile == null) {
            return fileUri;
        }
        return tempFile;
    }

    private String createNewTempFile(String fileUri) {
        try {
            String fileName = fileUri.substring(fileUri.lastIndexOf('/') + 1);
            Path tmp = Files.createTempFile(filesFolder, "", fileName);
            tmp.toFile().deleteOnExit();
            List<String> content = FileUtils.readAllLines(new URI(fileUri));
            Files.write(tmp, content);
            return tmp.toUri().toString();
        } catch (URISyntaxException | IOException e) {
            String message = String.format("Cannot create temp file for %s. Exception - %s", fileUri, e.getMessage());
            LOGGER.log(Level.SEVERE, message);
            LanguageClientLogUtil.logMessage(message, e);
            return null;
        }
    }

    /**
     * Write content of the file or its new changes to the instance of this class.
     *
     * @param fileName file name.
     * @param content  content of the file.
     * @param options  OpenOption for the file.
     * @throws IOException if an IO error occurs
     * @throws URISyntaxException if a URI error occurs
     */
    public void write(String fileName, List<String> content, OpenOption... options) throws IOException, URISyntaxException {
        try {
            lock.lock();  // block until condition holds
            LOGGER.finest("write() started with thread " + Thread.currentThread().getName());
            String tempFile = tempFile(fileName);
            if (fileName.equals(tempFile)) {
                //temp file was nor created
                return;
            }
            //rewrite the content of the file
            Files.write(Path.of(new URI(tempFile)), content, options);
        } finally {
            lock.unlock();
            LOGGER.finest("write() finished with thread " + Thread.currentThread().getName());
        }
    }
}
