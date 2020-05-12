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

import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenCommand;
import io.helidon.build.util.SystemLogWriter;

import static io.helidon.build.cli.harness.CommandContext.Verbosity.DEBUG;
import static io.helidon.build.util.MavenCommand.assertRequiredMavenVersion;
import static io.helidon.build.util.MavenVersion.toMavenVersion;

/**
 * A base type for commands that use {@link MavenCommand}.
 */
public class MavenBaseCommand extends BaseCommand {
    private static final String MINIMUM_REQUIRED_MAVEN_VERSION = "3.6.0";

    /**
     * Tests whether the installed Maven version is older than the required minimum.
     *
     * @param context The context.
     * @return {@code false} if the installed version meets the requirement, {@code false} if not and
     * the exit action has been set.
     */
    public boolean isMavenVersionOutOfDate(CommandContext context) {
        try {
            if (context.verbosity() == DEBUG) {
                SystemLogWriter.bind(Log.Level.DEBUG);
            }
            assertRequiredMavenVersion(toMavenVersion(MINIMUM_REQUIRED_MAVEN_VERSION));
            return false;
        } catch (IllegalStateException e) {
            context.exitAction(CommandContext.ExitStatus.FAILURE, e.getMessage());
            return true;
        }
    }
}
