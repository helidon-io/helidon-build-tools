/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.helidon.build.cli.harness.CommandModel.CommandInfo;
import io.helidon.build.common.Requirements;
import io.helidon.build.common.ansi.AnsiTextStyle;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static io.helidon.build.common.ansi.AnsiTextStyles.Red;
import static io.helidon.build.common.logging.LogLevel.ERROR;
import static io.helidon.build.common.logging.LogLevel.INFO;
import static io.helidon.build.common.logging.LogLevel.VERBOSE;
import static io.helidon.build.common.logging.LogLevel.WARN;

/**
 * The command context.
 */
public final class CommandContext {

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
     * Command context internal options.
     */
    public static final class InternalOptions {

        private static final InternalOptions EMPTY = new InternalOptions(null);

        private final BiFunction<String, String, String> lookup;

        /**
         * Create a new instance.
         *
         * @param lookup function used to look up the internal options
         */
        public InternalOptions(BiFunction<String, String, String> lookup) {
            this.lookup = lookup != null ? lookup : (v1, v2) -> v2;
        }

        /**
         * Rich text option.
         */
        public static final String RICH_TEXT_KEY = "use.rich.text";

        /**
         * Rich text option default value.
         */
        public static final String RICH_TEXT_DEFAULT_VALUE = "true";

        /**
         * Returns whether rich text should be disabled.
         *
         * @return {@code true} if rich text should not be used (equivalent to {@code --plain} option).
         */
        public boolean richTextDisabled() {
            return !Boolean.parseBoolean(lookup.apply(RICH_TEXT_KEY, RICH_TEXT_DEFAULT_VALUE));
        }
    }

    private final CommandRegistry registry;
    private final Properties properties;
    private final InternalOptions internalOptions;
    private final boolean embedded;
    private Verbosity verbosity;
    private ExitAction exitAction;
    private CommandParser parser;

    @SuppressWarnings("CopyConstructorMissesField")
    CommandContext(CommandContext parent) {
        this(parent.registry, parent.internalOptions, parent.embedded);
    }

