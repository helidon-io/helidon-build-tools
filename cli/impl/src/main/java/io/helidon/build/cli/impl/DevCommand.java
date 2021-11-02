/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.common.Log;
import io.helidon.build.common.Strings;
import io.helidon.build.common.ansi.AnsiConsoleInstaller;
import io.helidon.build.common.maven.MavenCommand;

import static io.helidon.build.cli.common.CliProperties.HELIDON_CLI_PLUGIN_VERSION_PROPERTY;
import static io.helidon.build.cli.harness.CommandContext.Verbosity.DEBUG;
import static io.helidon.build.cli.harness.CommandContext.Verbosity.NORMAL;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.cli.impl.CommandRequirements.requireValidMavenProjectConfig;
import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBrightGreen;
import static io.helidon.build.common.ansi.AnsiTextStyles.Red;
import static io.helidon.build.common.ansi.ConsoleUtils.clearScreen;
import static io.helidon.build.common.ansi.ConsoleUtils.hideCursor;
import static io.helidon.build.common.ansi.ConsoleUtils.rewriteLine;
import static io.helidon.build.common.ansi.ConsoleUtils.showCursor;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_APPLICATION_FAILED;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_APPLICATION_STARTING;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_BUILD_FAILED;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_BUILD_STARTING;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_HEADER;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_MESSAGE_PREFIX;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_START;
import static io.helidon.build.devloop.common.DevLoopMessages.DEV_LOOP_STYLED_MESSAGE_PREFIX;
import static java.lang.System.currentTimeMillis;

/**
 * The {@code dev} command.
 */
@Command(name = "dev", description = "Continuous application development")
public final class DevCommand extends BaseCommand {

    private static final String CLEAN_PROP_PREFIX = "-Ddev.clean=";
    private static final String FORK_PROP_PREFIX = "-Ddev.fork=";
    private static final String TERMINAL_MODE_PROP_PREFIX = "-Ddev.terminalMode=";
    private static final String APP_JVM_ARGS_PROP_PREFIX = "-Ddev.appJvmArgs=";
    private static final String APP_ARGS_PROP_PREFIX = "-Ddev.appArgs=";
    private static final String CLI_MAVEN_PLUGIN = "io.helidon.build-tools:helidon-cli-maven-plugin";
    private static final String DEV_GOAL_SUFFIX = ":dev";
    private static final String MAVEN_LOG_LEVEL_START = "[";
    private static final String MAVEN_LOG_LEVEL_END = "]";
    private static final String MAVEN_ERROR_LEVEL = "ERROR";
    private static final String MAVEN_FATAL_LEVEL = "FATAL";
    private static final String SLF4J_PREFIX = "SLF4J:";
    private static final String HEADER = "%n" + Bold.apply(DEV_LOOP_HEADER + " %s ");
    private static final String STARTING = BoldBrightGreen.apply("starting");
    private static final String EXITING = BoldBrightGreen.apply("exiting");
    private static final String DEFAULT_DEBUG_PORT = "5005";
    private static final String DEBUG_ARGS = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=*:%s";

    private final CommonOptions commonOptions;
    private final boolean clean;
    private final boolean fork;
    private final String appJvmArgs;
    private final String appArgs;
    private final String pluginVersion;
    private final boolean useCurrentPluginVersion;
    private TerminalModeOutput terminalModeOutput;

    /**
     * Constructor for {@link InitCommand}.
     *
     * @param commonOptions Common options.
     */
    DevCommand(CommonOptions commonOptions) {
        this(commonOptions, true, false, null, null, false, null,
             false, null, false);
    }

    @Creator
    DevCommand(CommonOptions commonOptions,
               @Flag(name = "clean", description = "Perform a clean before the first build") boolean clean,
               @Flag(name = "fork", description = "Fork mvn execution") boolean fork,
               @KeyValue(name = "app-jvm-args", description = "JVM args used when starting the application")
                       String appJvmArgs,
               @KeyValue(name = "app-args", description = "Application args used when starting the application")
                       String appArgs,
               @Flag(name = "app-debug", description = "Enable application debugger")
                       boolean appDebug,
               @KeyValue(name = "app-debug-port", description = "Specify application debugger port")
                       String appDebugPort,
               @Flag(name = "app-debug-no-wait", description = "Do not wait for debugger on application start")
                       boolean appDebugNoWait,
               @KeyValue(name = "plugin-version", description = "helidon-cli-maven-plugin version", visible = false)
                       String pluginVersion,
               @Flag(name = "current", description = "Use the build version as the helidon-cli-maven-plugin version",
                       visible = false)
                       boolean useCurrentPluginVersion) {
        super(commonOptions, true);
        this.commonOptions = commonOptions;
        this.clean = clean;
        this.fork = fork;
        this.appJvmArgs = appJvmArgs(appJvmArgs, appDebug, appDebugPort, appDebugNoWait);
        this.appArgs = appArgs;
        this.pluginVersion = pluginVersion;
        this.useCurrentPluginVersion = useCurrentPluginVersion;
    }

