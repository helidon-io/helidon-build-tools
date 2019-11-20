/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Shared constants.
 */
public class Constants {

    /**
     * The minimum supported JDK version.
     */
    public static final int MINIMUM_JDK_VERSION = 9;
    
    /**
     * Whether or not this is a Windows platform.
     */
    public static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    /**
     * End of line string.
     */
    public static final String EOL = System.getProperty("line.separator");

    /**
     * File system directory separator.
     */
    public static final String DIR_SEP = File.separator;

    /**
     * Whether or not CDS requires the unlock option.
     */
    public static final boolean CDS_REQUIRES_UNLOCK_OPTION = Runtime.version().major() <= 9;

    /**
     * The CDS unlock diagnostic options.
     */
    public static final String CDS_UNLOCK_OPTIONS = "-XX:+UnlockDiagnosticVMOptions";

    /**
     * Indent.
     */
    public static final String INDENT = "    ";

    /**
     * Excluded module names.
     */
    public static final Set<String> EXCLUDED_MODULES = Set.of("java.xml.ws.annotation");
}
