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

import java.util.Properties;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;

/**
 * The {@code dev} command.
 */
@Command(name = "dev", description = "Continuous application development")
public final class DevCommand extends BaseCommand implements CommandExecution {

    private static final String MAVEN_PLUGIN = "maven.plugin";

    private final CommonOptions commonOptions;
    private final boolean clean;
    private final boolean fork;

    @Creator
    DevCommand(
            CommonOptions commonOptions,
            @Flag(name = "clean", description = "Perform a clean before the first build") boolean clean,
            @Flag(name = "fork", description = "Fork mvn execution") boolean fork) {
        this.commonOptions = commonOptions;
        this.clean = clean;
        this.fork = fork;
    }

    @Override
    public void execute(CommandContext context) {
        Properties cliConfig = cliConfig();
        String cleanProp = "-Ddev.clean=" + clean;
        String forkProp = "-Ddev.fork=" + fork;
        StringBuilder pluginGoal = new StringBuilder(cliConfig.getProperty(MAVEN_PLUGIN));
        pluginGoal.append(":")
                .append(cliConfig().getProperty(HELIDON_VERSION))
                .append(":dev");

        // Execute Helidon maven plugin to enter dev loop
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(commonOptions.project())
                .command(MAVEN_EXEC, pluginGoal.toString(), cleanProp, forkProp);
        executeProcess(context, processBuilder);
    }
}
