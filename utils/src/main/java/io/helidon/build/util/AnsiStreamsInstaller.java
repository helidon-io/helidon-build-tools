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

import org.fusesource.jansi.Ansi;
import picocli.jansi.graalvm.AnsiConsole;

/**
 * Installer for {@link System#out} and {@link System#err} streams that support {@link Ansi} escapes, if possible.
 * Supports {@code GraalVM} native executables.
 */
public class AnsiStreamsInstaller {
    // Note: Class instances are not used here since this class is used within a maven plugin
    //       that might have a different version of Jansi
    private static final String JANSI_PACKAGE_PREFIX = "org.fusesource.jansi";
    private static final String JANSI_STRIP_STREAM_CLASS_NAME = "org.fusesource.jansi.AnsiPrintStream";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean ENABLED = new AtomicBoolean();

    /**
     * The system property that, if {@code true}, will bypass the default check
     * to see if stdin is a terminal.
     */
    public static final String FORCE_ANSI_PROPERTY = "jansi.force";

    /**
     * Returns a command-line argument to set the {@link #FORCE_ANSI_PROPERTY} to {@code true}
     * if Ansi escapes are enabled, or {@code false} if not.
     *
     * @return The command line argument, e.g. "-Djansi.force=true";
     */
    public static String forceAnsiArgument() {
        return "-D" + FORCE_ANSI_PROPERTY + "=" + isEnabled();
    }

    /**
     * Returns whether or not Ansi escapes are enabled. Calls {@link #ensureInstalled()}.
     *
     * @return {@code true} if enabled.
     */
    public static boolean isEnabled() {
        return ensureInstalled();
    }

    /**
     * Ensures that the system streams have been installed.
     *
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean ensureInstalled() {
        if (!INSTALLED.getAndSet(true)) {
            if (shouldInstall()) {
                AnsiConsole.systemInstall();
            }
            ENABLED.set(checkEnabled());
        }
        return ENABLED.get();
    }

    private static boolean checkEnabled() {
        if (Ansi.isEnabled()) {
            final String systemOutClass = System.out.getClass().getName();
            if (systemOutClass.startsWith(JANSI_PACKAGE_PREFIX)) {
                // We have a Jansi type installed, check if it is the type that strips escapes
                if (systemOutClass.equals(JANSI_STRIP_STREAM_CLASS_NAME)) {
                    Log.debug("Ansi escapes will be stripped by system streams");
                    return false;
                } else {
                    Log.debug("Ansi escapes should not be stripped by system streams");
                    return true;
                }
            } else {
                Log.debug("System.out not a Jansi type (%s); Ansi escapes should not be stripped", systemOutClass);
                return true;
            }
        } else {
            Log.debug("Ansi has been disabled");
            return false;
        }
    }

    private static boolean shouldInstall() {
        if ("true".equals(System.getProperty(FORCE_ANSI_PROPERTY))) {
            Log.debug("Installing Jansi streams: %s=true", FORCE_ANSI_PROPERTY);
            return true;
        } else if (System.console() != null) {
            Log.debug("Installing Jansi streams: console available");
            return true;
        } else {
            Log.debug("Skipping Jansi streams install: not forced and console not available");
            return false;
        }
    }

    private AnsiStreamsInstaller() {
    }
}
