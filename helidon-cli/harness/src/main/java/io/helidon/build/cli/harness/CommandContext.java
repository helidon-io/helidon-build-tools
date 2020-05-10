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

package io.helidon.build.cli.harness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.build.cli.harness.CommandModel.CommandInfo;
import io.helidon.build.util.Requirements;
import io.helidon.build.util.Log;
import io.helidon.build.util.Style;
import io.helidon.build.util.SystemLogWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static io.helidon.build.cli.harness.CommandContext.Verbosity.DEBUG;
import static io.helidon.build.cli.harness.CommandContext.Verbosity.VERBOSE;
import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.Style.BoldRed;
import static io.helidon.build.util.Style.BoldYellow;
import static io.helidon.build.util.Style.Red;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * The command context.
 */
public final class CommandContext {

    private final CLIDefinition cli;
    private final Logger logger;
    private final CommandRegistry registry;
    private final LogHandler logHandler;
    private ExitAction exitAction;
    private CommandParser parser;

    CommandContext(CommandContext parent) {
        this.cli = parent.cli;
        this.logger = parent.logger;
        this.registry = parent.registry;
        this.logHandler = parent.logHandler;
        this.exitAction = new ExitAction();
    }

    private CommandContext(CommandRegistry registry, CLIDefinition cli) {
        this.cli = Objects.requireNonNull(cli, "cli is null");
        this.logger = Logger.getAnonymousLogger();
        this.logger.setUseParentHandlers(false);
        this.logHandler = new LogHandler();
        this.logger.addHandler(logHandler);
        this.registry = Objects.requireNonNull(registry, "registry is null");
        this.exitAction = new ExitAction();
    }

    /**
     * Exit status.
     */
    public enum ExitStatus {
        /**
         * Success exit status.
         */
        SUCCESS,

        /**
         * Warning exit status.
         */
        WARNING,

        /**
         * Failure exit status.
         */
        FAILURE;

        /**
         * Test if this status is worse than the given one.
         *
         * @param status status to compare with
         * @return {@code true} if worse, {@code false} if not
         */
        public boolean isWorse(ExitStatus status) {
            return ordinal() > status.ordinal();
        }
    }

    /**
     * Exit action.
     */
    public final class ExitAction {

        private final ExitStatus status;
        private final String message;
        private final Throwable failure;

        private ExitAction() {
            this.status = ExitStatus.SUCCESS;
            this.message = null;
            this.failure = null;
        }

        ExitAction(ExitStatus status, String message) {
            this.status = Objects.requireNonNull(status, "exit status is null");
            this.message = Objects.requireNonNull(message, "message is null");
            this.failure = null;
        }

        ExitAction(Throwable failure) {
            this.status = ExitStatus.FAILURE;
            this.failure = Objects.requireNonNull(failure, "failure is null");
            this.message = null;
        }

        /**
         * Get the exit status.
         *
         * @return exit status, never {@code null}
         */
        public ExitStatus status() {
            return status;
        }

        /**
         * Get the exit message.
         *
         * @return message, may be {@code null}
         */
        public String message() {
            return message;
        }

        /**
         * Get the exit failure.
         *
         * @return failure, may be {@code null}
         */
        public Throwable failure() {
            return failure;
        }

        /**
         * Run the exit sequence for this action.
         * <b>WARNING:</b> This method invokes {@link System#exit(int)}.
         */
        @SuppressFBWarnings
        public void run() {
            switch (exitAction.status) {
                case FAILURE:
                    if (failure != null) {
                        Requirements.toFailure(failure).ifPresentOrElse(ce -> {
                            // Assume message is already rendered
                            exit(ce.getMessage(), null, Level.INFO, 1);
                        }, () -> {
                            // Otherwise just use the message in the exception
                            exit(LogHandler.THROWN_ONLY_MESSAGE, failure, SEVERE, 1);
                        });
                    } else {
                        exit(message, null, SEVERE, 1);
                    }
                    break;
                case WARNING:
                    exit(message, null, WARNING, 0);
                    break;
                default:
                    System.exit(0);
            }
        }

