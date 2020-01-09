/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker.util;

import java.io.File;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import static io.helidon.linker.util.Style.Bold;

/**
 * Shared constants.
 */
public final class Constants {

    /**
     * Operating system types.
     */
    public enum OSType {
        /**
         * Macintosh.
         */
        MacOS("java", true, null, "-f %m", "mvn"),
        /**
         * Windows.
         */
        Windows("java.exe", false, "powershell.exe", "%s", "mvn.cmd") {
            @Override
            public String withScriptExtension(String scriptName){
                return scriptName + ".ps1";
            }
        },
        /**
         * Linux.
         */
        Linux("java", true, null, "-c %Y", "mvn"),
        /**
         * Unknown.
         */
        Unknown("java", true, null, null, "mvn");

        private final String javaExecutable;
        private final boolean posix;
        private final String scriptExecutor;
        private final String statFormat;
        private final String mavenExec;

        OSType(String javaExecutable, boolean posix, String scriptExecutor, String statFormat, String mavenExec) {
            this.javaExecutable = javaExecutable;
            this.posix = posix;
            this.scriptExecutor = scriptExecutor;
            this.statFormat = statFormat;
            this.mavenExec = mavenExec;
        }

        /**
         * Default java executable.
         * @return java executable name
         */
        public String javaExecutable() {
            return javaExecutable;
        }

        /**
         * Returns the scriptName with the SO related extension.
         * @param scriptName The script file name without file extension
         * @return The scriptName with SO extension
         */
        public String withScriptExtension(String scriptName) {
            return scriptName;
        }

        /**
         * To check that OSType supports posix.
         * @return true when OSType supports posix or false if not.
         */
        public boolean isPosix() {
            return posix;
        }

        /**
         * In some OSType is necessary to specify the program to execute the script.
         * @return the program name to execute the script or null when it is not necessary.
         */
        public String scriptExecutor() {
            return scriptExecutor;
        }

        /**
         * Returns the stat format that depends on SO.
         * @return the stat format
         */
        public String statFormat() {
            return statFormat;
        }

        /**
         * Returns the maven execution file name.
         * @return the maven exec
         */
        public String mavenExec() {
            return mavenExec;
        }
    }

    /**
     * The operating system type.
     *
     */
    public static final OSType OS_TYPE = osType();

    /**
     * The minimum supported JDK version.
     */
    public static final int MINIMUM_JDK_VERSION = 9;

    /**
     * End of line string.
     */
    public static final String EOL = System.getProperty("line.separator");

    /**
     * File system directory separator.
     */
    public static final String DIR_SEP = File.separator;

    /**
     * Whether or not JDEPS requires the missing deps option.
     */
    public static final boolean JDEPS_REQUIRES_MISSING_DEPS_OPTION = Runtime.version().major() > 11;

    /**
     * Whether or not CDS requires the unlock option.
     */
    public static final boolean CDS_REQUIRES_UNLOCK_OPTION = Runtime.version().major() <= 10;

    /**
     * The CDS unlock diagnostic options.
     */
    public static final String CDS_UNLOCK_OPTIONS = "-XX:+UnlockDiagnosticVMOptions";

    /**
     * Whether or not CDS supports image copy (with preserved timestamps).
     */
    public static final boolean CDS_SUPPORTS_IMAGE_COPY = Runtime.version().major() >= 10;

    /**
     * Indent function.
     */
    public static final Function<String, String> INDENT = line -> "    " + line;

    /**
     * Indent bold function.
     */
    public static final Function<String, String> INDENT_BOLD = line -> "    " + Bold.apply(line);

    /**
     * Excluded module names.
     */
    public static final Set<String> EXCLUDED_MODULES = Set.of("java.xml.ws.annotation");

    /**
     * The debugger module name.
     */
    public static final String DEBUGGER_MODULE = "jdk.jdwp.agent";


    private static OSType osType() {
        final String name = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
        if (name.contains("win")) {
            return OSType.Windows;
        } else if (name.contains("mac") || name.contains("darwin")) {
            return OSType.MacOS;
        } else if (name.contains("nux")) {
            return OSType.Linux;
        } else {
            return OSType.Unknown;
        }
    }
    private Constants() {
    }
}
