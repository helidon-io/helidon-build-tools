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
package io.helidon.build.cli.impl;

import io.helidon.build.cli.harness.CLIDefinition;
import io.helidon.build.cli.harness.CommandRunner;

/**
 * Main entry point for the CLI.
 */
public final class Main {

    private static final CLIDefinition CLI_DEFINITION = CLIDefinition.create("helidon", "Helidon Project command line tool");

    private Main() {
    }

    /**
     * Execute the command.
     * @param args raw command line arguments
     */
    public static void main(String[] args) {
        CommandRunner.execute(CLI_DEFINITION, Main.class, args);
    }
}