        private void exit(String message, Throwable error, Level level, int statusCode) {
            if (message != null && !message.isEmpty()) {
                logLine();
                if (Style.isStyled(message)) {
                    logInfo(message);
                } else {
                    log(level, error, message);
                }
                logLine();
            }
            System.exit(0);
        }
    }

    /**
     * Get the CLI definition.
     *
     * @return CLI definition, never {@code null}
     */
    public CLIDefinition cli() {
        return cli;
    }

    /**
     * Get a command model by name.
     *
     * @param name command name
     * @return optional of command model, never {@code null}
     */
    public Optional<CommandModel> command(String name) {
        return registry.get(name);
    }

    /**
     * Get all commands.
     *
     * @return collection of command models, never {@code null}
     */
    public Collection<CommandModel> allCommands() {
        return registry.all();
    }

    /**
     * Log an empty line.
     */
    public void logLine() {
        log(Level.INFO, null, "");
    }

    /**
     * Log an INFO message.
     *
     * @param message INFO message to log
     * @param args message args passed to {@link String#format}.
     */
    public void logInfo(String message, Object... args) {
        log(Level.INFO, null, message, args);
    }

    /**
     * Log a WARNING message.
     *
     * @param message WARNING message to log
     * @param args message args passed to {@link String#format}.
     */
    public void logWarning(String message, Object... args) {
        log(Level.WARNING, null, message, args);
    }

    /**
     * Log a SEVERE message.
     *
     * @param message SEVERE message to log
     * @param args message args passed to {@link String#format}.
     */
    public void logError(String message, Object... args) {
        log(SEVERE, null, message, args);
    }

    /**
     * Log a SEVERE message.
     *
     * @param error error
     * @param message SEVERE message to log
     * @param args message args passed to {@link String#format}.
     */
    public void logError(Throwable error, String message, Object... args) {
        log(SEVERE, error, message, args);
    }

    /**
     * Log a FINE message.
     *
     * @param message FINE message to log
     * @param args message args passed to {@link String#format}.
     */
    public void logVerbose(String message, Object... args) {
        log(Level.FINE, null, message, args);
    }

    /**
     * Log a FINEST message.
     *
     * @param message FINEST message to log
     * @param args message args passed to {@link String#format}.
     */
    public void logDebug(String message, Object... args) {
        log(Level.FINEST, null, message, args);
    }

    private void log(Level level, Throwable error, String message, Object... args) {
        String msg = message == null ? "" : String.format(message, args);
        logger.log(level, msg, error);
    }

    /**
     * Execute a nested command.
     *
     * @param args raw arguments
     */
    public void execute(String... args) {
        CommandContext context = new CommandContext(this);
        CommandRunner.execute(context, args);
        this.exitAction = context.exitAction;
    }

    /**
     * Set the error message if not already set.
     *
     * @param status exit status
     * @param message error message
     * @return exit action.
     */
    public ExitAction exitAction(ExitStatus status, String message) {
        if (status.isWorse(exitAction.status)) {
            exitAction = new ExitAction(status, message);
        }
        return exitAction;
    }

    /**
     * Set the error.
     *
     * @param error error
     * @return exit action.
     */
    public ExitAction exitAction(Throwable error) {
        exitAction = new ExitAction(error);
        return exitAction;
    }

    /**
     * Get the exit action.
     *
     * @return exit action, never {@code null}
     */
    public ExitAction exitAction() {
        return exitAction;
    }

    /**
     * Set the command parser.
     *
     * @param parser parser
     */
    void parser(CommandParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser is null");
    }

    /**
     * Get the command parser.
     *
     * @return command parser
     * @throws IllegalStateException if parser is not set
     */
    public Properties properties() {
        if (parser == null) {
            return new Properties();
        }
        return parser.properties();
    }

