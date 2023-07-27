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

package io.helidon.lsp.server.management;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.maven.MavenCommand;
import io.helidon.lsp.common.Dependency;
import io.helidon.lsp.server.util.LanguageClientLogUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Support operations with maven.
 */
public class MavenSupport {

    private static final Logger LOGGER = Logger.getLogger(MavenSupport.class.getName());
    private static final String POM_FILE_NAME = "pom.xml";
    private static final String SRC_FOLDER = "src";
    private static final String MAIN_FOLDER = "main";
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final Gson GSON = new Gson();
    private static final String PLUGIN_GA = "io.helidon.build-tools.ide-support.lsp:helidon-lsp-maven-plugin";
    private static final String PLUGIN_GOAL = "list-dependencies";
    private static final MavenSupport INSTANCE = new MavenSupport();

    private String goal;
    private boolean isMavenInstalled;

    private MavenSupport() {
        try {
            MavenCommand.mavenExecutable();
            isMavenInstalled = true;
            String mavenVersion = getVersion();
            goal = PLUGIN_GA + (mavenVersion.isEmpty() ? "" : ":" + mavenVersion) + ":" + PLUGIN_GOAL;
        } catch (IllegalStateException e) {
            isMavenInstalled = false;
            String message = "Maven is not installed in the system";
            LOGGER.log(Level.SEVERE, message, e);
            LanguageClientLogUtil.logMessage(message, e);
        }
    }

    /**
     * Return instance of the MavenSupport class (singleton pattern).
     *
     * @return Instance of the MavenSupport class.
     */
    public static MavenSupport instance() {
        return INSTANCE;
    }

    /**
     * Get information about all dependencies for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @param timeout time in milliseconds to wait for maven command execution.
     * @return Set that contains information about the dependencies.
     */
    public Set<Dependency> dependencies(String pomPath, int timeout) {
        if (!isMavenInstalled) {
            LOGGER.log(Level.WARNING, "It is not possible to get Helidon dependencies from the maven repository. Maven is not "
                    + "installed.");
            return Set.of();
        }
        long startTime = System.currentTimeMillis();
        MavenPrintStream output = new MavenPrintStream();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            MavenCommand.builder()
                    .addArgument(goal)
                    .addArgument("-Dport=" + serverSocket.getLocalPort())
                    .directory(new File(pomPath).getParentFile())
                    .verbose(false)
                    .stdOut(output)
                    .stdErr(output)
                    .build()
                    .execute();

            return CompletableFuture.supplyAsync(() -> {
                Set<Dependency> result;
                try (
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    result = GSON.fromJson(in, new TypeToken<Set<Dependency>>() {}.getType());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                LOGGER.log(Level.FINEST, "getDependencies() for pom file {0} took {1} seconds",
                           new Object[] {
                                   pomPath,
                                   (double) (System.currentTimeMillis() - startTime) / 1000
                           });
                return result;
            }).get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            String message = "Error when executing the maven command - " + goal
                    + System.lineSeparator()
                    + output.content().stream().collect(Collectors.joining());
            LanguageClientLogUtil.logMessage(message, e);
        }
        LOGGER.log(Level.FINEST, "getDependencies() for pom file {0} took {1} seconds",
                   new Object[] {
                           pomPath,
                           (double) (System.currentTimeMillis() - startTime) / 1000
                   });
        return Set.of();
    }

    private static String getVersion() {
        try {
            try (InputStream is = MavenSupport.class.getClassLoader().getResourceAsStream("version.properties")) {
                if (is == null) {
                    throw new IllegalStateException("'version.properties' resource not found");
                }
                Properties props = new Properties();
                props.load(is);
                String version = props.getProperty("helidon-lsp-server.version");
                if (version == null) {
                    throw new IllegalStateException("'helidon-lsp-server.version' property is null");
                }
                return version.trim();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unresolve to resolve version", ex);
        }
    }

    /**
     * Get information about all dependencies for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @return Set that contains information about the dependencies.
     */
    public Set<Dependency> dependencies(String pomPath) {
        return dependencies(pomPath, DEFAULT_TIMEOUT);
    }

    /**
     * Get pom file for the file from the maven project.
     *
     * @param fileName File name.
     * @return Get pom file for the given file or null if pom.xml is not found.
     */
    public String resolvePom(String fileName) {
        Path currentPath = Paths.get(fileName);
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

    private String findPomForDir(Path directoryPath) {
        if (directoryPath == null) {
            return null;
        }
        File[] listFiles = directoryPath.toFile().listFiles();
        if (listFiles == null) {
            return null;
        }
        String pomFile = Arrays.stream(listFiles)
                .filter(file -> file.isFile() && file.getName().equals(POM_FILE_NAME))
                .findFirst()
                .map(File::getAbsolutePath).orElse(null);
        if (pomFile != null) {
            Boolean mavenSrcFolder = Arrays.stream(listFiles)
                    .filter(file -> file.isDirectory() && file.getName().equals(SRC_FOLDER))
                    .findFirst()
                    .map(File::listFiles)
                    .map(files -> Stream.of(files)
                            .anyMatch(file -> file.isDirectory() && file.getName()
                                    .equals(MAIN_FOLDER)))
                    .orElse(false);
            if (!mavenSrcFolder) {
                pomFile = null;
            }
        }
        return pomFile;
    }

    private static class MavenPrintStream extends PrintStream {

        private final List<String> content = new ArrayList<>();

        MavenPrintStream() {
            super(new ByteArrayOutputStream());
        }

        @Override
        public void println(String string) {
            content.add(string);
        }

        @Override
        public void print(String string) {
            content.add(string);
        }

        List<String> content() {
            return content;
        }
    }
}
