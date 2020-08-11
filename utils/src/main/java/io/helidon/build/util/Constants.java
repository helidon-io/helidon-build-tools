/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

import java.io.File;

import static io.helidon.build.util.OSType.currentOS;

/**
 * Shared constants.
 */
public final class Constants {

    /**
     * The current operating system type.
     */
    public static final OSType OS = currentOS();

    /**
     * End of line string.                                                                                                                                                                        `
     */
    public static final String EOL = System.getProperty("line.separator");

    /**
     * File system directory separator.
     */
    public static final String DIR_SEP = File.separator;

    /**
     * Property that must be set to "true" for cli extensions to be enabled.
     */
    public static final String HELIDON_CLI_PROPERTY = "helidon.cli";

    /**
     * The command line argument to enable cli extensions.
     */
    public static final String ENABLE_HELIDON_CLI = "-D" + HELIDON_CLI_PROPERTY + "=true";

    /**
     * Gets location of Java's home directory by checking the (@code java.home} property
     * followed by the {@code JAVA_HOME} environment variable.
     *
     * @return Java's home directory.
     * @throws RuntimeException If unable to find home directory.
     */
    public static String javaHome() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            javaHome = FileUtils.assertJavaExecutable().getParent().getParent().toString();
        }
        return javaHome;
    }

    private Constants() {
    }
}
