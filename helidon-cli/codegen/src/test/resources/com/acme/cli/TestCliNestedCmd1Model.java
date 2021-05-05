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

import com.acme.cli.TestCliNested.Cmd1;
import io.helidon.build.cli.harness.CommandModel;
import io.helidon.build.cli.harness.CommandModel.CommandInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.cli.harness.CommandParameters.ParameterInfo;
import io.helidon.build.cli.harness.CommandParser.Resolver;

/**
 * Command model (generated).
 */
public final class TestCliNestedCmd1Model extends CommandModel {

    /**
     * Parameters.
     */
    public static final ParameterInfo[] PARAMS = new ParameterInfo[]{
            new KeyValueInfo<>(
                    String.class,
                    "foo",
                    "The foo",
                    null,
                    false,
                    true)
    };

    /**
     * Singleton instance.
     */
    public static final TestCliNestedCmd1Model INSTANCE = new TestCliNestedCmd1Model();

    /**
     * Command name.
     */
    public static final String NAME = "cmd1";

    /**
     * Command description.
     */
    public static final String DESCRIPTION = "the first";

    private TestCliNestedCmd1Model() {
        super(new CommandInfo(NAME, DESCRIPTION), PARAMS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Cmd1 createExecution(Resolver resolver) {
        return new Cmd1(
                resolver.resolve((KeyValueInfo<String>) PARAMS[0]));
    }
}
