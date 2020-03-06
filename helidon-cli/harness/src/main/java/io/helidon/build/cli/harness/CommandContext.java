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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

        private ExitAction() {
            this.status = ExitStatus.SUCCESS;
            this.message = null;
        }

        ExitAction(ExitStatus status, String message) {
            this.status = Objects.requireNonNull(status, "exit status is null");
            this.message = Objects.requireNonNull(message, "message is null");
        }

        /**
         * Get the exit status.
         * @return exit status, never {@code null}
         */
        public ExitStatus status() {
            return status;
        }

        /**
         * Get the exit message.
         * @return message, may be {@code null}
         */
        public String message() {
            return message;
        }

        /**
         * Run the exit sequence for this action.
         * <b>WARNING:</b> This method invokes {@link System#exit(int)}.
         */
        @SuppressFBWarnings
        public void run() {
            switch (exitAction.status) {
                case FAILURE:
                    if (message != null && !message.isEmpty()) {
                        CommandContext.this.logError(message);
                    }
                    System.exit(1);
                    break;
                case WARNING:
                    if (message != null && !message.isEmpty()) {
                        CommandContext.this.logWarning(message);
                    }
                    System.exit(0);
                    break;
                default:
                    System.exit(0);
            }
        }
    }

    /**
     * Get the CLI definition.
     * @return CLI definition, never {@code null}
     */
    public CLIDefinition cli() {
        return cli;
    }

    /**
     * Get a command model by name.
     * @param name command name
     * @return optional of command model, never {@code null}
     */
    public Optional<CommandModel> command(String name) {
        return registry.get(name);
    }

    /**
     * Get all commands.
     * @return collection of command models, never {@code null}
     */
    public Collection<CommandModel> allCommands() {
        return registry.all();
    }

    /**
     * Get the logger for this context.
     * @return logger, never {@code null}
     */
    public Logger logger() {
        return logger;
    }

    /**
     * Log an INFO message.
     * @param message INFO message to log
     */
    public void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    /**
     * Log a WARNING message.
     * @param message WARNING message to log
     */
    public void logWarning(String message) {
        logger.log(Level.WARNING, message);
    }

    /**
     * Log a SEVERE message.
     * @param message SEVERE message to log
     */
    public void logError(String message) {
        logger.log(Level.SEVERE, message);
    }

    /**
     * Log a FINE message.
     * @param message FINE message to log
     */
    public void logVerbose(String message) {
        logger.log(Level.FINE, message);
    }

    /**
     * Log a FINEST message.
     * @param message FINEST message to log
     */
    public void logDebug(String message) {
        logger.log(Level.FINEST, message);
    }

    /**
     * Execute a nested command.
     * @param args raw arguments
     */
    public void execute(String... args) {
        CommandContext context = new CommandContext(this);
        CommandRunner.execute(context, args);
        this.exitAction = context.exitAction;
    }

    /**
     * Set the error message if not already set.
     * @param status exit status
     * @param message error message
     */
    public void exitAction(ExitStatus status, String message) {
        if (status.isWorse(exitAction.status)) {
            exitAction = new ExitAction(status, message);
        }
    }

    /**
     * Get the exit action.
     * @return exit action, never {@code null}
     */
    public ExitAction exitAction() {
        return exitAction;
    }

    /**
     * Set the command parser.
     * @param parser parser
     */
    void parser(CommandParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser is null");
    }

    /**
     * Get the command parser.
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
     * Enable verbose mode.
     * @param verbose verbose value
     */
    void verbosity(Verbosity verbosity) {
        this.logHandler.verbosity = verbosity;
    }

    /**
     * Set the exit action to {@link ExitStatus#FAILURE} and display an error message.
     *
     * @param command command name
     */
    void commandNotFoundError(String command) {
        List<String> allCommandNames = registry.commandsByName().values()
                .stream().map(CommandModel::command).map(CommandInfo::name).collect(Collectors.toList());
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
     * @param message error message
     */
    void error(String message) {
        exitAction(ExitStatus.FAILURE, message);
    }

    /**
     * Create a new command context.
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
    enum Verbosity {
        NORMAL,
        VERBOSE,
        DEBUG
    }

    /**
     * Custom log handler to print the message to {@code stdout} and {@code stderr}.
     */
    private static final class LogHandler extends Handler {

        private Verbosity verbosity = Verbosity.NORMAL;

        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            if (level == Level.INFO) {
                System.out.println(record.getMessage());
            } else if (level == Level.WARNING || level == Level.SEVERE) {
                System.err.println(record.getMessage());
            } else if ((level == Level.CONFIG || level == Level.FINE)
                    && (verbosity == Verbosity.VERBOSE || verbosity == Verbosity.DEBUG)) {
                System.out.println(record.getMessage());
            } else if (verbosity == Verbosity.DEBUG) {
                System.out.println(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
