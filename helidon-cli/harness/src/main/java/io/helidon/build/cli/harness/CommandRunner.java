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

import java.util.Objects;

import io.helidon.build.cli.harness.CommandParser.CommandParserException;
import io.helidon.build.util.AnsiConsoleInstaller;
import io.helidon.build.util.Proxies;

import static io.helidon.build.cli.harness.Config.userConfig;

/**
 * Command runner.
 */
public final class CommandRunner {

    private final CommandParser parser;
    private final CommandContext context;

    private CommandRunner(CommandContext context, String[] args) {
        this.context = Objects.requireNonNull(context, "context is null");
        this.parser = CommandParser.create(args == null ? new String[0] : args);
        this.context.parser(parser);
        this.context.properties().putAll(parser.globalResolver().properties());
    }

    /**
     * Execute the command.
     */
    public void execute() {
        parser.error().ifPresentOrElse(context::error, this::doExecute);
    }

    /**
     * Execute the current command from the parser.
     */
    private void doExecute() {
        parser.commandName().ifPresentOrElse(this::doExecuteCommandName, this::printUsage);
    }

    /**
     * Execute the command represented by the given name.
     *
     * @param command command name
     */
    private void doExecuteCommandName(String command) {
        context.command(command).map(this::mapHelp)
                .ifPresentOrElse(this::doExecuteCommand, () -> context.commandNotFoundError(command));
    }

    /**
     * Execute the given {@link CommandModel} instance.
     *
     * @param command command to execute
     */
    private void doExecuteCommand(CommandModel command) {
        CommandParser.Resolver globalResolver = parser.globalResolver();
        if (globalResolver.resolve(GlobalOptions.PLAIN_FLAG_INFO) || userConfig().richTextDisabled()) {
            AnsiConsoleInstaller.disable();
        } else {
            AnsiConsoleInstaller.install();
        }
        if (globalResolver.resolve(GlobalOptions.VERBOSE_FLAG_INFO)) {
            context.verbosity(CommandContext.Verbosity.VERBOSE);
        } else if (globalResolver.resolve(GlobalOptions.DEBUG_FLAG_INFO)) {
            context.verbosity(CommandContext.Verbosity.DEBUG);
        } else {
            context.verbosity(CommandContext.Verbosity.NORMAL);
        }
        try {
            CommandParser.Resolver resolver = parser.parseCommand(command);
            context.properties().putAll(resolver.properties());
            context.properties().forEach((key, value) -> System.setProperty((String) key, (String) value));
            command.createExecution(resolver).execute(context);
        } catch (CommandParserException ex) {
            context.error("%s%nSee '%s %s --help'", ex.getMessage(), context.cli().name(), command.command().name());
        } catch (Throwable t) {
            context.error(t);
        }
    }

    /**
     * Resolve the {@code --help} option and return the {@code help} command if found.
     *
     * @param command fallback command
     * @return {@code help} command if the {@code --help} option is provided, otherwise the supplied fallback command
     */
    private CommandModel mapHelp(CommandModel command) {
        return parser.globalResolver().resolve(GlobalOptions.HELP_FLAG_INFO) ? new HelpCommand() : command;
    }

    /**
     * No command provided, print the usage.
     */
    private void printUsage() {
        doExecuteCommand(new UsageCommand());
    }

    /**
     * Execute a sub-command.
     *
     * @param context command context
     * @param args    raw command line arguments
     */
    static void execute(CommandContext context, String... args) {
        new CommandRunner(context, args).execute();
    }

    /**
     * Execute a command.
     *
     * @param cli   CLI definition
     * @param clazz class used to derive the package of the sub-commands
     * @param args  raw command line arguments
     */
    public static void execute(CLIDefinition cli, Class<?> clazz, String... args) {
        Proxies.setProxyPropertiesFromEnv();
        CommandRegistry registry = CommandRegistry.load(clazz);
        CommandContext context = CommandContext.create(registry, cli);
        CommandRunner.execute(context, args);
        context.exitAction().run();
    }
}
