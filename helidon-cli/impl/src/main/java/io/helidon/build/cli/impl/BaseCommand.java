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

import java.nio.file.Path;

import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.util.AnsiConsoleInstaller;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;

/**
 * Class BaseCommand.
 */
public abstract class BaseCommand implements CommandExecution {

    static final String HELIDON_VERSION_PROPERTY = "helidon.version";

    private final CommonOptions commonOptions;
    private ProjectConfig projectConfig;
    private Path projectDir;

    /**
     * Constructor.
     *
     * @param commonOptions The common options.
     */
    protected BaseCommand(CommonOptions commonOptions) {
        AnsiConsoleInstaller.ensureInstalled();
        this.commonOptions = commonOptions;
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        assertPreconditions();
        checkForUpdates();
        invoke(context);
    }

    protected abstract void assertPreconditions();

    protected void checkForUpdates() {
        commonOptions.checkForUpdates();
    }

    protected abstract void invoke(CommandContext context) throws Exception;

    protected ProjectConfig projectConfig() {
        return projectConfig(commonOptions.project());
    }

    protected ProjectConfig projectConfig(Path dir) {
        if (projectConfig != null && dir.equals(projectDir)) {
            return projectConfig;
        }
        Path dotHelidon = dir.resolve(DOT_HELIDON);
        projectConfig = new ProjectConfig(dotHelidon);
        projectDir = dir;
        return projectConfig;
    }

    protected Metadata metadata() {
        return commonOptions.metadata();
    }
}
