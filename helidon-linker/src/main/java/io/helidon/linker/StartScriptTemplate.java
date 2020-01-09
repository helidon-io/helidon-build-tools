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

package io.helidon.linker;

import java.util.List;

import io.helidon.linker.StartScript.TemplateConfig;

import static io.helidon.linker.util.Constants.CDS_UNLOCK_OPTIONS;
import static io.helidon.linker.util.Constants.OSType.Windows;
import static io.helidon.linker.util.Constants.OS_TYPE;
import static io.helidon.linker.util.FileUtils.fileName;
import static java.util.Collections.emptyList;

/**
 * Template for start script.
 */
public class StartScriptTemplate extends StartScript.SimpleTemplate {
    private static final String TEMPLATE_RESOURCE_PATH = OS_TYPE == Windows ? "start-template.ps1" : "start-template.sh";
    private static final String JAR_NAME_VAR = "<JAR_NAME>";
    private static final String DEFAULT_ARGS_VAR = "<DEFAULT_APP_ARGS>";
    private static final String DEFAULT_JVM_VAR = "<DEFAULT_APP_JVM>";
    private static final String DEFAULT_DEBUG_VAR = "<DEFAULT_APP_DEBUG>";
    private static final String HAS_CDS_VAR = "<HAS_CDS>";
    private static final String HAS_DEBUG_VAR = "<HAS_DEBUG>";
    private static final String CDS_UNLOCK_OPTION_VAR = "<CDS_UNLOCK>";
    private static final String DEFAULT_ARGS_DESC_VAR = "<DEFAULT_APP_ARGS_DESC>";
    private static final String DEFAULT_JVM_DESC_VAR = "<DEFAULT_APP_JVM_DESC>";
    private static final String DEFAULT_DEBUG_DESC_VAR = "<DEFAULT_APP_DEBUG_DESC>";
    private static final String EXIT_ON_STARTED_VAR = "<EXIT_ON_STARTED>";
    private static final String STAT_FORMAT_VAR = "<STAT_FORMAT>";
    private static final String MODULES_TIME_STAMP_VAR = "<MODULES_TIME_STAMP>";
    private static final String JAR_TIME_STAMP_VAR = "<JAR_TIME_STAMP>";
    private static final String MODULES_FILE = "lib/modules";
    private static final String OVERRIDES = "Overrides \\\"${default%s}\\\".";
    private static final String CHECK_TIME_STAMPS = OS_TYPE == Windows ? "function checkTimeStamps" : "checkTimeStamps()";
    private static final String SETS = "Sets default %s.";
    private static final String CDS = "cds";
    private static final String DEBUG = "debug";
    private static final String COPY_INSTRUCTIONS_VAR = "<COPY_INSTRUCTIONS>";
    private static final String NO_CDS = ", use the --noCds option or disable CDS in image generation.";
    private static final String COPY_NOT_SUPPORTED = "Copies are not supported in this Java version: avoid them" + NO_CDS;
    private static final String COPY_SUPPORTED = "Use a timestamp preserving copy option (e.g. 'cp -rp')" + NO_CDS;

    /**
     * Constructor.
     */
    public StartScriptTemplate() {
        super(TEMPLATE_RESOURCE_PATH);
        removeLines((index, line) -> isComment(line));
    }

    @Override
    public String render(TemplateConfig config) {

        if (!config.cdsInstalled()) {
            removeCheckTimeStampFunction();
            removeLines(CDS, true);
        }

        if (!config.debugInstalled()) {
            removeLines(DEBUG, true);
        }

        final String name = fileName(config.mainJar());

        final String jvm = String.join(" ", config.defaultJvmOptions());
        final String jvmDesc = description(config.defaultJvmOptions(), "JVM options", "Jvm");

        final String args = String.join(" ", config.defaultArgs());
        final String argsDesc = description(config.defaultArgs(), "arguments", "Args");

        final List<String> debugOptions = config.debugInstalled() ? config.defaultDebugOptions() : emptyList();
        final String debug = String.join(" ", debugOptions);
        final String debugDesc = description(debugOptions, "debug options", "Debug");

        final String hasCds = config.cdsInstalled() ? "yes" : "";
        final String hasDebug = config.debugInstalled() ? "yes" : "";
        final String cdsUnlock = config.cdsRequiresUnlock() ? CDS_UNLOCK_OPTIONS + " " : "";

        final String statFormat = OS_TYPE.statFormat();

        final String modulesModTime = lastModifiedTime(config.installHomeDirectory().resolve(MODULES_FILE));
        final String jarModTime = lastModifiedTime(config.mainJar());
        final String copyInstructions = config.cdsSupportsImageCopy() ? COPY_SUPPORTED : COPY_NOT_SUPPORTED;

        replace(JAR_NAME_VAR, name);
        replace(DEFAULT_JVM_VAR, jvm);
        replace(DEFAULT_JVM_DESC_VAR, jvmDesc);
        replace(DEFAULT_ARGS_VAR, args);
        replace(DEFAULT_ARGS_DESC_VAR, argsDesc);
        replace(DEFAULT_DEBUG_VAR, debug);
        replace(DEFAULT_DEBUG_DESC_VAR, debugDesc);
        replace(HAS_CDS_VAR, hasCds);
        replace(HAS_DEBUG_VAR, hasDebug);
        replace(CDS_UNLOCK_OPTION_VAR, cdsUnlock);
        replace(STAT_FORMAT_VAR, statFormat);
        replace(MODULES_TIME_STAMP_VAR, modulesModTime);
        replace(JAR_TIME_STAMP_VAR, jarModTime);
        replace(COPY_INSTRUCTIONS_VAR, copyInstructions);
        replace(EXIT_ON_STARTED_VAR, config.exitOnStartedValue());

        return toString();
    }

    private static String description(List<String> defaults, String description, String varName) {
        if (defaults.isEmpty()) {
            return String.format(SETS, description);
        } else {
            return String.format(OVERRIDES, varName);
        }
    }

    private void removeCheckTimeStampFunction() {
        final int startIndex = indexOf(0, CHECK_TIME_STAMPS, false);
        final int endIndex = indexOfEquals(startIndex, "}") + 1;
        removeLines((index, line) -> index >= startIndex && index <= endIndex);
    }

    private static boolean isComment(String line) {
        final int length = line.length();
        return length > 0 && line.charAt(0) == '#' && (length == 1 || line.charAt(1) != '!');
    }
}
