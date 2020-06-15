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
import java.util.stream.Collectors;

import io.helidon.build.cli.harness.CommandModel.CommandInfo;
import io.helidon.build.util.Log;
import io.helidon.build.util.Log.Level;
import io.helidon.build.util.Requirements;
import io.helidon.build.util.Style;
import io.helidon.build.util.SystemLogWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static io.helidon.build.util.Log.Level.DEBUG;
import static io.helidon.build.util.Log.Level.ERROR;
import static io.helidon.build.util.Log.Level.INFO;
import static io.helidon.build.util.Log.Level.VERBOSE;
import static io.helidon.build.util.Log.Level.WARN;
import static io.helidon.build.util.Style.Red;

/**
 * The command context.
 */
public final class CommandContext {
    private static final SystemLogWriter LOG_WRITER = SystemLogWriter.install(INFO);
    private final CLIDefinition cli;
    private final CommandRegistry registry;
    private Verbosity verbosity;
    private ExitAction exitAction;
    private CommandParser parser;

    CommandContext(CommandContext parent) {
        this(parent.registry, parent.cli);
    }

    private CommandContext(CommandRegistry registry, CLIDefinition cli) {
        this.cli = Objects.requireNonNull(cli, "cli is null");
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
                            if (Style.isStyled(ce.getMessage())) {
                                exit(ce.getMessage(), null, INFO, 1);
                            } else {
                                exit(Red.apply(ce.getMessage()), null, INFO, 1);
                            }
                        }, () -> exit(failure.getMessage(), failure, ERROR, 1));
                    } else {
                        exit(message, null, ERROR, 1);
                    }
                    break;
                case WARNING:
                    exit(message, null, WARN, 0);
                    break;
                default:
                    System.exit(0);
            }
        }

        private void exit(String message, Throwable error, Level level, int statusCode) {
            if (message != null || error != null) {
                if (Log.isVerbose()) {
                    Log.info();
                }
                if (Style.isStyled(message)) {
                    Log.info(message);
                } else {
                    if (message == null) {
                        if (error.getMessage() != null) {
                            message = error.getMessage();
                        } else if (error.getCause() != null) {
                            message = error.getCause().getMessage();
                        } else {
                            message = "Unknown error";
                            error.printStackTrace();
                        }
                    }
                    Log.log(level, error, message);
                }
                if (Log.isVerbose()) {
                    Log.info();
                }
            }
            System.exit(statusCode);
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
        return verbosity;
    }

    /**
     * Enable verbose mode.
     *
     * @param verbosity verbosity value
     */
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    void verbosity(Verbosity verbosity) {
        this.verbosity = verbosity;
        switch (verbosity) {
            case DEBUG: {
                LOG_WRITER.level(DEBUG);
                break;
            }
            case VERBOSE: {
                LOG_WRITER.level(VERBOSE);
                break;
            }
            case NORMAL: {
                LOG_WRITER.level(INFO);
                break;
            }
            default: {
                throw new RuntimeException("unknown verbosity: " + verbosity);
            }
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
}
