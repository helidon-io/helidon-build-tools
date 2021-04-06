/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.helidon.build.common.OSType;

import static io.helidon.build.common.ansi.StyleFunction.Bold;
import static io.helidon.build.common.ansi.StyleFunction.BoldBrightYellow;

/**
 * Shared constants.
 */
public final class Constants {

    /**
     * The current operating system type.
     */
    public static final OSType OS = OSType.currentOS();

    /**
     * The minimum supported JDK version.
     */
    public static final int MINIMUM_JDK_VERSION = 9;

    /**
     * Whether or not this is a Docker build.
     */
    public static final boolean DOCKER_BUILD = "true".equals(System.getProperty("docker.build"));

    /**
     * The minimum supported JDK version when in a Docker env.
     */
    public static final int MINIMUM_DOCKER_JDK_VERSION = 10;

    /**
     * Whether or not JDEPS requires the missing deps option.
     */
    public static final boolean JDEPS_REQUIRES_MISSING_DEPS_OPTION = Runtime.version().feature() > 11;

    /**
     * Whether or not CDS requires the unlock option.
     */
    public static final boolean CDS_REQUIRES_UNLOCK_OPTION = Runtime.version().feature() <= 10;

    /**
     * The CDS unlock diagnostic options.
     */
    public static final String CDS_UNLOCK_OPTIONS = "-XX:+UnlockDiagnosticVMOptions";

    /**
     * Whether or not CDS supports image copy (with preserved timestamps).
     */
    public static final boolean CDS_SUPPORTS_IMAGE_COPY = Runtime.version().feature() >= 10;

    /**
     * End of line string.
     */
    public static final String EOL = System.getProperty("line.separator");

    /**
     * File system directory separator.
     */
    public static final String DIR_SEP = File.separator;

    /**
     * The suffix to append to JRI directories when name must be created.
     */
    public static final String JRI_DIR_SUFFIX = "-jri";

    /**
     * Indent function.
     */
    public static final Function<String, String> INDENT = line -> "    " + line;

    /**
     * Excluded module names.
     */
    public static final Set<String> EXCLUDED_MODULES = Set.of("java.xml.ws.annotation");

    /**
     * The debugger module name.
     */
    public static final String DEBUGGER_MODULE = "jdk.jdwp.agent";

    /**
     * Identifying substring for a Windows error message when running a script.
     */
    public static final String WINDOWS_SCRIPT_EXECUTION_ERROR = "FullyQualifiedErrorId";

    /**
     * Identifying substrings for the Windows error message that requires the user to set an execution policy.
     */
    public static final List<String> WINDOWS_SCRIPT_EXECUTION_POLICY_ERROR = List.of("UnauthorizedAccess",
                                                                                     "Execution",
                                                                                     "Policies");

    /**
     * The help message to log if the script execution policy error occurs.
     */
    public static final String WINDOWS_SCRIPT_EXECUTION_POLICY_ERROR_HELP =
            EOL
            + EOL
            + Bold.apply("To enable script execution, run the following command: ")
            + EOL
            + EOL
            + "    "
            + BoldBrightYellow.apply("powershell Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned")
            + EOL
            + EOL
            + Bold.apply("and answer 'Y' if prompted.");

    private Constants() {
    }
}
