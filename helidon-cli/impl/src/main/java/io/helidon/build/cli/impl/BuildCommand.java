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
import io.helidon.build.cli.harness.Config;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValue;
import io.helidon.build.util.MavenCommand;

import static io.helidon.build.cli.harness.CommandContext.Verbosity.NORMAL;
import static io.helidon.build.cli.impl.CommandRequirements.requireMinimumMavenVersion;
import static io.helidon.build.cli.impl.CommandRequirements.requireValidMavenProjectConfig;
import static io.helidon.build.util.Constants.ENABLE_HELIDON_CLI;

/**
 * The {@code build} command.
 */
@Command(name = "build", description = "Build the application")
public final class BuildCommand extends BaseCommand {

    private static final String JLINK_OPTION = "-Pjlink-image";
    private static final String NATIVE_OPTION = "-Pnative-image";
    private static final String PLUGIN_VERSION_PROPERTY_PREFIX = "-Dversion.plugin.helidon=";

    private final CommonOptions commonOptions;
    private final boolean clean;
    private final BuildMode buildMode;
    private final String pluginVersionProperty;

    enum BuildMode {
        PLAIN,
        NATIVE,
        JLINK
    }

    @Creator
    BuildCommand(CommonOptions commonOptions,
                 @Flag(name = "clean", description = "Perform a clean before the build") boolean clean,
                 @KeyValue(name = "mode", description = "Build mode", defaultValue = "PLAIN") BuildMode buildMode,
                 @Flag(name = "current", description = "Use the build version as the helidon-maven-plugin version",
                         visible = false)
                         boolean currentPluginVersion) {
        super(commonOptions, true);
        this.commonOptions = commonOptions;
        this.clean = clean;
        this.buildMode = buildMode;
        this.pluginVersionProperty = currentPluginVersion ? PLUGIN_VERSION_PROPERTY_PREFIX + Config.buildVersion() : null;
    }

    @Override
    protected void assertPreconditions() {
        requireMinimumMavenVersion();
        requireValidMavenProjectConfig(commonOptions);
    }

    @Override
    protected void invoke(CommandContext context) throws Exception {

        MavenCommand.Builder builder = MavenCommand.builder()
                                                   .addArgument(ENABLE_HELIDON_CLI)
                                                   .addOptionalArgument(pluginVersionProperty)
                                                   .addArguments(context.propertyArgs(true))
                                                   .verbose(context.verbosity() != NORMAL)
                                                   .directory(commonOptions.project());
        switch (buildMode) {
            case PLAIN:
                break;
            case JLINK:
                builder.addArgument(JLINK_OPTION);
                break;
            case NATIVE:
                builder.addArgument(NATIVE_OPTION);
                break;
            default:
                throw new IllegalStateException("Unknown build mode " + buildMode);
        }

        if (clean) {
            builder.addArgument("clean");
        }
        builder.addArgument("package");

        builder.build().execute();
    }
}