    private static String appJvmArgs(String appJvmArgs, boolean appDebug, String appDebugPort, boolean appDebugNoWait) {
        String result = Strings.isValid(appJvmArgs) ? appJvmArgs : null;
        String debugArgs = debugArgs(appDebug, appDebugPort, appDebugNoWait);
        return debugArgs == null ? result : result == null ? debugArgs : result + " " + debugArgs;
    }

    private static String debugArgs(boolean appDebug, String appDebugPort, boolean appDebugNoWait) {
        boolean havePort = Strings.isValid(appDebugPort);
        if (appDebug || havePort || appDebugNoWait) {
            String port = havePort ? appDebugPort : DEFAULT_DEBUG_PORT;
            String suspend = appDebugNoWait ? "n" : "y";
            return String.format(DEBUG_ARGS, suspend, port);
        } else {
            return null;
        }
    }

    @Override
    protected void assertPreconditions() {
        requireMinimumMavenVersion();
        requireValidMavenProjectConfig(commonOptions);
    }

    @Override
    protected void invoke(CommandContext context) throws Exception {

        // Dev goal

        String devGoal = CLI_MAVEN_PLUGIN;
        String cliPluginVersionProperty = null;
        String defaultPluginVersion = defaultHelidonPluginVersion(pluginVersion, useCurrentPluginVersion);
        String cliPluginVersion = cliPluginVersion(defaultPluginVersion);
        if (cliPluginVersion != null) {
            Log.verbose("Using CLI plugin version %s", cliPluginVersion);
            devGoal += ":" + cliPluginVersion;
            // Pass along the version so that the loop can specify it when doing full builds
            cliPluginVersionProperty = String.format("-D%s=%s", HELIDON_CLI_PLUGIN_VERSION_PROPERTY, cliPluginVersion);
        }
        devGoal += DEV_GOAL_SUFFIX;

        // Application args

        String jvmArgs = appJvmArgs == null ? null : APP_JVM_ARGS_PROP_PREFIX + appJvmArgs;
        String args = appArgs == null ? null : APP_ARGS_PROP_PREFIX + appArgs;

        // Clear terminal and print header if in terminal mode

        Verbosity verbosity = context.verbosity();
        boolean terminalMode = verbosity == NORMAL;
        if (terminalMode) {
            clearScreen();
            printState(STARTING, false);
            terminalModeOutput = new TerminalModeOutput();
        }

        // Add a shutdown hook to print an exit message

        Runtime.getRuntime().addShutdownHook(new Thread(DevCommand::exiting));

        // Execute helidon-maven-cli-plugin to enter dev loop

        Consumer<String> stdOut = terminalMode
                ? terminalModeOutput
                : DevCommand::printStdOutLine;

        Consumer<String> stdErr = DevCommand::printStdErrLine;

        Predicate<String> filter = terminalMode
                ? terminalModeOutput
                : DevCommand::printAllLines;

        MavenCommand.builder()
                    .verbose(verbosity == DEBUG)
                    .stdOut(stdOut)
                    .stdErr(stdErr)
                    .filter(filter)
                    .addArgument(devGoal)
                    .addArgument(CLEAN_PROP_PREFIX + clean)
                    .addArgument(FORK_PROP_PREFIX + fork)
                    .addArgument(TERMINAL_MODE_PROP_PREFIX + terminalMode)
                    .addArguments(context.propertyArgs(true))
                    .addOptionalArgument(cliPluginVersionProperty)
                    .addOptionalArgument(jvmArgs)
                    .addOptionalArgument(args)
                    .directory(commonOptions.project())
                    .build()
                    .execute();
    }

    private static void exiting() {
        showCursor();
        printState(EXITING, true);
    }

    private static void printState(String state, boolean newline) {
        final String header = newline ? HEADER + "%n" : HEADER;
        System.out.printf(header, state);
        System.out.flush();
    }

    private static boolean printAllLines(String line) {
        return true;
    }

    private static void printStdOutLine(String line) {
        System.out.println(line);
    }

    private static void printStdErrLine(String line) {
        System.out.println(Red.apply(line));
    }


    /**
     * A stateful filter/transform that cleans up output from {@code DevLoop}.
     */
    private static class TerminalModeOutput implements Predicate<String>, Consumer<String> {
        private static final String DEBUGGER_LISTEN_MESSAGE_PREFIX = "Listening for transport";
        private static final String DOWNLOADING_MESSAGE_PREFIX = "Downloading from";
        private static final String BUILD_SUCCEEDED = "BUILD SUCCESS";
        private static final String BUILD_FAILED = "BUILD FAILURE";
        private static final String HELP_TAG = "[Help";
        private static final String AT_TAG = " @ ";
        private static final String DOWNLOADING_ARTIFACTS = " downloading artifacts ";
        private static final long PROGRESS_UPDATE_MILLIS = 100;
        private static final String[] SPINNER = {
                ".   ",
                "..  ",
                "... ",
                " .. ",
                "  . "
        };

