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

import io.helidon.build.util.Log;

import static io.helidon.build.util.StyleFunction.Bold;

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
        String styledName = Bold.apply(context.cli().name());
        Log.info(String.format("%nUsage:\t%s [OPTIONS] COMMAND%n", styledName));
        Log.info(context.cli().description());
        Log.info("\nOptions:");
        Log.info(OutputHelper.table(GLOBAL_OPTIONS));
        Log.info("\nCommands:");
        Map<String, String> commands = new LinkedHashMap<>();
        for (CommandModel cmdModel : context.allCommands()) {
            CommandInfo cmdInfo = cmdModel.command();
            commands.put(cmdInfo.name(), cmdInfo.description());
        }
        if (!commands.isEmpty()) {
            Log.info(OutputHelper.table(commands));
        }
        Log.info(String.format("%nRun '%s COMMAND --help' for more information on a command.", context.cli().name()));
    }
}
