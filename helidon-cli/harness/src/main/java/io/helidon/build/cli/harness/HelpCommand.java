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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.helidon.build.util.Log;

/**
 * The {@code help} command.
 */
class HelpCommand extends CommandModel {

    private final ArgumentInfo<String> commandArg;

    HelpCommand() {
        super(new CommandInfo("help", "Help about the command"));
        commandArg = new ArgumentInfo<>(String.class, "command", false);
        addParameter(commandArg);
    }

    @Override
    boolean visible() {
        return false;
    }

    @Override
    public final CommandExecution createExecution(CommandParser parser) {
        return new HelpCommandExecution(parser);
    }

    private final class HelpCommandExecution implements CommandExecution {

        private final CommandParser parser;

        HelpCommandExecution(CommandParser parser) {
            this.parser = parser;
        }

        private Optional<String> commandName() {
            return Optional.ofNullable(parser.resolve(commandArg))
                // if the help command is forced because of --help, the actual command arg is the original command name
                .or(() -> parser.commandName().map((command) -> "help".equals(command) ? null : command))
                // if --help is found at this point, this is help about the help command
                .or(() -> Optional.ofNullable(parser.resolve(GlobalOptions.HELP_FLAG_INFO) ? "help" : null));
        }

        @Override
        public void execute(CommandContext context) {
            commandName().ifPresentOrElse(
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
            if (option instanceof KeyValueInfo && !((KeyValueInfo) option).required()) {
                Object defaultValue = ((KeyValueInfo<?>) option).defaultValue();
                if (defaultValue != null) {
                    desc += " (default: " + defaultValue + ")";
                }
            }
            return desc;
        }

        private void doExecute(CommandContext context, CommandModel model) {
            Map<String, String> options = new LinkedHashMap<>();
            options.putAll(UsageCommand.GLOBAL_OPTIONS);
            String usage = "";
            String argument = "";
            for (ParameterInfo<?> param : model.parameters()) {
                if (!param.visible()) {
                    continue;
                }
                if (!usage.isEmpty()) {
                    usage += " ";
                }
                if (param instanceof ArgumentInfo) {
                    argument = ((ArgumentInfo) param).usage();
                } else if (param instanceof OptionInfo) {
                    usage += ((OptionInfo) param).usage();
                }
                if (param instanceof NamedOptionInfo) {
                    NamedOptionInfo<?> option = (NamedOptionInfo<?>) param;
                    options.put("--" + option.name(), optionDescription(option));
                } else if (param instanceof CommandFragmentInfo) {
                    for (ParameterInfo<?> fragmentParam : ((CommandFragmentInfo) param).parameters()) {
                        if (fragmentParam.visible()) {
                            if (fragmentParam instanceof NamedOptionInfo) {
                                NamedOptionInfo<?> fragmentOption = (NamedOptionInfo<?>) fragmentParam;
                                usage += fragmentOption.usage();
                                options.put("--" + fragmentOption.name(), optionDescription(fragmentOption));
                            }
                        }
                    }
                }
            }
            if (!argument.isEmpty()) {
                usage += (usage.isEmpty() ? argument : (" " + argument));
            }
            Log.info(String.format("%nUsage:\t%s %s [OPTIONS] %s%n", context.cli().name(), model.command().name(), usage));
            Log.info(model.command().description());
            Log.info("\nOptions:");
            Log.info(OutputHelper.table(options));
        }
    }
}
