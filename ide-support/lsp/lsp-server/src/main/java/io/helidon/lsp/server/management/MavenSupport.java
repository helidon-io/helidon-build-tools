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

import io.helidon.build.common.maven.MavenCommand;
import io.helidon.lsp.common.Dependency;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Support operations with maven.
 */
public class MavenSupport {

    private static final Logger LOGGER = Logger.getLogger(MavenSupport.class.getName());
    private static final String POM_FILE_NAME = "pom.xml";
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final Gson GSON = new Gson();
    private static final String LSP_MAVEN_PLUGIN = "io.helidon.ide-support.lsp:helidon-lsp-maven-plugin";
    private static final String DEPENDENCIES_GOAL = "list-dependencies";
    private static final MavenSupport INSTANCE = new MavenSupport();

    private static String mavenVersion;
    private static String lspMvnDependenciesCommand;

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
        return INSTANCE;
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
        mavenVersion = getMavenVersion();
        lspMvnDependenciesCommand =
                LSP_MAVEN_PLUGIN + (mavenVersion.isEmpty() ? "" : ":" + mavenVersion) + ":" + DEPENDENCIES_GOAL;
    }

    /**
     * Get information about all dependencies for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @param timeout time in milliseconds to wait for maven command execution.
     * @return List that contains information about the dependencies.
     */
    public Set<Dependency> dependencies(final String pomPath, int timeout) {
        if (!isMavenInstalled) {
            LOGGER.log(Level.WARNING, "It is not possible to get Helidon dependencies from the maven repository. Maven is not "
                    + "installed.");
            return null;
        }
        long startTime = System.currentTimeMillis();
        MavenPrintStream output = new MavenPrintStream();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            MavenCommand.builder()
                        .addArgument(lspMvnDependenciesCommand)
                        .addArgument("-Dport=" + serverSocket.getLocalPort())
                        .directory(new File(pomPath).getParentFile())
                        .verbose(false)
                        .stdOut(output)
                        .stdErr(output)
                        .build()
                        .execute();

            return CompletableFuture.supplyAsync(() -> {
                Set<Dependency> result = null;
                try (
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                ) {
                    result = GSON.fromJson(in, new TypeToken<Set<Dependency>>() {
                    }.getType());
                } catch (IOException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Error when executing the maven command - " + lspMvnDependenciesCommand + System.lineSeparator()
                                    + output.content().stream().collect(Collectors.joining(System.lineSeparator())),
                            e
                    );
                }
                LOGGER.log(
                        Level.FINEST,
                        "getDependencies() for pom file {0} took {1} seconds",
                        new Object[]{pomPath, (double) (System.currentTimeMillis() - startTime) / 1000}
                );
                return result;
            }).get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error when executing the maven command - " + lspMvnDependenciesCommand + System.lineSeparator()
                            + output.content().stream().collect(Collectors.joining(System.lineSeparator())),
                    e
            );
        }
        LOGGER.log(
                Level.FINEST,
                "getDependencies() for pom file {0} took {1} seconds",
                new Object[]{pomPath, (double) (System.currentTimeMillis() - startTime) / 1000}
        );
        return null;
    }

    private static String getMavenVersion() {
        final Properties properties = new Properties();
        final String corePomProperties = "META-INF/maven/io.helidon.ide-support.lsp/helidon-lsp-server/pom.properties";

        try (InputStream in = MavenSupport.class.getClassLoader().getResourceAsStream(corePomProperties)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ioe) {
            return "";
        }

        String version = properties.getProperty("version");
        return version == null ? "" : version.trim();
    }

    /**
     * Get information about all dependencies for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @return List that contains information about the dependencies.
     */
    public Set<Dependency> dependencies(final String pomPath) {
        return dependencies(pomPath, DEFAULT_TIMEOUT);
    }

    /**
     * Get pom file for the file from the maven project.
     *
     * @param fileName File name.
     * @return Get pom file for the given file or null if pom.xml is not found.
     * @throws IOException IOException
     */
    public String resolvePom(final String fileName) throws IOException {
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
