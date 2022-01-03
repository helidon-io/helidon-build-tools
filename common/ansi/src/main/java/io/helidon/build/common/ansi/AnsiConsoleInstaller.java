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

package io.helidon.build.common.ansi;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.common.Log;

import org.fusesource.jansi.Ansi;
import picocli.jansi.graalvm.AnsiConsole;

/**
 * Installer for {@link System#out} and {@link System#err} streams that support {@link Ansi} escapes, if possible.
 * Supports {@code GraalVM} native executables.
 */
public class AnsiConsoleInstaller {

    /**
     * The Helidon child process property name.
     */
    public static final String HELIDON_CHILD_PROCESS_PROPERTY = "helidon.child.process";

    /**
     * The system property that, if {@code true}, will bypass the default check
     * to see if stdin is a terminal.
     */
    private static final String JANSI_FORCE_PROPERTY = "jansi.force";

    /**
     * The system property that, if {@code true}, will install streams that strip Ansi escapes.
     */
    private static final String JANSI_STRIP_PROPERTY = "jansi.strip";

    /**
     * The system property that, if {@code true}, will not install streams.
     */
    private static final String JANSI_PASS_THROUGH_PROPERTY = "jansi.passthrough";

    // Note: Class instances are not used here since this class is used within a maven plugin
    //       that might have a different version of Jansi
    private static final String JANSI_PACKAGE_PREFIX = "org.fusesource.jansi";
    private static final String JANSI_STRIP_STREAM_CLASS_NAME = "org.fusesource.jansi.AnsiPrintStream";
    private static final boolean IS_HELIDON_CHILD_PROCESS = Boolean.getBoolean(HELIDON_CHILD_PROCESS_PROPERTY);
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static final AtomicReference<ConsoleType> CONSOLE_TYPE = new AtomicReference<>();
    private static final AtomicBoolean ENABLED = new AtomicBoolean();

    /**
     * Enables use of Ansi escapes and that the system streams have been installed, if possible.
     * There are a few reasons why this may not result in enabling Ansi escapes:
     * <ol>
     *     <li>The {@code System.in} stream is not a tty (i.e. not connected to a terminal)</li>
     *     <li>The {@code jansi.strip} system property is set to {@code true}</li>
     *     <li>The operating system is not supported</li>
     * </ol>
     *
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean install() {
        if (!INSTALLED.getAndSet(true)) {
            ConsoleType desiredType = desiredConsoleType();
            AnsiConsole.systemInstall();
            ConsoleType installedType = installedConsoleType(desiredType);
            CONSOLE_TYPE.set(installedType);
            ENABLED.set(installedType == ConsoleType.ANSI);
        }
        return ENABLED.get();
    }

    /**
     * Disable use of Ansi escapes if not already enabled.
     *
     * @throws IllegalStateException If Ansi escapes are already enabled.
     */
    public static void disable() {
        if (INSTALLED.get()) {
            if (ENABLED.get()) {
                throw new IllegalStateException("Color support is already enabled");
            }
        } else {
            INSTALLED.set(true);
            ENABLED.set(false);
            CONSOLE_TYPE.set(ConsoleType.DEFAULT);
            System.setProperty(JANSI_STRIP_PROPERTY, "true");
            Ansi.setEnabled(false);
        }
    }

    /**
     * Console types.
     */
    public enum ConsoleType {
        /**
         * Support Ansi escapes.
         */
        ANSI(JANSI_FORCE_PROPERTY),

        /**
         * Strip Ansi escapes.
         */
        STRIP_ANSI(JANSI_STRIP_PROPERTY),

        /**
         * Do not explicitly support or strip Ansi escapes.
         */
        DEFAULT(JANSI_PASS_THROUGH_PROPERTY);

        private final String argument;

        ConsoleType(String propertyName) {
            this.argument = "-D" + HELIDON_CHILD_PROCESS_PROPERTY + "=true" + " -D" + propertyName + "=true";
        }

        /**
         * Returns the command-line argument that forces Ansi escapes to be handled in a child process
         * the same way as they are in this one.
         *
         * @return The argument.
         */
        public String childProcessArgument() {
            return argument;
        }
    }

