/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.helidon.build.util.Constants;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.build.util.Proxies;

import static io.helidon.build.cli.impl.CommandRequirements.unsupportedJavaVersion;
import static io.helidon.build.util.Constants.EOL;
import static java.io.File.pathSeparatorChar;
import static java.util.Objects.requireNonNull;

/**
 * Utility to execute plugins.
 */
public class Plugins {
    private static final AtomicReference<Path> PLUGINS_JAR = new AtomicReference<>();
    private static final String JAR_NAME_PREFIX = "cli-plugins-";
    private static final String JAR_NAME_SUFFIX = ".jar";
    private static final String JAR_RESOURCE_DIR = "plugins";
    private static final String DEBUG_PORT_PROPERTY = "plugin.debug.port";
    private static final int DEFAULT_DEBUG_PORT = Integer.getInteger(DEBUG_PORT_PROPERTY, 0);
    private static final String DEBUG_ARG_PREFIX = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:";
    private static final String JAVA_HOME = Constants.javaHome();
    private static final String JAVA_HOME_BIN = JAVA_HOME + File.separator + "bin";
    private static final String PATH_VAR = "PATH";
    private static final String JAVA_HOME_VAR = "JAVA_HOME";
    private static final String JIT_LEVEL_ONE = "-XX:TieredStopAtLevel=1";
    private static final String JIT_TWO_COMPILER_THREADS = "-XX:CICompilerCount=2";
    private static final String TIMED_OUT_SUFFIX = " timed out";
    private static final String UNSUPPORTED_CLASS_VERSION_ERROR = UnsupportedClassVersionError.class.getSimpleName();

    private static Path pluginJar() throws Exception {
        Path pluginJar = PLUGINS_JAR.get();
        if (pluginJar == null) {
            final String cliVersion = Config.buildProperties().buildRevision();
            final String jarName = JAR_NAME_PREFIX + cliVersion + JAR_NAME_SUFFIX;
            pluginJar = Config.userConfig().pluginsDir().resolve(jarName);
            if (!Files.exists(pluginJar)) {
                final String resourcePath = JAR_RESOURCE_DIR + "/" + jarName;
                final ClassLoader loader = Plugins.class.getClassLoader();
                final InputStream input = requireNonNull(loader.getResourceAsStream(resourcePath), resourcePath + " not found!");
                Log.debug("unpacked %s", pluginJar);
                Files.copy(input, pluginJar);
            }
            PLUGINS_JAR.set(pluginJar.toAbsolutePath());
        }
        return pluginJar;
    }

    /**
     * Removes any cached plugin jar.
     */
    static void clearPluginJar() {
        PLUGINS_JAR.set(null);
    }

    /**
     * Execute a plugin and wait for it to complete.
     *
     * @param pluginName The plugin name.
     * @param pluginArgs The plugin args.
     * @param maxWaitSeconds The maximum number of seconds to wait for completion.
     * @throws Exception If an error occurs.
     */
    public static void execute(String pluginName,
                               List<String> pluginArgs,
                               int maxWaitSeconds) throws Exception {
        execute(pluginName, pluginArgs, maxWaitSeconds, Plugins::devNull);
    }

    /**
     * Execute a plugin and wait for it to complete.
     *
     * @param pluginName The plugin name.
     * @param pluginArgs The plugin args.
     * @param maxWaitSeconds The maximum number of seconds to wait for completion.
     * @param stdOut The std out consumer.
     * @throws Exception If an error occurs.
     */
    public static void execute(String pluginName,
                               List<String> pluginArgs,
                               int maxWaitSeconds,
                               Consumer<String> stdOut) throws Exception {

        // Create the command

        final List<String> command = new ArrayList<>();
        command.add("java");
        command.add(JIT_LEVEL_ONE);
        command.add(JIT_TWO_COMPILER_THREADS);
        if (DEFAULT_DEBUG_PORT > 0) {
            command.add(DEBUG_ARG_PREFIX + DEFAULT_DEBUG_PORT);
        }
        final String proxyArgs = Proxies.javaProxyArgs();
        if (proxyArgs != null && !proxyArgs.isEmpty()) {
            command.add(proxyArgs);
        }
        command.add("-jar");
        command.add(pluginJar().toString());
        command.add(requireNonNull(pluginName));
        if (Log.isDebug()) {
            command.add("--debug");
        } else if (Log.isVerbose()) {
            command.add("--verbose");
        }
        command.addAll(pluginArgs);

        // Create the process builder

        final ProcessBuilder processBuilder = new ProcessBuilder().command(command);

        // Ensure we use the current Java versions

        final Map<String, String> env = processBuilder.environment();
        String path = JAVA_HOME_BIN + pathSeparatorChar + env.get(PATH_VAR);
        env.put(PATH_VAR, path);
        env.put(JAVA_HOME_VAR, JAVA_HOME);

        // Fork and wait...

        Log.debug("executing %s", command);

        final List<String> stdErr = new ArrayList<>();
        ProcessMonitor processMonitor = ProcessMonitor.builder()
                                                      .processBuilder(processBuilder)
                                                      .stdOut(stdOut)
                                                      .stdErr(stdErr::add)
                                                      .capture(true)
                                                      .build()
                                                      .start();
        try {
            processMonitor.waitForCompletion(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (ProcessMonitor.ProcessFailedException error) {
            if (containsUnsupportedClassVersionError(stdErr)) {
                unsupportedJavaVersion();
            } else {
                throw new PluginFailed(String.join(EOL, error.monitor().output()));
            }
        } catch (ProcessMonitor.ProcessTimeoutException error) {
            throw new PluginFailed(pluginName + TIMED_OUT_SUFFIX);
        } catch (Exception e) {
            if (stdErr.isEmpty()) {
                throw new PluginFailed(e);
            } else {
                throw new PluginFailed(String.join(EOL, stdErr), e);
            }
        }
    }

    /**
     * Plugin failure.
     */
    public static class PluginFailed extends Exception {
        private PluginFailed(String message) {
            super(message);
        }

        private PluginFailed(Exception cause) {
            super(cause);
        }

        private PluginFailed(String message, Exception cause) {
            super(message, cause);
        }
    }

    private static boolean containsUnsupportedClassVersionError(List<String> stdErr) {
        return stdErr.stream().anyMatch(line -> line.contains(UNSUPPORTED_CLASS_VERSION_ERROR));
    }

    private static void devNull(String line) {
    }

    private Plugins() {
    }
}
