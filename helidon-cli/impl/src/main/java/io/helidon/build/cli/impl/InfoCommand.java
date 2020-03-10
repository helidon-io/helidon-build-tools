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

import java.util.Arrays;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;

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
    public void execute(CommandContext context) {
        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());
        if (!projectConfig.exists()) {
            context.exitAction(CommandContext.ExitStatus.FAILURE, "Unable to find project");
            return;
        }
        context.logInfo("");
        context.logInfo("project:");
        context.logInfo("  flavor: " + projectConfig.property(PROJECT_FLAVOR).toUpperCase());
        context.logInfo("  directory: " + projectConfig.property(PROJECT_DIRECTORY));
        if (projectConfig.contains(PROJECT_MAINCLASS)) {
            context.logInfo("  mainClass: " + projectConfig.property(PROJECT_MAINCLASS));
            context.logInfo("  sourceDirs: " + formatList(projectConfig.property(PROJECT_SOURCEDIRS)));
            context.logInfo("  classesDirs: " + formatList(projectConfig.property(PROJECT_CLASSDIRS)));
            context.logInfo("  resourceDirs: " + formatList(projectConfig.property(PROJECT_RESOURCEDIRS)));
        }
        context.logInfo("");
    }

    private static String formatList(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Arrays.stream(value.split(",")).forEach(s -> builder.append("\n    - " + s));
        return builder.toString();
    }
}
