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

import io.helidon.build.cli.harness.CommandModel.FlagInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValuesInfo;
import io.helidon.build.cli.harness.CommandParameters.CommandFragmentInfo;
import io.helidon.build.cli.harness.CommandParameters.ParameterInfo;
import io.helidon.build.cli.harness.CommandParser.Resolver;

/**
 * Command fragment info (generated).
 */
public final class TestOptionsInfo extends CommandFragmentInfo<TestOptions> {

    /**
     * Parameters.
     */
    public static final ParameterInfo[] PARAMS = new ParameterInfo[]{
            new KeyValueInfo<>(
                    String.class,
                    "name",
                    "The name",
                    null,
                    false,
                    true),
            new KeyValuesInfo<>(
                    String.class,
                    "talents",
                    "The talents",
                    false),
            new FlagInfo(
                    "record",
                    "Record flag",
                    true),
            new FlagInfo(
                    "export",
                    "Export flag",
                    false)
    };

    /**
     * Singleton instance.
     */
    public static final TestOptionsInfo INSTANCE = new TestOptionsInfo();

    private TestOptionsInfo() {
        super(TestOptions.class, PARAMS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TestOptions resolve(Resolver resolver) {
        return new TestOptions(
                resolver.resolve((KeyValueInfo<String>) PARAMS[0]),
                resolver.resolve((KeyValuesInfo<String>) PARAMS[1]),
                resolver.resolve((FlagInfo) PARAMS[2]),
                resolver.resolve((FlagInfo) PARAMS[3]));
    }
}
