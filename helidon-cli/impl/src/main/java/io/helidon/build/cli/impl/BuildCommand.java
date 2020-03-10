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

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandContext.ExitStatus;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;

/**
 * The {@code build} command.
 */
@Command(name = "build", description = "Build the application")
public final class BuildCommand extends BaseCommand implements CommandExecution {

    private final CommonOptions commonOptions;
    private final boolean clean;
    private final BuildMode buildMode;

    enum BuildMode {
        PLAIN,
        NATIVE,
        JLINK
    }

    @Creator
    BuildCommand(
            CommonOptions commonOptions,
            @Flag(name = "clean", description = "Perform a clean before the build") boolean clean,
            @KeyValue(name = "mode", description = "Build mode", defaultValue = "PLAIN") BuildMode buildMode) {
        this.commonOptions = commonOptions;
        this.clean = clean;
        this.buildMode = buildMode;
    }

    @Override
    public void execute(CommandContext context) {
        if (buildMode != BuildMode.PLAIN) {
            context.exitAction(ExitStatus.FAILURE, "Only " + BuildMode.PLAIN
                    + " build mode is supported at this time");
            return;
        }

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(commonOptions.project());
        if (clean) {
            processBuilder.command(MAVEN_EXEC, "clean", "install");
        } else {
            processBuilder.command(MAVEN_EXEC, "install");
        }
        executeProcess(context, processBuilder);
    }
}