    /**
     * Returns the command-line argument that forces Ansi escapes to be handled in a child process
     * the same way as they are in this one.
     *
     * @return The argument.
     */
    public static String childProcessArgument() {
        return consoleType().childProcessArgument();
    }

    /**
     * Indicates if this process is a child of another Helidon process.
     *
     * @return {@code true} if this process is a child of another Helidon process.
     */
    public static boolean isHelidonChildProcess() {
        return IS_HELIDON_CHILD_PROCESS;
    }

    /**
     * Indicates if Ansi escapes are enabled. Calls {@link #install()}.
     *
     * @return {@code true} if enabled.
     */
    public static ConsoleType consoleType() {
        install();
        return CONSOLE_TYPE.get();
    }

    /**
     * Indicates if Ansi escapes are enabled. Calls {@link #install()}.
     *
     * @return {@code true} if enabled.
     */
    public static boolean areAnsiEscapesEnabled() {
        return install();
    }

    private static ConsoleType desiredConsoleType() {
        if (Boolean.getBoolean(JANSI_FORCE_PROPERTY)) {
            Log.preInitDebug("Jansi streams requested: %s=true", JANSI_FORCE_PROPERTY);
            return ConsoleType.ANSI;
        } else if (Boolean.getBoolean(JANSI_STRIP_PROPERTY)) {
            Log.preInitDebug("Jansi strip streams requested: %s=true", JANSI_STRIP_PROPERTY);
            return ConsoleType.STRIP_ANSI;
        } else if (Boolean.getBoolean(JANSI_PASS_THROUGH_PROPERTY)) {
            Log.preInitDebug("Jansi pass through streams requested: %s=true", JANSI_PASS_THROUGH_PROPERTY);
            return ConsoleType.STRIP_ANSI;
        } else if (System.console() != null) {
            Log.preInitDebug("No Jansi request, but Console is available");
            return ConsoleType.ANSI;
        } else {
            Log.preInitDebug("No Jansi request and Console is not available");
            return ConsoleType.DEFAULT;
        }
    }

    private static ConsoleType installedConsoleType(ConsoleType desiredType) {
        final PrintStream systemOut = System.out;
        final Class<? extends PrintStream> systemOutclass = systemOut.getClass();
        final String systemOutClassName = systemOutclass.getName();
        ConsoleType installedType;
        if (systemOutClassName.startsWith(JANSI_PACKAGE_PREFIX)) {
            if (systemOutClassName.equals(JANSI_STRIP_STREAM_CLASS_NAME)) {
                try {
                    // jansi 2.x always use AnsiPrintStream, but has a mode flag
                    String mode = systemOutclass.getMethod("getMode").invoke(systemOut).toString();
                    if (mode.equalsIgnoreCase("strip")) {
                        installedType = ConsoleType.STRIP_ANSI;
                    } else if (mode.equalsIgnoreCase("force")) {
                        installedType = ConsoleType.ANSI;
                    } else {
                        installedType = ConsoleType.DEFAULT;
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    // jansi 1.x uses AnsiPrintStream only for stripping
                    installedType = ConsoleType.STRIP_ANSI;
                }
            } else {
                installedType = ConsoleType.ANSI;
            }
        } else {
            installedType = ConsoleType.DEFAULT;
        }
        if (desiredType != installedType) {
            switch (installedType) {
                case STRIP_ANSI:
                    Log.preInitDebug("Desired = %s, but Ansi escapes will be stripped by system streams.", desiredType);
                    break;
                case ANSI:
                    Log.preInitDebug("Desired = %s, but Ansi escapes should be supported by system streams.", desiredType);
                    break;
                case DEFAULT:
                    Log.preInitDebug("Desired = %s, but System.out not a Jansi type (%s) ao Ansi escapes should not be stripped",
                            desiredType, systemOutClassName);
                    break;
                default:
                    // do nothing
            }
        }
        return installedType;
    }

    private AnsiConsoleInstaller() {
    }
}
