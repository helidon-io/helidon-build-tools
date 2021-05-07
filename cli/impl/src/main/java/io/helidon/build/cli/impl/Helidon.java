/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import io.helidon.build.cli.harness.CommandLineInterface;
import io.helidon.build.cli.harness.CommandRunner;

/**
 * Helidon CLI definition and entry-point.
 */
@CommandLineInterface(
        name = "helidon",
        description = "Helidon Project command line tool",
        commands = {
                BuildCommand.class,
                DevCommand.class,
                InfoCommand.class,
                InitCommand.class,
                VersionCommand.class
        })
public final class Helidon {

    private Helidon() {
    }

    /**
     * Execute the command.
     *
     * @param args raw command line arguments
     */
    public static void main(String[] args) {
        CommandRunner.builder()
                     .args(args)
                     .optionLookup(Config.userConfig()::property)
                     .cliClass(Helidon.class)
                     .build()
                     .initProxy()
                     .execute()
                     .runExitAction();
    }
}
