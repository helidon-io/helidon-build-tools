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

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;

/**
 * The {@code init} command.
 */
@Command(name = "init", description = "Generate a new project")
public final class InitCommand implements CommandExecution {

    private final CommonOptions commonOptions;
    private final boolean batch;
    private final Flavor flavor;
    private final Build build;

    /**
     * Helidon flavors.
     */
    enum Flavor {
        MP,
        SE
    }

    /**
     * Build systems.
     */
    enum Build {
        MAVEN,
        GRADLE,
    }

    @Creator
    InitCommand(
            CommonOptions commonOptions,
            @KeyValue(name = "flavor", description = "Helidon flavor", defaultValue = "SE") Flavor flavor,
            @KeyValue(name = "build", description = "Build type", defaultValue = "MAVEN") Build build,
            @Flag(name = "batch", description = "Non interactive, user input is passed as system properties") boolean batch) {

        // TODO don't set the defaults for flavor and build
        this.commonOptions = commonOptions;
        this.flavor = flavor;
        this.build = build;
        this.batch = batch;
    }

    @Override
    public void execute(CommandContext context) {
        context.logInfo(String.format("%n//TODO exec init, project=%s, flavor=%s, build=%s, batch=%s, properties=%s",
                commonOptions.project(), String.valueOf(flavor), String.valueOf(build), String.valueOf(batch),
                context.properties()));
    }
}
