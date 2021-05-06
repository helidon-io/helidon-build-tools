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

import io.helidon.build.cli.harness.CommandModel;
import io.helidon.build.cli.harness.CommandRegistry;

/**
 * Command registry for TestCli1 (generated).
 */
public final class TestCli1Registry extends CommandRegistry {

    private static final String CLI_CLASS = TestCli1.class.getName();
    private static final String CLI_NAME = "test1";
    private static final String CLI_DESCRIPTION = "Test cli #1";
    private static final CommandModel[] COMMANDS = new CommandModel[]{
            TestCommand1Model.INSTANCE,
            TestCommand2Model.INSTANCE
    };

    /**
     * Create a new instance.
     */
    public TestCli1Registry() {
        super(CLI_CLASS, CLI_NAME, CLI_DESCRIPTION);
        register(COMMANDS);
    }
}