    /**
     * Returns the verbosity level.
     *
     * @return The level.
     */
    public Verbosity verbosity() {
        return logHandler.verbosity;
    }

    /**
     * Enable verbose mode.
     *
     * @param verbosity verbosity value
     */
    void verbosity(Verbosity verbosity) {
        this.logHandler.verbosity = verbosity;
        if (verbosity == DEBUG) {
            SystemLogWriter.bind(Log.Level.DEBUG);
        } else if (verbosity == VERBOSE) {
            // TODO SystemLogWriter.bind(Log.Level.FINE);
        }
    }

    /**
     * Set the exit action to {@link ExitStatus#FAILURE} and display an error message.
     *
     * @param command command name
     */
    void commandNotFoundError(String command) {
        List<String> allCommandNames = registry.commandsByName()
                                               .values()
                                               .stream()
                                               .map(CommandModel::command)
                                               .map(CommandInfo::name)
                                               .collect(Collectors.toList());
        String match = CommandMatcher.match(command, allCommandNames);
        String cliName = cli.name();
        if (match != null) {
            error(String.format("'%s' is not a valid command.%nDid you mean '%s'?%nSee '%s --help' for more information",
                                command, match, cliName));
        } else {
            error(String.format("'%s' is not a valid command.%nSee '%s --help' for more information", command, cliName));
        }
    }

    /**
     * Set the exit action to {@link ExitStatus#FAILURE} with the given error message.
     *
     * @param message error message
     * @param args message args.
     * @return exit action.
     */
    ExitAction error(String message, Object... args) {
        return exitAction(ExitStatus.FAILURE, String.format(message, args));
    }

    /**
     * Set the exit action to {@link ExitStatus#FAILURE} with the given error.
     *
     * @param error error
     * @return exit action.
     */
    ExitAction error(Throwable error) {
        return exitAction(error);
    }

    /**
     * Create a new command context.
     *
     * @param registry command registry
     * @param cliDef CLI definition
     * @return command context, never {@code null}
     */
    public static CommandContext create(CommandRegistry registry, CLIDefinition cliDef) {
        return new CommandContext(registry, cliDef);
    }

    /**
     * Verbosity levels.
     */
    public enum Verbosity {
        /**
         * Normal level.
         */
        NORMAL,
        /**
         * Verbose level.
         */
        VERBOSE,
        /**
         * Debug level.
         */
        DEBUG
    }

    /**
     * Custom log handler to print the message to {@code stdout} and {@code stderr}.
     */
    private static final class LogHandler extends Handler {
        static final String THROWN_ONLY_MESSAGE = "<thrown-only>";

        private Verbosity verbosity = Verbosity.NORMAL;

        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            String message = record.getMessage();
            boolean verbose = verbosity == Verbosity.VERBOSE || verbosity == Verbosity.DEBUG;

            if (level == Level.INFO) {
                System.out.println(message);
            } else if (level == Level.WARNING) {
                System.err.println(BoldYellow.apply(message));
            } else if (level == SEVERE) {
                StringBuilder sb = new StringBuilder();
                boolean thrownOnly = message.equalsIgnoreCase(THROWN_ONLY_MESSAGE);
                if (!thrownOnly) {
                    sb.append(BoldRed.apply(message));
                }
                if (verbose) {
                    String trace = toStackTrace(record);
                    if (trace != null) {
                        if (!thrownOnly) {
                            sb.append(EOL);
                        }
                        sb.append(Red.apply(trace));
                    }
                } else if (thrownOnly) {
                    sb.append(BoldRed.apply(record.getThrown().getMessage()));
                }
                System.err.println(sb.toString());
            } else if (verbose && (level == Level.CONFIG || level == Level.FINE)) {
                System.out.println(message);
            } else if (verbosity == Verbosity.DEBUG) {
                System.out.println(message);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        private static String toStackTrace(final LogRecord record) {
            Throwable thrown = record.getThrown();
            if (thrown != null) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    thrown.printStackTrace(pw);
                    pw.close();
                    return sw.toString();
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }
}
