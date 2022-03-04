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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.helidon.build.common.Log;

/**
 * The {@code help} command.
 */
class HelpCommand extends CommandModel {

    private static final CommandInfo CMD_INFO = new CommandInfo("help", "Help about the command");
    private static final ArgumentInfo<String> ARG_INFO = new ArgumentInfo<>(String.class, "command", false);

    HelpCommand() {
        super(CMD_INFO, ARG_INFO);
    }

    @Override
    boolean visible() {
        return false;
    }

    @Override
    public final CommandExecution createExecution(CommandParser.Resolver resolver) {
        return new HelpCommandExecution(resolver);
    }

    private static final class HelpCommandExecution implements CommandExecution {

        private final CommandParser.Resolver resolver;

        HelpCommandExecution(CommandParser.Resolver resolver) {
            this.resolver = resolver;
        }

        private Optional<String> commandName(CommandContext context) {
            return Optional.ofNullable(resolver.resolve(ARG_INFO))
                // if the help command is forced because of --help, the actual command arg is the original command name
                .or(() -> context.parser().commandName().map((command) -> "help".equals(command) ? null : command))
                // if --help is found at this point, this is help about the help command
                .or(() -> Optional.ofNullable(resolver.resolve(GlobalOptions.HELP_FLAG_INFO) ? "help" : null));
        }

        @Override
        public void execute(CommandContext context) {
            commandName(context).ifPresentOrElse(
                    // execute
                    (commandName) -> this.doExecute(context, commandName),
                    // just help, print usage
                    context::execute);
        }

        private void doExecute(CommandContext context, String commandName) {
            context.command(commandName).ifPresentOrElse(
                    // execute
                    (command) -> this.doExecute(context, command),
                    // command name is not found
                    () -> context.commandNotFoundError(commandName));
        }

        private String optionDescription(NamedOptionInfo<?> option) {
            String desc = option.description();
            if (option instanceof KeyValueInfo && !((KeyValueInfo<?>) option).required()) {
                Object defaultValue = ((KeyValueInfo<?>) option).defaultValue();
                if (defaultValue != null) {
                    desc += " (default: " + defaultValue + ")";
                }
            }
            return desc;
        }

        private void doExecute(CommandContext context, CommandModel model) {
            Map<String, String> options = new LinkedHashMap<>(UsageCommand.GLOBAL_OPTIONS);
            StringBuilder usage = new StringBuilder();
            String argument = "";
            for (ParameterInfo<?> param : model.parameters()) {
                if (!param.visible()) {
                    continue;
                }
                if (usage.length() > 0) {
                    usage.append(" ");
                }
                if (param instanceof ArgumentInfo) {
                    argument = ((ArgumentInfo<?>) param).usage();
                } else if (param instanceof OptionInfo) {
                    usage.append(((OptionInfo<?>) param).usage());
                }
                if (param instanceof NamedOptionInfo) {
                    NamedOptionInfo<?> option = (NamedOptionInfo<?>) param;
                    options.put("--" + option.name(), optionDescription(option));
                } else if (param instanceof CommandFragmentInfo) {
                    for (ParameterInfo<?> fragmentParam : ((CommandFragmentInfo<?>) param).parameters()) {
                        if (fragmentParam.visible()) {
                            if (fragmentParam instanceof NamedOptionInfo) {
                                NamedOptionInfo<?> fragmentOption = (NamedOptionInfo<?>) fragmentParam;
                                appendUsage(usage, fragmentOption.usage());
                                options.put("--" + fragmentOption.name(), optionDescription(fragmentOption));
                            } else if (fragmentParam instanceof ArgumentInfo) {
                                ArgumentInfo<?> fragmentArgument = (ArgumentInfo<?>) fragmentParam;
                                appendUsage(usage, fragmentArgument.usage());
                            }
                        }
                    }
                }
            }
            if (!argument.isEmpty()) {
                usage.append((usage.length() == 0) ? argument : (" " + argument));
            }
            Log.info(String.format("%nUsage:\t%s %s [OPTIONS] %s%n", context.cliName(), model.command().name(), usage));
            Log.info(model.command().description());
            Log.info("\nOptions:");
            Log.info(OutputHelper.table(options));
        }
    }

    private static void appendUsage(StringBuilder usage, String message) {
        int length = usage.length();
        if (length > 0 && usage.charAt(length - 1) != ' ') {
            usage.append(' ');
        }
        usage.append(message);
    }
}
