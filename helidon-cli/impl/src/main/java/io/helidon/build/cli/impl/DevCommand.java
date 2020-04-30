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
import io.helidon.build.cli.harness.CommandContext.Verbosity;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.util.MavenCommand;

import static io.helidon.build.cli.harness.CommandContext.Verbosity.DEBUG;
import static io.helidon.build.cli.harness.CommandContext.Verbosity.NORMAL;
import static io.helidon.build.util.AnsiConsoleInstaller.clearScreen;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_FAILED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_MESSAGE_PREFIX;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_SERVER_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_START_MESSAGE;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_STYLED_MESSAGE_PREFIX;
import static io.helidon.build.util.Style.Bold;
import static io.helidon.build.util.Style.BoldBrightGreen;

/**
 * The {@code dev} command.
 */
@Command(name = "dev", description = "Continuous application development")
public final class DevCommand extends MavenBaseCommand implements CommandExecution {

    private static final String CLEAN_PROP_PREFIX = "-Ddev.clean=";
    private static final String FORK_PROP_PREFIX = "-Ddev.fork=";
    private static final String TERMINAL_MODE_PROP_PREFIX = "-Ddev.terminalMode=";
    private static final String DEBUG_PORT_PROPERTY = "debug.port";
    private static final String DEV_GOAL = "helidon:dev";
    private static final String MAVEN_LOG_LEVEL_START = "[";
    private static final String MAVEN_LOG_LEVEL_END = "]";
    private static final String MAVEN_ERROR_LEVEL = "ERROR";
    private static final String MAVEN_FATAL_LEVEL = "FATAL";

    private final CommonOptions commonOptions;
    private final boolean clean;
    private final boolean fork;
    private TerminalModeOutput terminalModeOutput;

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
        if (isMavenVersionOutOfDate(context)) {
            return;
        }

        Verbosity verbosity = context.verbosity();
        boolean terminalMode = verbosity == NORMAL;

        // Clear terminal and print header if in terminal mode

        if (terminalMode) {
            clearScreen();
            System.out.println();
            System.out.print(Bold.apply("helidon dev ") + BoldBrightGreen.apply("starting "));
            System.out.flush();
            terminalModeOutput = new TerminalModeOutput();
        }

        // Execute helidon-maven-plugin to enter dev loop

        Consumer<String> stdOut = terminalMode
                ? terminalModeOutput
                : DevCommand::printAllLines;

        Predicate<String> filter = terminalMode
                ? terminalModeOutput
                : line -> true;

        MavenCommand.builder()
                .verbose(verbosity == DEBUG)
                .stdOut(stdOut)
                .filter(filter)
                .addArgument(DEV_GOAL)
                .addArgument(CLEAN_PROP_PREFIX + clean)
                .addArgument(FORK_PROP_PREFIX + fork)
                .addArgument(TERMINAL_MODE_PROP_PREFIX + terminalMode)
                .directory(commonOptions.project())
                .debugPort(Integer.getInteger(DEBUG_PORT_PROPERTY, 0))
                .build()
                .execute();
    }

    private static void printAllLines(String line) {
        System.out.println(line);
    }

    /**
     * A stateful filter/transform that cleans up output from {@code DevLoop}.
     */
    private static class TerminalModeOutput implements Predicate<String>, Consumer<String> {
        private static final String DEBUGGER_LISTEN_MESSAGE_PREFIX = "Listening for transport";
        private static final String BUILD_SUCCEEDED = "BUILD SUCCESS";
        private static final String BUILD_FAILED = "BUILD FAILURE";
        private static final String HELP_TAG = "[Help";
        private static final String AT_TAG = " @ ";
        private static final int LINES_PER_UPDATE = 3;
        private static final int MAX_UPDATES = 3;

        private boolean debugger;
        private int devLoopStartingUpdates;
        private int devLoopStartingCountDown;
        private boolean devLoopStarted;
        private boolean suspendOutput;
        private boolean insertLine;
        private boolean appendLine;
        private boolean appendLineIfError;

        @Override
        public boolean test(String line) {
            if (devLoopStarted) {
                if (line.startsWith(DEV_LOOP_STYLED_MESSAGE_PREFIX)
                        || line.startsWith(DEV_LOOP_MESSAGE_PREFIX)) {
                    if (line.contains(DEV_LOOP_BUILD_STARTING)) {
                        appendLineIfError = true;
                    } else if (line.contains(DEV_LOOP_SERVER_STARTING)) {
                        appendLine = true;
                    } else if (line.contains(DEV_LOOP_BUILD_FAILED)) {
                        insertLine = true;
                    }
                    restoreOutput();
                    return true;
                } else if (suspendOutput) {
                    return false;
                } else if (line.contains(BUILD_SUCCEEDED)
                        || line.contains(BUILD_FAILED)
                        || line.contains(HELP_TAG)) {
                    suspendOutput();
                    return false;
                } else {
                    return !line.equals(AT_TAG);
                }
            } else if (line.endsWith(DEV_LOOP_START_MESSAGE)) {
                devLoopStarted = true;
                return false;
            } else if (line.startsWith(DEBUGGER_LISTEN_MESSAGE_PREFIX)) {
                debugger = true;
                return true;
            } else {
                updateProgress();
                return false;
            }
        }

        private void suspendOutput() {
            suspendOutput = true;
        }

        private void restoreOutput() {
            suspendOutput = false;
        }

        private void updateProgress() {
            if (devLoopStartingCountDown == 0) {
                if (devLoopStartingUpdates < MAX_UPDATES) {
                    if (debugger) {
                        System.out.println();
                        devLoopStartingUpdates = MAX_UPDATES;
                    } else {
                        System.out.print('.');
                        devLoopStartingCountDown = LINES_PER_UPDATE;
                        devLoopStartingUpdates++;
                    }
                }
            } else {
                devLoopStartingCountDown--;
            }
        }

        @Override
        public void accept(String line) {
            if (!line.isBlank()) {
                if (insertLine) {
                    System.out.println();
                    insertLine = false;
                }
                if (line.startsWith(MAVEN_LOG_LEVEL_START)) {
                    int levelEnd = line.indexOf(MAVEN_LOG_LEVEL_END);
                    if (levelEnd > 0) {
                        String level = line.substring(0, levelEnd);
                        if (level.contains(MAVEN_ERROR_LEVEL)
                                || level.contains(MAVEN_FATAL_LEVEL)) {
                            if (appendLineIfError) {
                                System.out.println();
                                appendLineIfError = false;
                            }
                            String message = line.substring(levelEnd + 2);
                            if (!message.isBlank()) {
                                System.out.println(message);
                            }
                        }
                    }
                } else {
                    System.out.println(line);
                }
                if (appendLine) {
                    System.out.println();
                    appendLine = false;
                }
            }
        }
    }
}
