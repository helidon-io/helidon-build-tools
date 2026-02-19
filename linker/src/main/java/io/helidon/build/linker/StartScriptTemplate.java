/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

package io.helidon.build.linker;

import java.util.List;

import io.helidon.build.linker.StartScript.TemplateConfig;

import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.OSType.CURRENT_OS;
import static io.helidon.build.common.OSType.Windows;
import static java.util.Collections.emptyList;

/**
 * Template for start script.
 */
public class StartScriptTemplate extends StartScript.SimpleTemplate {

    /**
     * Constructor.
     */
    public StartScriptTemplate() {
        super(CURRENT_OS == Windows ? "start-template.ps1" : "start-template.sh");
        removeLines((index, line) -> isComment(line));
    }

    @Override
    public String render(TemplateConfig config) {
        if (!config.cdsInstalled()) {
            if (CURRENT_OS == Windows) {
                removeFunction("function checkTimeStamps");
                removeFunction("function setupCds");
            } else {
                removeFunction("checkTimeStamps()");
                removeFunction("setupCds()");
            }
            removeLines("cds");
        }

        if (!config.aotInstalled()) {
            if (CURRENT_OS == Windows) {
                removeFunction("function setupAot");
            } else {
                removeFunction("setupAot()");
            }
            removeLines("aot");
        }

        if (!config.debugInstalled()) {
            removeLines("debug");
        }

        String name = fileName(config.mainJar());

        String jvm = String.join(" ", config.defaultJvmOptions());
        String jvmDesc = description(config.defaultJvmOptions(), "JVM options", "Jvm");

        String args = String.join(" ", config.defaultArgs());
        String argsDesc = description(config.defaultArgs(), "arguments", "Args");

        List<String> debugOptions = config.debugInstalled() ? config.defaultDebugOptions() : emptyList();
        String debug = String.join(" ", debugOptions);
        String debugDesc = description(debugOptions, "debug options", "Debug");

        String hasCds = config.cdsInstalled() ? "yes" : "";
        String hasDebug = config.debugInstalled() ? "yes" : "";
        String cdsUnlock = config.cdsRequiresUnlock() ? "-XX:+UnlockDiagnosticVMOptions " : "";

        String statFormat = CURRENT_OS.statFormat();

        String modulesModTime = lastModifiedTime(config.installHomeDirectory().resolve("lib/modules"));
        String jarModTime = lastModifiedTime(config.mainJar());

        String copyInstructions;
        if (config.cdsSupportsImageCopy()) {
            copyInstructions = "Use a timestamp preserving copy option (e.g. 'cp -rp')"
                               + ", use the --noCds option or disable CDS in image generation.";
        } else {
            copyInstructions = "Copies are not supported in this Java version: avoid them"
                               + ", use the --noCds option or disable CDS in image generation.";
        }

        replace("<JAR_NAME>", name);
        replace("<DEFAULT_APP_JVM>", jvm);
        replace("<DEFAULT_APP_JVM_DESC>", jvmDesc);
        replace("<DEFAULT_APP_ARGS>", args);
        replace("<DEFAULT_APP_ARGS_DESC>", argsDesc);
        replace("<DEFAULT_APP_DEBUG>", debug);
        replace("<DEFAULT_APP_DEBUG_DESC>", debugDesc);
        replace("<HAS_CDS>", hasCds);
        replace("<HAS_DEBUG>", hasDebug);
        replace("<CDS_UNLOCK>", cdsUnlock);
        replace("<STAT_FORMAT>", statFormat);
        replace("<MODULES_TIME_STAMP>", modulesModTime);
        replace("<JAR_TIME_STAMP>", jarModTime);
        replace("<COPY_INSTRUCTIONS>", copyInstructions);
        replace("<EXIT_ON_STARTED>", config.exitOnStartedValue());

        return toString();
    }

    private static String description(List<String> defaults, String description, String varName) {
        if (defaults.isEmpty()) {
            return String.format("Sets default %s.", description);
        } else {
            String escapedQuote = CURRENT_OS.escapedQuote();
            return String.format("Overrides %s${default%s}%s.", escapedQuote, varName, escapedQuote);
        }
    }

    private void removeFunction(String functionName) {
        int startIndex = indexOf(functionName);
        int endIndex = indexOf(startIndex, (index, line) -> line.equals("}")) + 1;
        removeLines((index, line) -> index >= startIndex && index <= endIndex);
    }

    private static boolean isComment(String line) {
        int length = line.length();
        return length > 0 && line.charAt(0) == '#' && (length == 1 || line.charAt(1) != '!');
    }
}
