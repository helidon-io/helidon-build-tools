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
import io.helidon.build.util.MavenCommand;
import io.helidon.build.util.Style;

import static io.helidon.build.cli.harness.CommandContext.Verbosity.NORMAL;
import static io.helidon.build.util.AnsiConsoleInstaller.clearScreen;
import static io.helidon.build.util.Constants.DEV_LOOP_START_MESSAGE;

/**
 * The {@code dev} command.
 */
@Command(name = "dev", description = "Continuous application development")
public final class DevCommand extends BaseCommand implements CommandExecution {

    private static final String CLEAN_PROP_PREFIX = "-Ddev.clean=";
    private static final String FORK_PROP_PREFIX = "-Ddev.fork=";
    private static final String DO_NOT_USE_MAVEN_LOG = "-Ddev.useMavenLog=false";
    private static final String DEBUG_PORT_PROPERTY = "debug.port";
    private static final String DEV_GOAL = "helidon:dev";
    private static final String MAVEN_LOG_LEVEL_START = "[";
    private static final String MAVEN_LOG_LEVEL_END = "]";
    private static final String MAVEN_ERROR_LEVEL = "ERROR";


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

        // Clear the terminal if Ansi escapes are enabled and print header

        clearScreen();
        System.out.println();
        System.out.print(Style.Bold.apply("dev loop starting "));
        System.out.flush();

        // Execute helidon-maven-plugin to enter dev loop

        Consumer<String> stdOut = context.verbosity() == NORMAL
                ? DevCommand::printMavenErrorLinesOnly
                : DevCommand::printAllLines;

        MavenCommand.builder()
                .verbose(context.verbosity() != NORMAL)
                .stdOut(stdOut)
                .filter(new OnlyLoopOutput())
                .addArgument(DEV_GOAL)
                .addArgument(DO_NOT_USE_MAVEN_LOG)
                .addArgument(CLEAN_PROP_PREFIX + clean)
                .addArgument(FORK_PROP_PREFIX + fork)
                .directory(commonOptions.project())
                .debugPort(Integer.getInteger(DEBUG_PORT_PROPERTY, 0))
                .build()
                .execute();
    }

    private static class OnlyLoopOutput implements Predicate<String> {
        private static final String DEBUGGER_LISTEN_MESSAGE_PREFIX = "Listening for transport";
        private static final int LINES_PER_UPDATE = 3;
        private static final int MAX_UPDATES = 3;
        private boolean debugger;
        private boolean started;
        private int updates;
        private int updateCountDown;

        @Override
        public boolean test(String line) {
            if (started) {
                return true;
            } else if (line.endsWith(DEV_LOOP_START_MESSAGE)) {
                started = true;
            } else if (line.startsWith(DEBUGGER_LISTEN_MESSAGE_PREFIX)) {
                debugger = true;
                return true;
            } else if (updateCountDown == 0) {
                if (updates < MAX_UPDATES) {
                    if (debugger) {
                        System.out.println();
                        updates = MAX_UPDATES;
                    } else {
                        System.out.print('.');
                        updateCountDown = LINES_PER_UPDATE;
                        updates++;
                    }
                }
            } else {
                updateCountDown--;
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
