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

import io.helidon.build.util.Log;

import static io.helidon.build.util.StyleFunction.Bold;
import static io.helidon.build.util.StyleFunction.BoldBlue;
import static io.helidon.build.util.StyleFunction.Italic;

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
        private Map<String, String> options;
        private Map<String, String> required;
        private StringBuilder requiredOptions;

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

        private void doExecute(CommandContext context, CommandModel model) {
            StringBuilder usage = new StringBuilder();
            requiredOptions = new StringBuilder();
            required = new LinkedHashMap<>();
            options = new LinkedHashMap<>(UsageCommand.GLOBAL_OPTIONS);
            String argument = "";
            for (ParameterInfo<?> param : model.parameters()) {
                if (!param.visible()) {
                    continue;
                }
                if (param instanceof ArgumentInfo) {
                    argument = ((ArgumentInfo<?>) param).usage();
                }
                if (param instanceof NamedOptionInfo) {
                    appendOption((NamedOptionInfo<?>) param);
                } else if (param instanceof CommandFragmentInfo) {
                    for (ParameterInfo<?> fragmentParam : ((CommandFragmentInfo<?>) param).parameters()) {
                        if (fragmentParam.visible()) {
                            if (fragmentParam instanceof NamedOptionInfo) {
                                appendOption((NamedOptionInfo<?>) fragmentParam);
                            } else if (fragmentParam instanceof ArgumentInfo) {
                                argument = (((ArgumentInfo<?>) fragmentParam).usage());
                            }
                        }
                    }
                }
            }
            if (!argument.isEmpty()) {
                usage.append(' ').append(Italic.apply(argument));
            }
            String styledName = BoldBlue.apply(context.cliName() + " " + model.command().name());
            Log.info("%n%s%n", Bold.apply(model.command().description()));
            Log.info(String.format("Usage: %s %s[%s]%s", styledName, requiredOptions, Italic.apply("OPTIONS"), usage));
            int maxKeyWidth = OutputHelper.maxKeyWidth(required, options);
            if (!required.isEmpty()) {
                Log.info("\nRequired\n");
                Log.info(OutputHelper.table(required, maxKeyWidth));
            }
            Log.info("\nOptions\n");
            Log.info(OutputHelper.table(options, maxKeyWidth));
        }

        private void appendOption(NamedOptionInfo<?> option) {
            if (option instanceof RequiredOption && ((RequiredOption) option).required()) {
                requiredOptions.append(option.syntax().replace(" | ", "|")).append(' ');
                required.put(option.syntax(), optionDescription(option));
            } else {
                options.put(option.syntax(), optionDescription(option));
            }
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
    }
}
