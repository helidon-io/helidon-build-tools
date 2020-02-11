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
    public CommandExecution createExecution(CommandParser parser) {
        return this::execute;
    }

    private static Map<String, String> createGlobalOptionsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("-D<name>=<value>", "Define a system property");
        map.put("--verbose", CommandModel.VERBOSE_OPTION.description());
        map.put("--debug", CommandModel.DEBUG_OPTION.description());
        return map;
    }

    private void execute(CommandContext context) {
        context.logInfo(String.format("%nUsage:\t%s [OPTIONS] COMMAND%n", context.cli().name()));
        context.logInfo(context.cli().description());
        context.logInfo("\nOptions:");
        context.logInfo(OutputHelper.table(GLOBAL_OPTIONS));
        context.logInfo("\nCommands:");
        Map<String, String> commands = new LinkedHashMap<>();
        for (CommandModel cmdModel : context.allCommands()) {
            CommandInfo cmdInfo = cmdModel.command();
            commands.put(cmdInfo.name(), cmdInfo.description());
        }
        if (!commands.isEmpty()) {
            context.logInfo(OutputHelper.table(commands));
        }
        context.logInfo(String.format("%nRun '%s COMMAND --help' for more information on a command.", context.cli().name()));
    }
}
