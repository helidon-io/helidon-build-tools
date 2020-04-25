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

import java.util.function.Consumer;
import java.util.function.Predicate;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;

import static io.helidon.build.util.AnsiConsoleInstaller.clearScreen;

/**
 * The {@code dev} command.
 */
@Command(name = "dev", description = "Continuous application development")
public final class DevCommand extends BaseCommand implements CommandExecution {

    private static final String CLEAN_PROP_PREFIX = "-Ddev.clean=";
    private static final String FORK_PROP_PREFIX = "-Ddev.fork=";
    private static final String DO_NOT_USE_MAVEN_LOG = "-Ddev.useMavenLog=false";
    private static final String DEV_GOAL = "helidon:dev";
    private static final String MAVEN_LOG_LEVEL_START = "[";
    private static final String MAVEN_LOG_LEVEL_END = "]";
    private static final String MAVEN_ERROR_LEVEL = "ERROR";

    /**
     * The message logged on loop start.
     * NOTE: must be kept identical to DevLoop.START_MESSAGE
     */
    private static final String START_MESSAGE = "dev loop started";

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

        // Clear the terminal if Ansi escapes are enabled

        clearScreen();

        // Execute helidon-maven-plugin to enter dev loop

        Consumer<String> stdOut = context.verbosity() == CommandContext.Verbosity.NORMAL
                                  ? DevCommand::printMavenErrorLinesOnly
                                  : DevCommand::printAllLines;
        MavenCommand.builder()
                    .description("dev loop starting")
                    .verbosity(context.verbosity())
                    .stdOut(stdOut)
                    .filter(new OnlyLoopOutput())
                    .addArgument(DEV_GOAL)
                    .addArgument(DO_NOT_USE_MAVEN_LOG)
                    .addArgument(CLEAN_PROP_PREFIX + clean)
                    .addArgument(FORK_PROP_PREFIX + fork)
                    .directory(commonOptions.project())
                    .build()
                    .execute();
    }

    private static class OnlyLoopOutput implements Predicate<String> {
        private boolean started;

        @Override
        public boolean test(String line) {
            if (started) {
                return true;
            } else if (line.endsWith(START_MESSAGE)) {
                started = true;
            }
            return false;
        }
    }

    private static void printAllLines(String line) {
        System.out.println(line);
    }

    private static void printMavenErrorLinesOnly(String line) {
        if (line.startsWith(MAVEN_LOG_LEVEL_START)) {
            int levelEnd = line.indexOf(MAVEN_LOG_LEVEL_END);
            if (levelEnd > 0 && line.substring(0, levelEnd).contains(MAVEN_ERROR_LEVEL)) {
/*
                String message = line.substring(levelEnd + 1);
                if (!message.isBlank()) {
                    System.out.println(message);
                }
*/
                System.out.println(line);
            }
        } else {
            System.out.println(line);
        }
    }
}
