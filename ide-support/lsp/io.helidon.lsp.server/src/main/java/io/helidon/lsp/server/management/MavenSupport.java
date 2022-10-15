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

package io.helidon.lsp.server.management;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.lsp.common.Dependency;

/**
 * Support operations with maven.
 */
public class MavenSupport {

    private static final Logger LOGGER = Logger.getLogger(MavenSupport.class.getName());
    private static final String POM_FILE_NAME = "pom.xml";
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final Gson GSON = new Gson();
    private static final String DEPENDENCIES_MVN_COMMAND = "io.helidon.ide-support" +
            ".lsp:helidon-lsp-maven-plugin:list-dependencies";
    private static final MavenSupport instance = new MavenSupport();
    ;

    private boolean isMavenInstalled = false;

    private MavenSupport() {
        initialize();
    }

    /**
     * Return instance of the MavenSupport class (singleton pattern).
     *
     * @return Instance of the MavenSupport class.
     */
    public static MavenSupport instance() {
        return instance;
    }

    private void initialize() {
        Path mavenPath = null;
        try {
            mavenPath = MavenCommand.mavenExecutable();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Maven is not installed in the system", e);
            return;
        }
        if (mavenPath != null) {
            isMavenInstalled = true;
        }
    }

    /**
     * Get information about all dependencies for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @param timeout time in milliseconds to wait for maven command execution.
     * @return List that contains information about the dependencies.
     */
    public Set<Dependency> getDependencies(final String pomPath, int timeout) {
        if (!isMavenInstalled) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            MavenCommand.builder()
                        .addArgument(DEPENDENCIES_MVN_COMMAND)
                        .addArgument("-Dport=" + serverSocket.getLocalPort())
                        .directory(new File(pomPath).getParentFile())
                        .verbose(false)
                        .build().execute();

            return CompletableFuture.supplyAsync(() -> {
                Set<Dependency> result = null;
                try (
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                ) {
                    result = GSON.fromJson(in, new TypeToken<Set<Dependency>>() {
                    }.getType());
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error when executing the maven command - " + DEPENDENCIES_MVN_COMMAND, e);
                }
                LOGGER.log(
                        Level.FINEST,
                        "getDependencies() for pom file {0} took {1} seconds",
                        new Object[]{pomPath, (double) (System.currentTimeMillis() - startTime) / 1000}
                );
                return result;
            }).get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error when executing the maven command - " + DEPENDENCIES_MVN_COMMAND, e);
        }
        LOGGER.log(
                Level.FINEST,
                "getDependencies() for pom file {0} took {1} seconds",
                new Object[]{pomPath, (double) (System.currentTimeMillis() - startTime) / 1000}
        );
        return null;
    }

    /**
     * Get information about all dependencies for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @return List that contains information about the dependencies.
     */
    public Set<Dependency> getDependencies(final String pomPath) {
        return getDependencies(pomPath, DEFAULT_TIMEOUT);
    }

    /**
     * Get pom file for the file from the maven project.
     *
     * @param fileName File name.
     * @return Get pom file for the given file or null if pom.xml is not found.
     * @throws IOException IOException
     */
    public String getPomForFile(final String fileName) throws IOException {
        final Path currentPath = Paths.get(fileName);
        Path currentDirPath;
        if (currentPath.toFile().isDirectory()) {
            currentDirPath = currentPath;
        } else {
            currentDirPath = currentPath.getParent();
        }
        String pomForDir = findPomForDir(currentDirPath);
        while (pomForDir == null && currentDirPath != null) {
            currentDirPath = currentDirPath.getParent();
            pomForDir = findPomForDir(currentDirPath);
        }
        return pomForDir;
    }

    private String findPomForDir(final Path directoryPath) throws IOException {
        if (directoryPath == null) {
            return null;
        }
        File[] listFiles = directoryPath.toFile().listFiles();
        if (listFiles == null) {
            return null;
        }
        return Arrays.stream(listFiles)
                     .filter(file ->
                             file.isFile() && file.getName().equals(POM_FILE_NAME))
                     .findFirst()
                     .map(File::getAbsolutePath).orElse(null);
    }

}
