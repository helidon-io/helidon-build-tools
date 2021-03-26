/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.helidon.build.util.JavaProcessBuilder;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.build.util.Proxies;

import static io.helidon.build.cli.impl.CommandRequirements.unsupportedJavaVersion;
import static io.helidon.build.util.Constants.EOL;
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
    private static final String JIT_LEVEL_ONE = "-XX:TieredStopAtLevel=1";
    private static final String JIT_TWO_COMPILER_THREADS = "-XX:CICompilerCount=2";
    private static final String TIMED_OUT_SUFFIX = " timed out";
    private static final String UNSUPPORTED_CLASS_VERSION_ERROR = UnsupportedClassVersionError.class.getSimpleName();
    private static final String PLUGIN_CLASS_NAME = "io.helidon.build.cli.plugin.Plugin";

    private static Path pluginJar() {
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
                try {
                    Files.copy(input, pluginJar);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            PLUGINS_JAR.set(pluginJar.toAbsolutePath());
        }
        return pluginJar;
    }

    /**
     * Resets state.
     *
     * @param deleteJar {@code true} if plugin jar should be deleted if present.
     */
    static void reset(boolean deleteJar) {
        if (deleteJar) {
            final Path existing = PLUGINS_JAR.get();
            if (existing != null && Files.exists(existing)) {
                try {
                    Files.delete(existing);
                } catch (IOException e) {
                    Log.warn("Could not delete %s: %s", existing, e.toString());
                }
            }
        }
        PLUGINS_JAR.set(null);
    }

    /**
     * Execute a plugin and wait for it to complete.
     *
     * @param pluginName     The plugin name.
     * @param pluginArgs     The plugin args.
     * @param maxWaitSeconds The maximum number of seconds to wait for completion.
     */
    public static void execute(String pluginName,
                               List<String> pluginArgs,
                               int maxWaitSeconds) {
        execute(pluginName, pluginArgs, maxWaitSeconds, Plugins::devNull);
    }

    /**
     * Execute a plugin.
     * If the plugin class is available on the class-path, the plugin is executed via reflection.
     * Otherwise a process is forked and monitored until completion.
     *
     * @param pluginName     The plugin name.
     * @param pluginArgs     The plugin args.
     * @param maxWaitSeconds If forked, the maximum number of seconds to wait for completion.
     * @param stdOut         The std out consumer.
     */
    public static void execute(String pluginName,
                               List<String> pluginArgs,
                               int maxWaitSeconds,
                               Consumer<String> stdOut) {

        try {
            embedded(pluginName, pluginArgs, stdOut);
        } catch (ClassNotFoundException ignored) {
            forked(pluginName, pluginArgs, maxWaitSeconds, stdOut);
        }
    }

    private static void embedded(String pluginName,
                                 List<String> pluginArgs,
                                 Consumer<String> stdOut) throws ClassNotFoundException {

        Class<?> pluginClass = Class.forName(PLUGIN_CLASS_NAME);
        PrintStream origStdOut = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));
            List<String> command = new ArrayList<>();
            command.add(requireNonNull(pluginName));
            if (Log.isDebug()) {
                command.add("--debug");
            } else if (Log.isVerbose()) {
                command.add("--verbose");
            }
            command.addAll(pluginArgs);
            Method method = pluginClass.getMethod("execute", String[].class);
            method.invoke(null, new Object[]{command.toArray(new String[0])});
            try (BufferedReader reader = new BufferedReader(new StringReader(baos.toString()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdOut.accept(line);
                }
            } catch (IOException ex) {
                throw new PluginFailed(ex);
            }
        } catch (InvocationTargetException ex) {
            if (ex.getCause() != null) {
                throw new PluginFailed(ex.getCause());
            } else {
                throw new PluginFailed(ex);
            }
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new PluginFailed(ex);
        } finally {
            System.setOut(origStdOut);
        }
    }

    private static void forked(String pluginName,
                               List<String> pluginArgs,
                               int maxWaitSeconds,
                               Consumer<String> stdOut) {

        // Create the command
        final List<String> command = new ArrayList<>();
        command.add("java");
        command.add(JIT_LEVEL_ONE);
        command.add(JIT_TWO_COMPILER_THREADS);
        if (DEFAULT_DEBUG_PORT > 0) {
            command.add(DEBUG_ARG_PREFIX + DEFAULT_DEBUG_PORT);
        }
        command.addAll(Proxies.javaProxyArgs());
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

        final ProcessBuilder processBuilder = JavaProcessBuilder.newInstance().command(command);

        // Fork and wait...

        Log.debug("executing %s", command);

        final List<String> stdErr = new ArrayList<>();
        try {
            ProcessMonitor.builder()
                          .processBuilder(processBuilder)
                          .stdOut(stdOut)
                          .stdErr(stdErr::add)
                          .capture(true)
                          .build()
                          .start()
                          .waitForCompletion(maxWaitSeconds, TimeUnit.SECONDS);
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
    public static class PluginFailed extends RuntimeException {
        private PluginFailed(String message) {
            super(message);
        }

        private PluginFailed(Throwable cause) {
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
