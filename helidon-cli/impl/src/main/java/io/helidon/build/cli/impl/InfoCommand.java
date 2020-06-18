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
package io.helidon.build.cli.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.cli.impl.VersionCommand.addProjectProperty;
import static io.helidon.build.cli.impl.VersionCommand.maxKeyWidth;
import static io.helidon.build.util.ProjectConfig.HELIDON_VERSION;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_VERSION;

/**
 * The {@code info} command.
 */
@Command(name = "info", description = "Print project information")
public final class InfoCommand extends BaseCommand implements CommandExecution {
    private static final int MIN_WIDTH = "plugin.build.revision".length();
    private final CommonOptions commonOptions;
    private final boolean verbose;

    @Creator
    InfoCommand(CommonOptions commonOptions) {
        this.commonOptions = commonOptions;
        this.verbose = Log.isVerbose();
    }

    @Override
    public void execute(CommandContext context) throws Exception {

        // Build properties

        Map<String, String> buildProps = new LinkedHashMap<>();
        VersionCommand.addBuildProperties(buildProps);

        // System properties

        Map<String, String> systemProps = new LinkedHashMap<>();
        if (verbose) {
            System.getProperties().keySet().stream().sorted().forEach(key -> {
                String name = key.toString();
                String value = System.getProperty(name);
                value = value.replace("\n", "\\n");
                value = value.replace("\r", "\\r");
                value = value.replace("\b", "\\b");
                systemProps.put(key.toString(), value);
            });
        }

        // Env vars

        Map<String, String> envVars = new LinkedHashMap<>();
        if (verbose) {
            System.getenv().keySet().stream().sorted().forEach(key -> {
                String value = System.getenv(key);
                envVars.put(key, value);
            });
        }

        // Project config

        Map<String, String> projectProps = new LinkedHashMap<>();
        ProjectConfig projectConfig = projectConfig(commonOptions);
        if (projectConfig.exists()) {
            addProjectProperty("version", PROJECT_VERSION, projectConfig, projectProps);
            addProjectProperty("helidon.version", HELIDON_VERSION, projectConfig, projectProps);
            addProjectProperty("flavor", PROJECT_FLAVOR, projectConfig, projectProps);
            addProjectProperty("directory", PROJECT_DIRECTORY, projectConfig, projectProps);
            addProjectProperty("version", PROJECT_VERSION, projectConfig, projectProps);
            addProjectProperty("main.class", PROJECT_MAINCLASS, projectConfig, projectProps);
            addProjectProperty("source.dirs", PROJECT_SOURCEDIRS, projectConfig, projectProps);
            addProjectProperty("classes.dirs", PROJECT_CLASSDIRS, projectConfig, projectProps);
            addProjectProperty("resource.dirs", PROJECT_RESOURCEDIRS, projectConfig, projectProps);
        }

        // Log them all

        int maxWidth = Math.max(maxKeyWidth(buildProps, systemProps, envVars, projectProps), MIN_WIDTH);
        log("Project Config", projectProps, maxWidth);
        log("Build", buildProps, maxWidth);
        log("System Properties", systemProps, maxWidth);
        log("Environment Variables", envVars, maxWidth);
        logHeader("Plugin Build");
        Plugins.execute("GetInfo", pluginArgs(maxWidth), 5, Log::info);
    }

    private List<String> pluginArgs(int maxWidth) {
        return List.of("--maxWidth", Integer.toString(maxWidth));
    }

    private static void logHeader(String header) {
        Log.info();
        Log.info("$(bold | %s)", header);
        Log.info();
    }

    private static void log(String header, Map<String, String> map, int maxKeyWidth) {
        if (!map.isEmpty()) {
            logHeader(header);
            VersionCommand.log(map, maxKeyWidth);
        }
    }
}
