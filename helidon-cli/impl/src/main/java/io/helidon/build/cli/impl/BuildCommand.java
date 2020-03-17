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

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;

/**
 * The {@code build} command.
 */
@Command(name = "build", description = "Build the application")
public final class BuildCommand extends BaseCommand implements CommandExecution {

    private static final String JLINK_OPTION = "-Pjlink-image";
    private static final String NATIVE_OPTION = "-Pnative-image";

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
        ProcessBuilder processBuilder = new ProcessBuilder().directory(commonOptions.project());
        ArrayList<String> args = new ArrayList<>();
        args.add(MAVEN_EXEC);
        switch (buildMode) {
            case PLAIN:
                // no-op
                break;
            case JLINK:
                args.add(JLINK_OPTION);
                break;
            case NATIVE:
                args.add(NATIVE_OPTION);
                break;
            default:
                throw new IllegalStateException("Unknown build mode " + buildMode);
        }
        if (clean) {
            args.add("clean");
        }
        args.add("package");
        processBuilder.command(args.toArray(new String[]{}));
        executeProcess(context, processBuilder);
    }
}
