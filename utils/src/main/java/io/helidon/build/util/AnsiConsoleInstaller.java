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

package io.helidon.build.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.fusesource.jansi.Ansi;
import picocli.jansi.graalvm.AnsiConsole;

import static org.fusesource.jansi.Ansi.ansi;

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
     * Returns whether or not this process is a child of another Helidon process.
     *
     * @return {@code true} if this process is a child of another Helidon process.
     */
    public static boolean isHelidonChildProcess() {
        return IS_HELIDON_CHILD_PROCESS;
    }

    /**
     * Returns whether or not Ansi escapes are enabled. Calls {@link #ensureInstalled()}.
     *
     * @return {@code true} if enabled.
     */
    public static ConsoleType consoleType() {
        ensureInstalled();
        return CONSOLE_TYPE.get();
    }

    /**
     * Returns whether or not Ansi escapes are enabled. Calls {@link #ensureInstalled()}.
     *
     * @return {@code true} if enabled.
     */
    public static boolean areAnsiEscapesEnabled() {
        return ensureInstalled();
    }

    /**
     * Clears the screen if Ansi escapes are enabled.
     *
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean clearScreen() {
        return clearScreen(0);
    }

    /**
     * Clears the screen from the given row if Ansi escapes are enabled.
     *
     * @param startRow The row at which to start clearing.
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean clearScreen(int startRow) {
        if (areAnsiEscapesEnabled()) {
            System.out.print(ansi().cursor(startRow, 0).eraseScreen());
            System.out.flush();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Ensures that the system streams have been installed.
     *
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean ensureInstalled() {
        if (!INSTALLED.getAndSet(true)) {
            ConsoleType desiredType = desiredConsoleType();
            AnsiConsole.systemInstall();
            ConsoleType installedType = installedConsoleType(desiredType);
            CONSOLE_TYPE.set(installedType);
            ENABLED.set(installedType == ConsoleType.ANSI);
        }
        return ENABLED.get();
    }

    private static ConsoleType desiredConsoleType() {
        if (Boolean.getBoolean(JANSI_FORCE_PROPERTY)) {
            Log.debug("Jansi streams requested: %s=true", JANSI_FORCE_PROPERTY);
            return ConsoleType.ANSI;
        } else if (Boolean.getBoolean(JANSI_STRIP_PROPERTY)) {
            Log.debug("Jansi strip streams requested: %s=true", JANSI_STRIP_PROPERTY);
            return ConsoleType.STRIP_ANSI;
        } else if (Boolean.getBoolean(JANSI_PASS_THROUGH_PROPERTY)) {
            Log.debug("Jansi pass through streams requested: %s=true", JANSI_PASS_THROUGH_PROPERTY);
            return ConsoleType.STRIP_ANSI;
        } else if (System.console() != null) {
            Log.debug("No Jansi request, but Console is available");
            return ConsoleType.ANSI;
        } else {
            Log.debug("No Jansi request and Console is not available");
            return ConsoleType.DEFAULT;
        }
    }

    private static ConsoleType installedConsoleType(ConsoleType desiredType) {
        final String systemOutClass = System.out.getClass().getName();
        if (systemOutClass.startsWith(JANSI_PACKAGE_PREFIX)) {
            // We have a Jansi type installed, check if it is the type that strips escapes
            if (systemOutClass.equals(JANSI_STRIP_STREAM_CLASS_NAME)) {
                if (desiredType != ConsoleType.STRIP_ANSI) {
                    Log.debug("Desired = %s, but Ansi escapes will be stripped by system streams.", desiredType);
                }
                return ConsoleType.STRIP_ANSI;
            } else {
                if (desiredType != ConsoleType.ANSI) {
                    Log.debug("Desired = %s, but Ansi escapes should be supported by system streams.", desiredType);
                }
                return ConsoleType.ANSI;
            }
        } else {
            if (desiredType != ConsoleType.DEFAULT) {
                Log.debug("Desired = %s, but System.out not a Jansi type (%s) ao Ansi escapes should not be stripped",
                        desiredType, systemOutClass);
            }
            return ConsoleType.DEFAULT;
        }
    }


    private AnsiConsoleInstaller() {
    }
}
