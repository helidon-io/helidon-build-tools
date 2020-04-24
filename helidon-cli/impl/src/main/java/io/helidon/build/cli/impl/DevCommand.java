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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandContext.Verbosity;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.util.AnsiStreamsInstaller;

/**
 * The {@code dev} command.
 */
@Command(name = "dev", description = "Continuous application development")
public final class DevCommand extends BaseCommand implements CommandExecution {

    private static final String CLEAN_PROP_PREFIX = "-Ddev.clean=";
    private static final String FORK_PROP_PREFIX = "-Ddev.fork=";
    private static final String DEV_GOAL = "helidon:dev";
    private static final String MAVEN_OPTS_VAR = "MAVEN_OPTS";

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
        // Execute Helidon maven plugin to enter dev loop

        List<String> command = new ArrayList<>();
        command.add(MAVEN_EXEC);
        command.add(DEV_GOAL);
        command.add(CLEAN_PROP_PREFIX + clean);
        command.add(FORK_PROP_PREFIX + fork);
        if (!context.verbosity().equals(Verbosity.NORMAL)) {
            command.add("--debug");
        }

        ProcessBuilder processBuilder = new ProcessBuilder().directory(commonOptions.project())
                                                            .command(command);

        // Set the jansi.force property using MAVEN_OPTS, since this value is interpreted too early
        // to pass it to the mvn command

        String forceAnsi = AnsiStreamsInstaller.forceAnsiArgument();
        Map<String, String> env = processBuilder.environment();
        String opts = env.get(MAVEN_OPTS_VAR);
        if (opts == null) {
            opts = forceAnsi;
        } else {
            opts += (" " + forceAnsi);
        }
        env.put(MAVEN_OPTS_VAR, opts);

        executeProcess(context, processBuilder);
    }
}
