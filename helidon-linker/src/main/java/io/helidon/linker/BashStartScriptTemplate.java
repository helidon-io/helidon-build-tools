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

package io.helidon.linker;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import io.helidon.linker.StartScript.TemplateConfig;
import io.helidon.linker.util.StreamUtils;

import static io.helidon.linker.util.Constants.CDS_UNLOCK_OPTIONS;
import static io.helidon.linker.util.Constants.EOL;
import static io.helidon.linker.util.Constants.OSType.MacOS;
import static io.helidon.linker.util.Constants.OS_TYPE;
import static java.util.Collections.emptyList;

/**
 * Template for bash script.
 */
class BashStartScriptTemplate extends StartScript.SimpleTemplate {
    private static final String TEMPLATE_RESOURCE = "start-template.sh";
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
    private static final String STAT_FORMAT_VAR = "<STAT_FORMAT>";
    private static final String STAT_FORMAT_MAC = "-f %m";
    private static final String STAT_FORMAT_LINUX = "-c %Y";
    private static final String MODULES_TIME_STAMP_VAR = "<MODULES_TIME_STAMP>";
    private static final String JAR_TIME_STAMP_VAR = "<JAR_TIME_STAMP>";
    private static final String MODULES_FILE = "lib/modules";
    private static final String OVERRIDES = "Overrides \\\"${default%s}\\\".";
    private static final String CHECK_TIME_STAMPS = "checkTimeStamps()";
    private static final String CDS_WARNING = "WARNING: CDS";
    private static final String SETS = "Sets default %s.";
    private static final String CDS = "cds";
    private static final String DEBUG = "debug";
    private static final String COPY_INSTRUCTIONS_VAR = "<COPY_INSTRUCTIONS>";
    private static final String NO_CDS = ", use the --noCds option or disable CDS in image generation.";
    private static final String COPY_NOT_SUPPORTED = "Copies are not supported in this Java version: avoid them" + NO_CDS;
    private static final String COPY_SUPPORTED = "Use a timestamp preserving copy option (e.g. 'cp -rp')" + NO_CDS;
    private List<String> template;
    private TemplateConfig config;

    @Override
    public String resolve(TemplateConfig config) {
        this.template = load();
        this.config = config;
        return resolve();
    }

    private String resolve() {
        final String name = config.mainJar().getFileName().toString();

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

        final String statFormat = OS_TYPE == MacOS ? STAT_FORMAT_MAC : STAT_FORMAT_LINUX;
        final String modulesModTime = lastModifiedTime(config.installHomeDirectory().resolve(MODULES_FILE));
        final String jarModTime = lastModifiedTime(config.mainJar());
        final String copyInstructions = config.cdsSupportsImageCopy() ? COPY_SUPPORTED : COPY_NOT_SUPPORTED;

        if (!config.cdsInstalled()) {
            removeCheckTimeStampFunction();
            removeTemplateLines(CDS);
        }

        if (!config.debugInstalled()) {
            removeTemplateLines(DEBUG);
        }

        return String.join(EOL, template)
                     .replace(JAR_NAME_VAR, name)
                     .replace(DEFAULT_JVM_VAR, jvm)
                     .replace(DEFAULT_JVM_DESC_VAR, jvmDesc)
                     .replace(DEFAULT_ARGS_VAR, args)
                     .replace(DEFAULT_ARGS_DESC_VAR, argsDesc)
                     .replace(DEFAULT_DEBUG_VAR, debug)
                     .replace(DEFAULT_DEBUG_DESC_VAR, debugDesc)
                     .replace(HAS_CDS_VAR, hasCds)
                     .replace(HAS_DEBUG_VAR, hasDebug)
                     .replace(CDS_UNLOCK_OPTION_VAR, cdsUnlock)
                     .replace(STAT_FORMAT_VAR, statFormat)
                     .replace(MODULES_TIME_STAMP_VAR, modulesModTime)
                     .replace(JAR_TIME_STAMP_VAR, jarModTime)
                     .replace(COPY_INSTRUCTIONS_VAR, copyInstructions);
    }

    private static String description(List<String> defaults, String description, String varName) {
        if (defaults.isEmpty()) {
            return String.format(SETS, description);
        } else {
            return String.format(OVERRIDES, varName);
        }
    }

    private void removeCheckTimeStampFunction() {
        final int startIndex = indexOf(template, 0, CHECK_TIME_STAMPS);
        final int warningIndex = indexOf(template, startIndex + 1, CDS_WARNING);
        final int closingBraceIndex = indexOf(template, warningIndex + 1, "}") + 1; // include empty line
        removeLines(template, (index, line) -> index >= startIndex && index <= closingBraceIndex);
    }

    private void removeTemplateLines(String substring) {
        removeLines(template, (index, line) -> containsIgnoreCase(line, substring));
    }

    private static boolean isComment(String line) {
        final int length = line.length();
        return length > 0 && line.charAt(0) == '#' && (length == 1 || line.charAt(1) != '!');
    }

    private List<String> load() {
        final InputStream content = StartScript.class.getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE);
        if (content == null) {
            throw new IllegalStateException(TEMPLATE_RESOURCE + " not found");
        } else {
            try {
                return removeLines(StreamUtils.toLines(content), (index, line) -> isComment(line));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
