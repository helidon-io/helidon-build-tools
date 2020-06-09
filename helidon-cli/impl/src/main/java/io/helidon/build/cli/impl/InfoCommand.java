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
import java.util.Map;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.cli.impl.VersionCommand.addProjectProperty;
import static io.helidon.build.cli.impl.VersionCommand.log;
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
    private final CommonOptions commonOptions;

    @Creator
    InfoCommand(CommonOptions commonOptions) {
        this.commonOptions = commonOptions;
    }

    @Override
    public void execute(CommandContext context) throws Exception {

        // System info

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("JRE", Runtime.version().toString());
        system.put("OS", System.getProperty("os.name", "<unknown>"));
        log("system", system);

        // Build properties

        VersionCommand.logBuildProperties();

        // Project config

        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());
        if (projectConfig.exists()) {
            Map<String, Object> projectProps = new LinkedHashMap<>();
            addProjectProperty("version", PROJECT_VERSION, projectConfig, projectProps);
            addProjectProperty("helidon", HELIDON_VERSION, projectConfig, projectProps);
            addProjectProperty("flavor", PROJECT_FLAVOR, projectConfig, projectProps);
            addProjectProperty("directory", PROJECT_DIRECTORY, projectConfig, projectProps);
            addProjectProperty("version", PROJECT_VERSION, projectConfig, projectProps);
            addProjectProperty("mainClass", PROJECT_MAINCLASS, projectConfig, projectProps);
            addProjectProperty("sourceDirs", PROJECT_SOURCEDIRS, projectConfig, projectProps);
            addProjectProperty("classesDirs", PROJECT_CLASSDIRS, projectConfig, projectProps);
            addProjectProperty("resourceDIrs", PROJECT_RESOURCEDIRS, projectConfig, projectProps);
            log("project", projectProps);
        }

        // Plugin info
        Plugins.execute("GetInfo", 5);
    }
}
