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
package com.acme.cli;

import io.helidon.build.cli.harness.*;

@CommandLineInterface(
        name = "test-nested",
        description = "Test cli with nested command classes",
        commands = {
                TestCliNested.Cmd1.class,
                TestCliNested.Cmd2.class,
        })
class TestCliNested {

    public static void main(String[] args) {
    }

    @Command(name = "cmd1", description = "the first")
    static class Cmd1 implements CommandExecution {

        @Creator
        Cmd1(@Option.KeyValue(name = "foo", description = "The foo") String foo) {
        }

        @Override
        public void execute(CommandContext context) throws Exception {
        }
    }

    @Command(name = "cmd2", description = "the second")
    static class Cmd2 implements CommandExecution {

        @Creator
        Cmd2(@Option.KeyValue(name = "bar", description = "The bar") String bar) {
        }

        @Override
        public void execute(CommandContext context) throws Exception {
        }
    }
}