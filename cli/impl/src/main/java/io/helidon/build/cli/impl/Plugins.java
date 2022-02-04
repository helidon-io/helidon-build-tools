/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.cli.plugin.Plugin;
import io.helidon.build.common.JavaProcessBuilder;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.ProcessMonitor.ProcessFailedException;
import io.helidon.build.common.ProcessMonitor.ProcessTimeoutException;
import io.helidon.build.common.Proxies;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;

import org.graalvm.nativeimage.ImageInfo;

import static io.helidon.build.cli.impl.CommandRequirements.unsupportedJavaVersion;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static java.util.Objects.requireNonNull;

/**
 * Utility to execute plugins.
 */
public class Plugins {

    private static final String EOL = System.lineSeparator();
    private static final AtomicReference<Path> PLUGINS_JAR = new AtomicReference<>();
    private static final String JAR_NAME_PREFIX = "cli-plugins-";
    private static final String JAR_NAME_SUFFIX = ".jar";
    private static final String JAR_RESOURCE_DIR = "plugins";
    private static final String DEBUG_PORT_PROPERTY = "plugin.debug.port";
    private static final int DEFAULT_DEBUG_PORT = Integer.getInteger(DEBUG_PORT_PROPERTY, 0);
    private static final String DEBUG_ARG_PREFIX = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:";
    private static final boolean FORK = Boolean.getBoolean("plugin.fork");
    private static final String JIT_LEVEL_ONE = "-XX:TieredStopAtLevel=1";
    private static final String JIT_TWO_COMPILER_THREADS = "-XX:CICompilerCount=2";
    private static final String TIMED_OUT_SUFFIX = " timed out";
    private static final String UNSUPPORTED_CLASS_VERSION_ERROR = UnsupportedClassVersionError.class.getSimpleName();

    private static Path pluginJar() {
        Path pluginJar = PLUGINS_JAR.get();
        if (pluginJar == null) {
            final String cliVersion = Config.buildRevision();
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
     * @throws PluginFailed if the execution fails
     */
    public static void execute(String pluginName,
                               List<String> pluginArgs,
                               int maxWaitSeconds) throws PluginFailed {

        execute(pluginName, pluginArgs, maxWaitSeconds, STDOUT);
    }

    /**
     * Execute a plugin.
     * If executing inside a native executable, the plugin execution is done by spawning a Java process using the
     * bundled plugin JAR file. Otherwise, the execution is done in the current JVM.
     *
     * @param pluginName     The plugin name.
     * @param pluginArgs     The plugin args.
     * @param maxWaitSeconds If spawned, the maximum number of seconds to wait for completion.
     * @param stdOut         The std out consumer.
     * @throws PluginFailed if the execution fails
     */
    public static void execute(String pluginName,
                               List<String> pluginArgs,
                               int maxWaitSeconds,
                               PrintStream stdOut) throws PluginFailed {

        if (FORK || ImageInfo.inImageRuntimeCode()) {
            spawned(pluginName, pluginArgs, maxWaitSeconds, stdOut);
        } else {
            embedded(pluginName, pluginArgs, stdOut);
        }
    }

    private static List<String> pluginArgs(String pluginName, List<String> pluginArgs) {
        List<String> args = new ArrayList<>();
        args.add(requireNonNull(pluginName));
        if (LogLevel.isDebug() && !pluginArgs.contains("--debug")) {
            args.add("--debug");
        } else if (LogLevel.isVerbose() && !pluginArgs.contains("--verbose")) {
            args.add("--verbose");
        }
        args.addAll(pluginArgs);
        return args;
    }

    private static void embedded(String pluginName,
                                 List<String> pluginArgs,
                                 PrintStream stdOut) throws PluginFailed {
        try {
            List<String> command = pluginArgs(pluginName, pluginArgs);
            Plugin.execute(command.toArray(new String[0]), stdOut::println);
        } catch (Plugin.Failed ex) {
            if (ex.getCause() != null) {
                throw new PluginFailed(ex.getCause());
            } else {
                throw new PluginFailed(ex);
            }
        } catch (Exception ex) {
            throw new PluginFailed(ex);
        }
    }

    private static void spawned(String pluginName,
                                List<String> pluginArgs,
                                int maxWaitSeconds,
                                PrintStream stdOut) throws PluginFailed {

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
        command.addAll(pluginArgs(pluginName, pluginArgs));

        // Create the process builder

        final ProcessBuilder processBuilder = JavaProcessBuilder.newInstance().command(command);
        ProcessMonitor process = ProcessMonitor.builder()
                                               .processBuilder(processBuilder)
                                               .stdOut(stdOut)
                                               .capture(true)
                                               .build();

        try {
            Log.debug("executing %s", command);
            process.execute(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (ProcessFailedException error) {
            if (process.stdErr().contains(UNSUPPORTED_CLASS_VERSION_ERROR)) {
                unsupportedJavaVersion();
            } else {
                StringBuilder b = new StringBuilder();
                for (String line : error.monitor().output().split("\\R")) {
                    if (b.length() > 0) {
                        b.append(EOL);
                    }
                    b.append(line);
                }
                throw new PluginFailedUnchecked(b.toString());
            }
        } catch (ProcessTimeoutException error) {
            throw new PluginFailed(pluginName + TIMED_OUT_SUFFIX);
        } catch (Exception e) {
            if (process.stdErr().isEmpty()) {
                throw new PluginFailed(e);
            } else {
                throw new PluginFailed(String.join(EOL, process.stdErr()), e);
            }
        }
    }

    /**
     * Plugin failure unchecked exception.
     * This is a runtime exception used when the error has already been logged.
     *
     * @see PluginFailed for the checked variant
     */
    public static class PluginFailedUnchecked extends RuntimeException {

        private PluginFailedUnchecked(String message) {
            super(message);
        }
    }

    /**
     * Plugin failure.
     */
    public static class PluginFailed extends Exception {
        private PluginFailed(String message) {
            super(message);
        }

        private PluginFailed(Throwable cause) {
            super(cause.getMessage(), cause);
        }

        private PluginFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private Plugins() {
    }
}
