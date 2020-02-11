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

import java.util.Collection;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValues;

/**
 * The {@code features} command.
 */
@Command(name = "features", description = "List or add features to the project")
public final class FeaturesCommand implements CommandExecution {

    private final CommonOptions commonOptions;
    private final Collection<String> add;
    private final boolean list;
    private final boolean all;

    @Creator
    FeaturesCommand(
            CommonOptions commonOptions,
            @KeyValues(name = "add", description = "Add features to the project") Collection<String> add,
            @Flag(name = "list", description = "List the features used in the project") boolean list,
            @Flag(name = "all", description = "List all available features") boolean all) {

        this.commonOptions = commonOptions;
        this.add = add;
        this.list = list;
        this.all = all;
    }

    @Override
    public void execute(CommandContext context) {
        context.logInfo(String.format("%n// TODO exec features, project=%s, add=%s, list=%s, all=%s",
                commonOptions.project(), add, String.valueOf(list), String.valueOf(all)));
    }
}
