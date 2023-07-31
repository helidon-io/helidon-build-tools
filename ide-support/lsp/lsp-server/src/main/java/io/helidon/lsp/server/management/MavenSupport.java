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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.helidon.build.common.maven.MavenCommand;
import io.helidon.lsp.common.Dependency;
import io.helidon.lsp.server.util.LanguageClientLogUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Support operations with maven.
 */
public class MavenSupport {

    private static final Logger LOGGER = Logger.getLogger(MavenSupport.class.getName());
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final Gson GSON = new Gson();
    private static final String PLUGIN_GA = "io.helidon.build-tools.ide-support.lsp:helidon-lsp-maven-plugin";
    private static final String PLUGIN_GOAL = "list-dependencies";
    private static final MavenSupport INSTANCE = new MavenSupport();
    private static final Type DEPENDENCIES_TYPE = new TypeToken<Set<Dependency>>() { }.getType();

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
     * @param pom {@code pom.xml}
     * @param timeout time in milliseconds to wait for maven command execution.
     * @param argument optional argument to pass to the maven command, may be {@code null}
     * @return Set that contains information about the dependencies.
     */
    public Set<Dependency> dependencies(Path pom, int timeout, String argument) {
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
                    .addOptionalArgument(argument)
                    .addArgument("-Dport=" + serverSocket.getLocalPort())
                    .directory(pom.getParent())
                    .verbose(false)
                    .stdOut(output)
                    .stdErr(output)
                    .build()
                    .execute();

            Set<Dependency> dependencies = supplyAsync(() -> recv(serverSocket)).get(timeout, TimeUnit.MILLISECONDS);

            LOGGER.log(Level.FINEST, "getDependencies() for pom file {0} took {1} seconds",
                       new Object[] {
                               pom,
                               (double) (System.currentTimeMillis() - startTime) / 1000
                       });

            return dependencies;
        } catch (Exception e) {
            String message = "Error when executing the maven command - " + goal
                    + System.lineSeparator()
                    + String.join("", output.content());
            LanguageClientLogUtil.logMessage(message, e);
            return Set.of();
        }
    }

    private static Set<Dependency> recv(ServerSocket serverSocket) {
        try (Socket clientSocket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), UTF_8))) {
            return GSON.fromJson(reader, DEPENDENCIES_TYPE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get information about all dependencies for the given pom file.
     *
     * @param pom {@code pom.xml}
     * @return Set that contains information about the dependencies.
     */
    public Set<Dependency> dependencies(Path pom) {
        return dependencies(pom, DEFAULT_TIMEOUT, null);
    }

    /**
     * Get the {@code pom.xml} for a project file.
     *
     * @param path project file
     * @return {@code pom.xml} or {@code null} if not found
     */
    public static Path resolvePom(Path path) {
        Path absolute = path.toAbsolutePath();
        Path directory = Files.isDirectory(path) ? absolute : absolute.getParent();
        while (directory != null) {
            Path pom = findPomForDir(directory);
            if (pom != null) {
                return pom.toAbsolutePath();
            }
            directory = directory.getParent();
        }
        return null;
    }

    private static Path findPomForDir(Path directory) {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().equals("pom.xml"))
                    .filter(file -> Files.isDirectory(file.getParent().resolve("src/main")))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private static class MavenPrintStream extends PrintStream {

        private final List<String> content = new ArrayList<>();

        MavenPrintStream() {
            super(new ByteArrayOutputStream(), false, UTF_8);
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