        private final boolean ansiEnabled;
        private boolean debugger;
        private long lastProgressMillis;
        private int progressIndex;
        private boolean progressStarted;
        private boolean progressCompleted;
        private boolean skipHeader;
        private boolean devLoopStarted;
        private boolean buildFailed;
        private int buildFailedErrorCount;
        private boolean suspendOutput;
        private boolean insertLine;
        private boolean appendLine;
        private boolean insertLineIfError;

        private TerminalModeOutput() {
            this.ansiEnabled = AnsiConsoleInstaller.areAnsiEscapesEnabled();
        }

        @Override
        public boolean test(String line) {
            if (devLoopStarted) {
                if (line.contains(DEV_LOOP_HEADER)) {
                    clearProgressIndicator();
                    if (skipHeader) {
                        skipHeader = false;
                        System.out.println();
                    } else {
                        header(line);
                    }
                    return false;
                } else if (line.startsWith(DEV_LOOP_STYLED_MESSAGE_PREFIX)
                           || line.startsWith(DEV_LOOP_MESSAGE_PREFIX)) {
                    if (line.contains(DEV_LOOP_BUILD_STARTING)) {
                        insertLineIfError = true;
                    } else if (line.contains(DEV_LOOP_APPLICATION_STARTING)) {
                        appendLine = true;
                    } else if (line.contains(DEV_LOOP_BUILD_FAILED)
                               || line.contains(DEV_LOOP_APPLICATION_FAILED)) {
                        insertLine = true;
                    }
                    restoreOutput();
                    return true;
                } else if (line.startsWith(SLF4J_PREFIX)) {
                    return false;
                } else if (buildFailed) {
                    return isErrorMessage(line);
                } else if (suspendOutput) {
                    return false;
                } else if (line.contains(BUILD_FAILED)) {
                    buildFailed = true;
                    return false;
                } else if (line.contains(BUILD_SUCCEEDED)
                           || line.contains(HELP_TAG)) {
                    suspendOutput();
                    return false;
                } else {
                    return !line.equals(AT_TAG);
                }
            } else if (line.endsWith(DEV_LOOP_START)) {
                devLoopStarted = true;
                insertLine = !ansiEnabled;
                return false;
            } else if (line.startsWith(DEBUGGER_LISTEN_MESSAGE_PREFIX)) {
                debugger = true;
                return true;
            } else if (isErrorMessage(line)) {
                return true;
            } else {
                updateProgress(line);
                return false;
            }
        }

        private boolean isErrorMessage(String line) {
            if (errorMessage(line) != null) {
                devLoopStarted = true;
                if (buildFailed) {
                    buildFailedErrorCount++;
                    if (buildFailedErrorCount >= 2) {
                        // Only log the first error if build failed.
                        return false;
                    }
                }
                insertLine = true;
                insertLineIfError = ansiEnabled;
                return true;
            } else {
                return false;
            }
        }

        private void suspendOutput() {
            suspendOutput = true;
        }

        private void restoreOutput() {
            suspendOutput = false;
        }

        private void updateProgress(String line) {
            if (!progressCompleted) {
                if (debugger) {
                    System.out.println();
                    progressCompleted = true;
                } else if (!skipHeader) {
                    if (line.contains(DOWNLOADING_MESSAGE_PREFIX)) {
                        header(Bold.apply(DEV_LOOP_HEADER));
                        System.out.print(DEV_LOOP_STYLED_MESSAGE_PREFIX + BoldBlue.apply(DOWNLOADING_ARTIFACTS));
                        System.out.flush();
                        skipHeader = true;
                        progressStarted = false;
                    }
                }
                if (ansiEnabled) {
                    updateProgressIndicator();
                }
            }
        }

        private void updateProgressIndicator() {
            final long currentMillis = currentTimeMillis();
            if (progressStarted) {
                final long elapsedMillis = currentMillis - lastProgressMillis;
                if (elapsedMillis >= PROGRESS_UPDATE_MILLIS) {
                    rewriteLine(SPINNER[progressIndex++]);
                }
            } else {
                hideCursor();
                System.out.print(SPINNER[progressIndex++]);
                System.out.flush();
                progressStarted = true;
            }
            if (progressIndex == SPINNER.length) {
                progressIndex = 0;
            }
            lastProgressMillis = currentMillis;
        }

        private void clearProgressIndicator() {
            if (progressStarted) {
                final int charsToClear = SPINNER[0].length();
                final String blank = " ".repeat(charsToClear);
                rewriteLine(charsToClear, blank);
                showCursor();
                progressCompleted = true;
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
                    if (Strings.isValid(errorMessage)) {
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
