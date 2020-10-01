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
import io.helidon.build.cli.harness.Config;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.util.AnsiConsoleInstaller;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenCommand;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.RequirementFailure;
import io.helidon.build.util.StyleFunction;

import static io.helidon.build.cli.harness.CommandContext.Verbosity.DEBUG;
import static io.helidon.build.cli.harness.CommandContext.Verbosity.NORMAL;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.cli.impl.CommandRequirements.requireValidMavenProjectConfig;
import static io.helidon.build.util.ConsoleUtils.clearScreen;
import static io.helidon.build.util.ConsoleUtils.hideCursor;
import static io.helidon.build.util.ConsoleUtils.rewriteLine;
import static io.helidon.build.util.ConsoleUtils.showCursor;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_APPLICATION_FAILED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_APPLICATION_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_FAILED;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_BUILD_STARTING;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_HEADER;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_MESSAGE_PREFIX;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_START;
import static io.helidon.build.util.DevLoopMessages.DEV_LOOP_STYLED_MESSAGE_PREFIX;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static io.helidon.build.util.StyleFunction.Bold;
import static io.helidon.build.util.StyleFunction.BoldBlue;
import static io.helidon.build.util.StyleFunction.BoldBrightGreen;
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
    private static final String HELIDON_CLI_PLUGIN_VERSION_PROP_PREFIX = "-Dversion.plugin.helidon-cli=";
    private static final String CLI_MAVEN_PLUGIN = "io.helidon.build-tools:helidon-cli-maven-plugin";
    private static final String DEV_GOAL_SUFFIX = ":dev";
    private static final String MAVEN_LOG_LEVEL_START = "[";
    private static final String MAVEN_LOG_LEVEL_END = "]";
    private static final String MAVEN_ERROR_LEVEL = "ERROR";
    private static final String MAVEN_FATAL_LEVEL = "FATAL";
    private static final String SLF4J_PREFIX = "SLF4J:";

    private final CommonOptions commonOptions;
    private final boolean clean;
    private final boolean fork;
    private final String pluginVersion;
    private final String appJvmArgs;
    private final String appArgs;
    private TerminalModeOutput terminalModeOutput;

    @Creator
    DevCommand(CommonOptions commonOptions,
               @Flag(name = "clean", description = "Perform a clean before the first build") boolean clean,
               @Flag(name = "fork", description = "Fork mvn execution") boolean fork,
               @KeyValue(name = "version", description = "helidon-cli-maven-plugin version", visible = false)
                       String pluginVersion,
               @Flag(name = "current", description = "Use the build version as the helidon-cli-maven-plugin version",
                       visible = false)
                       boolean currentPluginVersion,
               @KeyValue(name = "app-jvm-args", description = "JVM args used when starting the application")
                       String appJvmArgs,
               @KeyValue(name = "app-args", description = "Application args used when starting the application")
                       String appArgs) {
        super(commonOptions, true);
        this.commonOptions = commonOptions;
        this.clean = clean;
        this.fork = fork;
        this.pluginVersion = pluginVersion == null ? (currentPluginVersion ? Config.buildVersion() : null) : pluginVersion;
        this.appJvmArgs = appJvmArgs;
        this.appArgs = appArgs;
    }

    @Override
    protected void assertPreconditions() {
        requireMinimumMavenVersion();
        requireValidMavenProjectConfig(commonOptions);
    }

    @Override
    protected void invoke(CommandContext context) throws Exception {

        // Dev goal

        String cliPluginVersion = cliPluginVersion();
        String devGoal = CLI_MAVEN_PLUGIN;
        String cliPluginVersionProperty = null;
        if (cliPluginVersion != null) {
            Log.verbose("Using CLI plugin version %s", cliPluginVersion);
            devGoal += ":" + cliPluginVersion;
            // Pass along the version so that the loop can specify it when doing full builds
            cliPluginVersionProperty = HELIDON_CLI_PLUGIN_VERSION_PROP_PREFIX + cliPluginVersion;
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
            System.out.println();
            System.out.print(Bold.apply("helidon dev ") + BoldBrightGreen.apply("starting "));
            System.out.flush();
            terminalModeOutput = new TerminalModeOutput();
        }

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

    private String cliPluginVersion() {
        if (pluginVersion == null) {

            // No plugin version was specified via command line options, so we need to select it
            // from metadata based on the Helidon version for the project.

            // First, try to find the Helidon version from the project config (.helidon file). Note
            // that if this file was deleted and no build has occurred, a minimal project config is
            // generated in our preconditions, which will attempt to find and store the Helidon
            // version by reading the pom and checking for a Helidon parent pom. Though a Helidon
            // parent pom will normally be present, it is not required so we may not find it in the
            // config; in that case, fallback to the latest version. If we fail to get that, use our
            // build version.

            Metadata meta = metadata();
            ProjectConfig projectConfig = projectConfig();
            String helidonVersionProperty = projectConfig.property(ProjectConfig.HELIDON_VERSION);
            String buildVersion = Config.buildVersion();
            MavenVersion helidonVersion;
            if (helidonVersionProperty == null) {
                try {
                    helidonVersion = meta.latestVersion();
                    Log.debug("helidon.version missing in %s, using latest: %s", projectConfig.file(), helidonVersion);
                } catch (Exception e) {
                    helidonVersion = toMavenVersion(buildVersion);
                    Log.debug("unable to lookup latest Helidon version, using build version %s: %s",
                              buildVersion, e.getMessage());
                }
            } else {
                helidonVersion = toMavenVersion(helidonVersionProperty);
            }

            // Short circuit if Helidon version is qualified since metadata only exists for releases

            if (helidonVersion.isQualified()) {
                Log.debug("Helidon version %s not a release, using current CLI version %s", helidonVersion, buildVersion);
                return buildVersion;
            }

            // Now lookup and return the CLI plugin version (which will short circuit if Helidon version is
            // prior to the existence of the CLI plugin).

            try {
                Log.debug("using Helidon version %s to find CLI plugin version", helidonVersion);
                return meta.cliPluginVersion(helidonVersion, true).toString();
            } catch (Plugins.PluginFailed e) {
                Log.debug("unable to lookup CLI plugin version for Helidon version %s: %s", helidonVersion, e.getMessage());
            } catch (RequirementFailure e) {
                Log.debug("CLI plugin version not specified for Helidon version %s: %s", helidonVersion);
            } catch (Exception e) {
                Log.debug("unable to lookup CLI plugin version for Helidon version %s: %s", helidonVersion, e.toString());
            }

            // We failed so return null to let the project pom dictate the version (which will fail if not configured)

            return null;
        } else {
            return pluginVersion;
        }
    }

    private static boolean printAllLines(String line) {
        return true;
    }

    private static void printStdOutLine(String line) {
        System.out.println(line);
    }

    private static void printStdErrLine(String line) {
        System.out.println(StyleFunction.Red.apply(line));
    }

    /**
     * A stateful filter/transform that cleans up output from {@code DevLoop}.
     */
    private static class TerminalModeOutput implements Predicate<String>, Consumer<String> {
        private static final String DEBUGGER_LISTEN_MESSAGE_PREFIX = "Listening for transport";
        private static final String SCANNING_MESSAGE_PREFIX = "Scanning for";
        private static final String DOWNLOADING_MESSAGE_PREFIX = "Downloading from";
        private static final String BUILD_SUCCEEDED = "BUILD SUCCESS";
        private static final String BUILD_FAILED = "BUILD FAILURE";
        private static final String HELP_TAG = "[Help";
        private static final String AT_TAG = " @ ";
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
        private boolean downloading;
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
                } else if (!line.contains(SCANNING_MESSAGE_PREFIX)) {
                    if (!skipHeader) {
                        if (line.contains(DOWNLOADING_MESSAGE_PREFIX)) {
                            downloading = true;
                            header(Bold.apply(DEV_LOOP_HEADER));
                            System.out.print(DEV_LOOP_STYLED_MESSAGE_PREFIX + BoldBlue.apply(" downloading artifacts "));
                            System.out.flush();
                            skipHeader = true;
                        }
                    }
                    if (ansiEnabled) {
                        updateProgressIndicator();
                    }
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
                System.out.print(SPINNER[progressIndex++]);
                hideCursor();
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
