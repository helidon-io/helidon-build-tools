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
import io.helidon.build.util.AnsiConsoleInstaller;
import io.helidon.build.util.MavenCommand;
import io.helidon.build.util.Style;

import static io.helidon.build.cli.harness.CommandContext.ExitStatus.FAILURE;
import static io.helidon.build.cli.harness.CommandContext.Verbosity.DEBUG;
import static io.helidon.build.cli.harness.CommandContext.Verbosity.NORMAL;
import static io.helidon.build.util.AnsiConsoleInstaller.clearScreen;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_FAILED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_HEADER;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_MESSAGE_PREFIX;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_SERVER_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_START;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_STYLED_MESSAGE_PREFIX;
import static io.helidon.build.util.FileUtils.WORKING_DIR;
import static io.helidon.build.util.ProjectConfig.ensureHelidonCliConfig;
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
    private static final String DEV_GOAL = "helidon:dev";
    private static final String MAVEN_LOG_LEVEL_START = "[";
    private static final String MAVEN_LOG_LEVEL_END = "]";
    private static final String MAVEN_ERROR_LEVEL = "ERROR";
    private static final String MAVEN_FATAL_LEVEL = "FATAL";
    private static final String SLF4J_PREFIX = "SLF4J:";

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

    private boolean isValidConfig(CommandContext context) {
        try {
            ensureHelidonCliConfig(WORKING_DIR, null);
            return true;
        } catch (Exception e) {
            context.exitAction(FAILURE, e.getMessage());
            return false;
        }
    }

    @Override
    public void execute(CommandContext context) {

        // Ensure preconditions

        if (isMavenVersionOutOfDate(context) || !isValidConfig(context)) {
            return;
        }

        // Clear terminal and print header if in terminal mode

        Verbosity verbosity = context.verbosity();
        boolean terminalMode = verbosity == NORMAL;
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
                                  : DevCommand::printStdOutLine;

        Consumer<String> stdErr = DevCommand::printStdErrLine;

        Predicate<String> filter = terminalMode
                                   ? terminalModeOutput
                                   : DevCommand::printAllLines;

        try {
            MavenCommand.builder()
                        .verbose(verbosity == DEBUG)
                        .stdOut(stdOut)
                        .stdErr(stdErr)
                        .filter(filter)
                        .addArgument(DEV_GOAL)
                        .addArgument(CLEAN_PROP_PREFIX + clean)
                        .addArgument(FORK_PROP_PREFIX + fork)
                        .addArgument(TERMINAL_MODE_PROP_PREFIX + terminalMode)
                        .directory(commonOptions.project())
                        .build()
                        .execute();
        } catch (Exception e) {
            context.exitAction(FAILURE, e.getMessage());
        }
    }

    private static boolean printAllLines(String line) {
        return true;
    }

    private static void printStdOutLine(String line) {
        System.out.println(line);
    }

    private static void printStdErrLine(String line) {
        System.out.println(Style.Red.apply(line));
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
        private boolean insertLineIfError;

        @Override
        public boolean test(String line) {
            if (devLoopStarted) {
                if (line.contains(DEV_LOOP_HEADER)) {
                    header(line);
                    return false;
                } else if (line.startsWith(DEV_LOOP_STYLED_MESSAGE_PREFIX)
                           || line.startsWith(DEV_LOOP_MESSAGE_PREFIX)) {
                    if (line.contains(DEV_LOOP_BUILD_STARTING)) {
                        insertLineIfError = true;
                    } else if (line.contains(DEV_LOOP_SERVER_STARTING)) {
                        appendLine = true;
                    } else if (line.contains(DEV_LOOP_BUILD_FAILED)) {
                        insertLine = true;
                    }
                    restoreOutput();
                    return true;
                } else if (line.startsWith(SLF4J_PREFIX)) {
                    return false;
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
            } else if (line.endsWith(DEV_LOOP_START)) {
                devLoopStarted = true;
                insertLine = !AnsiConsoleInstaller.areAnsiEscapesEnabled();
                return false;
            } else if (line.startsWith(DEBUGGER_LISTEN_MESSAGE_PREFIX)) {
                debugger = true;
                return true;
            } else if (errorMessage(line) != null) {
                devLoopStarted = true;
                insertLine = true;
                insertLineIfError = AnsiConsoleInstaller.areAnsiEscapesEnabled();
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

        private void header(String line) {
            if (clearScreen()) {
                System.out.println();
                System.out.println(line);
            }
            System.out.println();
        }

        private static String errorMessage(String line) {
            if (line.startsWith(MAVEN_LOG_LEVEL_START)) {
                int levelEnd = line.indexOf(MAVEN_LOG_LEVEL_END);
                if (levelEnd > 0) {
                    String level = line.substring(0, levelEnd);
                    if (level.contains(MAVEN_ERROR_LEVEL)
                        || level.contains(MAVEN_FATAL_LEVEL)) {
                        return line.substring(levelEnd + 2);
                    }
                }
            }
            return null;
        }

        @Override
        public void accept(String line) {
            if (!line.isBlank()) {
                if (insertLine) {
                    System.out.println();
                    insertLine = false;
                }
                if (line.startsWith(MAVEN_LOG_LEVEL_START)) {
                    String errorMessage = errorMessage(line);
                    if (errorMessage != null && !errorMessage.isBlank()) {
                        if (insertLineIfError) {
                            System.out.println();
                            insertLineIfError = false;
                        }
                        System.out.println(errorMessage);
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
