/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;

/**
 * Operating system types.
 */
public enum OSType {
    /**
     * Macintosh.
     */
    MacOS("java", true, null, "-f %m", "mvn", "\\\""),

    /**
     * Windows.
     */
    Windows("java.exe", false, "powershell.exe", "%s", "mvn.cmd", "`\"") {
        @Override
        public String withScriptExtension(String scriptName) {
            return scriptName + ".ps1";
        }
    },

    /**
     * Linux.
     */
    Linux("java", true, null, "-c %Y", "mvn", "\\\""),

    /**
     * Unknown.
     */
    Unknown("java", true, null, null, "mvn", "\\\"");

    private final String javaExecutable;
    private final boolean posix;
    private final String scriptExecutor;
    private final String statFormat;
    private final String mavenExec;
    private final String escapedQuote;

    OSType(String javaExecutable,
           boolean posix,
           String scriptExecutor,
           String statFormat,
           String mavenExec,
           String escapedQuote) {
        this.javaExecutable = javaExecutable;
        this.posix = posix;
        this.scriptExecutor = scriptExecutor;
        this.statFormat = statFormat;
        this.mavenExec = mavenExec;
        this.escapedQuote = escapedQuote;
    }

    /**
     * Returns the current operating system type.
     *
     * @return The type.
     */
    public static OSType currentOS() {
        final String name = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
        if (name.contains("win")) {
            return OSType.Windows;
        } else if (name.contains("mac")
                   || name.contains("darwin")) {
            return OSType.MacOS;
        } else if (name.contains("linux")
                   || name.contains("unix")
                   || name.contains("solaris")
                   || name.contains("aix")
                   || name.contains("freebsd")
                   || name.contains("sunos")) {
            return OSType.Linux;
        } else {
            return OSType.Unknown;
        }
    }

    /**
     * Default java executable.
     *
     * @return java executable name
     */
    public String javaExecutable() {
        return javaExecutable;
    }

    /**
     * Returns the scriptName with the SO related extension.
     *
     * @param scriptName The script file name without file extension
     * @return The scriptName with SO extension
     */
    public String withScriptExtension(String scriptName) {
        return scriptName;
    }

    /**
     * To check that OS supports posix.
     *
     * @return true when OS supports posix or false if not.
     */
    public boolean isPosix() {
        return posix;
    }

    /**
     * In some OSType is necessary to specify the program to execute the script.
     *
     * @return the program name to execute the script or null when it is not necessary.
     */
    public String scriptExecutor() {
        return scriptExecutor;
    }

    /**
     * Returns the stat format that depends on OS.
     *
     * @return the stat format
     */
    public String statFormat() {
        return statFormat;
    }

    /**
     * Returns the maven execution file name.
     *
     * @return the maven exec
     */
    public String mavenExec() {
        return mavenExec;
    }

    /**
     * Returns an escaped quote for this OS.
     *
     * @return The escaped quote.
     */
    public String escapedQuote() {
        return escapedQuote;
    }
}
