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

import io.helidon.build.common.Log;

import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;

/**
 * Built-in usage command.
 */
final class UsageCommand extends CommandModel {

    static final String NAME = "";
    static final Map<String, String> GLOBAL_OPTIONS = createGlobalOptionsMap();

    UsageCommand() {
        super(new CommandInfo(NAME, ""));
    }

    @Override
    boolean visible() {
        return false;
    }

    @Override
    public CommandExecution createExecution(CommandParser.Resolver resolver) {
        return this::execute;
    }

    private static Map<String, String> createGlobalOptionsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("-D<name>=<value>", "Define a system property");
        map.put(GlobalOptions.VERBOSE_FLAG_ARGUMENT, GlobalOptions.VERBOSE_FLAG_DESCRIPTION);
        map.put(GlobalOptions.DEBUG_FLAG_ARGUMENT, GlobalOptions.DEBUG_FLAG_DESCRIPTION);
        map.put(GlobalOptions.PLAIN_FLAG_ARGUMENT, GlobalOptions.PLAIN_FLAG_DESCRIPTION);
        return map;
    }

    private void execute(CommandContext context) {
        Map<String, String> commands = new LinkedHashMap<>();
        for (CommandModel cmdModel : context.allCommands()) {
            CommandInfo cmdInfo = cmdModel.command();
            commands.put(cmdInfo.name(), cmdInfo.description());
        }
        int maxKeyWidth = OutputHelper.maxKeyWidth(GLOBAL_OPTIONS, commands);
        String styledName = BoldBlue.apply(context.cliName());
        String styledCommand = Italic.apply("COMMAND");
        String styledInfo = BoldBlue.apply(context.cliName()) + " " + styledCommand + " " + BoldBlue.apply("--help");
        Log.info("%n%s%n", Bold.apply(context.cliDescription()));
        Log.info("Usage: %s [OPTIONS] %s%n", styledName, styledCommand);
        Log.info("Options\n");
        Log.info(OutputHelper.table(GLOBAL_OPTIONS, maxKeyWidth));
        Log.info("\nCommands\n");
        if (!commands.isEmpty()) {
            Log.info(OutputHelper.table(commands, maxKeyWidth));
        }
        Log.info("%nRun %s for more information on a command.", styledInfo);
    }
}
