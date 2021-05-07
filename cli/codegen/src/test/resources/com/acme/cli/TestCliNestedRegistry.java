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
 * Command registry for TestCliNested (generated).
 */
public final class TestCliNestedRegistry extends CommandRegistry {

    private static final String CLI_CLASS = TestCliNested.class.getName();
    private static final String CLI_NAME = "test-nested";
    private static final String CLI_DESCRIPTION = "Test cli with nested command classes";
    private static final CommandModel[] COMMANDS = new CommandModel[]{
            TestCliNestedCmd1Model.INSTANCE,
            TestCliNestedCmd2Model.INSTANCE
    };

    /**
     * Create a new instance.
     */
    public TestCliNestedRegistry() {
        super(CLI_CLASS, CLI_NAME, CLI_DESCRIPTION);
        register(COMMANDS);
    }
}
