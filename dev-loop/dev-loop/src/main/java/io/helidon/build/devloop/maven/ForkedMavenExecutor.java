/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.devloop.maven;

import java.nio.file.Path;
import java.util.Arrays;

import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.devloop.BuildExecutor;
import io.helidon.build.devloop.BuildMonitor;

/**
 * A {@link BuildExecutor} that forks a Maven process.
 */
public class ForkedMavenExecutor extends BuildExecutor {
    private final int maxBuildWaitSeconds;

    /**
     * Constructor.
     *
     * @param projectDir The project directory.
     * @param monitor The build monitor. All output is written to {@link BuildMonitor#stdOutConsumer()} and
     * {@link BuildMonitor#stdErrConsumer()}.
     * @param maxBuildWaitSeconds The maximum number of seconds to wait for a build to complete.
     */
    public ForkedMavenExecutor(Path projectDir, BuildMonitor monitor, int maxBuildWaitSeconds) {
        super(projectDir, monitor);
        this.maxBuildWaitSeconds = maxBuildWaitSeconds;
    }

    @Override
    public boolean willFork() {
        return true;
    }

    @Override
    public void execute(String... args) throws Exception {
        MavenCommand.builder()
                    .directory(projectDirectory())
                    .arguments(Arrays.asList(args))
                    .stdOut(monitor().stdOutConsumer())
                    .stdErr(monitor().stdErrConsumer())
                    .maxWaitSeconds(maxBuildWaitSeconds)
                    .build()
                    .execute();
    }
}