    CommandContext(CommandRegistry registry, InternalOptions internalOptions, boolean embedded) {
        this.registry = Objects.requireNonNull(registry, "registry is null");
        this.exitAction = new ExitAction();
        this.properties = new Properties();
        this.internalOptions = internalOptions != null ? internalOptions : InternalOptions.EMPTY;
        this.embedded = embedded;
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
        @SuppressWarnings("unused")
        public Throwable failure() {
            return failure;
        }

        /**
         * Run the exit sequence for this action.
         * <b>WARNING:</b> This method invokes {@link System#exit(int)}.
         */
        @SuppressFBWarnings
        public void runExitAction() {
            switch (exitAction.status) {
                case FAILURE:
                    if (failure != null) {
                        Requirements.toFailure(failure).ifPresentOrElse(ce -> {
                            if (AnsiTextStyle.isStyled(ce.getMessage())) {
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
                    exit(null, null, INFO, 0);
            }
        }

        private void exit(String message, Throwable error, LogLevel level, int statusCode) {
            String errorMessage = errorMessage(message, error);
            if (errorMessage != null) {
                if (LogLevel.isVerbose()) {
                    Log.info();
                }
                if (AnsiTextStyle.isStyled(message)) {
                    Log.info(message);
                } else {
                    Log.log(level, error, message);
                }
                if (LogLevel.isVerbose()) {
                    Log.info();
                }
            }
            if (embedded) {
                if (statusCode != 0) {
                    throw new Error(AnsiTextStyle.strip(message), error);
                }
            } else {
                System.exit(statusCode);
            }
        }

        private String errorMessage(String message, Throwable error) {
            if (message == null) {
                if (error != null) {
                    if (error.getMessage() != null) {
                        message = error.getMessage();
                    } else if (error.getCause() != null) {
                        message = error.getCause().getMessage();
                    } else {
                        message = "Unknown error";
                    }
                }
            }
            return message;
        }
    }

    /**
     * Get the CLI name.
     *
     * @return CLI name, never {@code null}
     */
    public String cliName() {
        return registry.cliName();
    }

    /**
     * Get the CLI description.
     *
     * @return CLI description, never {@code null}
     */
    public String cliDescription() {
        return registry.cliDescription();
    }

    /**
     * Get the internal options.
     *
     * @return InternalOptions, never {@code null}
     */
    public InternalOptions internalOptions() {
        return internalOptions;
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
        new CommandRunner(context, args).execute();
        this.exitAction = context.exitAction;
    }

    /**
     * Set the error message if not already set.
     *
     * @param status  exit status
     * @param message error message
     * @return exit action.
     */
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
        this.properties.putAll(parser.globalResolver().properties());
    }

    /**
     * Get the command parser.
     *
     * @return parser
     * @throws IllegalStateException if the parser is not set
     */
    CommandParser parser() {
        if (parser == null) {
            throw new IllegalStateException("parser is not set");
        }
        return parser;
    }

    /**
     * Get the parsed properties.
     *
     * @return properties, never {@code null}
     */
    public Properties properties() {
        return properties;
    }

    /**
     * Returns the parsed properties as a list of {@code -Dkey=value} arguments.
     *
     * @param mapEmptyValuesToTrue If {@code true}, any empty value will be mapped to {@code "true"}.
     * @return The arguments.
     */
    public List<String> propertyArgs(boolean mapEmptyValuesToTrue) {
        return properties.entrySet()
                         .stream()
                         .map(e -> {
                             String value = e.getValue().toString();
                             if (mapEmptyValuesToTrue && value.isEmpty()) {
                                 value = "true";
                             }
                             return "-D" + e.getKey() + "=" + value;
                         })
                         .collect(Collectors.toList());
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
        LogLevel level;
        switch (verbosity) {
            case DEBUG: {
                level = LogLevel.DEBUG;
                break;
            }
            case VERBOSE: {
                level = VERBOSE;
                break;
            }
            case NORMAL: {
                level = (INFO);
                break;
            }
            default: {
                throw new RuntimeException("unknown verbosity: " + verbosity);
            }
        }
        LogLevel.set(level);
        System.setProperty(LogLevel.LEVEL_PROPERTY, level.name());
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
        if (match != null) {
            error(String.format("'%s' is not a valid command.%nDid you mean '%s'?%nSee '%s --help' for more information",
                    command, match, cliName()));
        } else {
            error(String.format("'%s' is not a valid command.%nSee '%s --help' for more information", command, cliName()));
        }
    }

    /**
     * Set the exit action to {@link ExitStatus#FAILURE} with the given error message.
     *
     * @param message error message
     * @param args    message args.
     */
    void error(String message, Object... args) {
        exitAction = new ExitAction(ExitStatus.FAILURE, String.format(message, args));
    }

    /**
     * Set the exit action to {@link ExitStatus#FAILURE} with the given error.
     *
     * @param error error
     */
    void error(Throwable error) {
        exitAction = new ExitAction(error);
    }

    /**
     * CommandContext builder.
     *
     * @param <T> builder subclass type
     */
    public abstract static class Builder<T extends Builder<T>> {

        private boolean embedded;
        private Class<?> cliClass;
        private BiFunction<String, String, String> lookup;

        Builder() {
        }

        /**
         * Set the internal option lookup.
         *
         * @param lookup function used to lookup internal options
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public T optionLookup(BiFunction<String, String, String> lookup) {
            this.lookup = lookup;
            return (T) this;
        }

        /**
         * Set the cli class.
         *
         * @param cliClass class
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public T cliClass(Class<?> cliClass) {
            this.cliClass = cliClass;
            return (T) this;
        }

        /**
         * Set embedded mode.
         *
         * @param embedded {@code true} if embedded mode
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public T embedded(boolean embedded) {
            this.embedded = embedded;
            return (T) this;
        }
        /**
         * Build the command context instance.
         *
         * @return CommandContext
         */
        protected CommandContext buildContext() {
            CommandRegistry registry = CommandRegistry.load(cliClass);
            return new CommandContext(registry, new InternalOptions(lookup), embedded);
        }
    }
}
