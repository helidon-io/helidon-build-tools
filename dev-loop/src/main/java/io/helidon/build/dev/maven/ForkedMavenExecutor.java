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

package io.helidon.build.dev.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.BuildMonitor;
import io.helidon.build.util.Constants;
import io.helidon.build.util.ProcessMonitor;

/**
 * A {@link BuildExecutor} that forks a process.
 */
public class ForkedMavenExecutor extends BuildExecutor {
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
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
    public void execute(String... args) throws Exception {
        final List<String> command = new ArrayList<>();
        command.add(MAVEN_EXEC);
        command.addAll(Arrays.asList(args));

        // Make sure we use the current JDK by forcing it first in the path and setting JAVA_HOME. This might be required
        // if we're in an IDE whose process was started with a different JDK.

        final String javaHome = Constants.javaHome();
        final String javaHomeBin = javaHome + File.separator + "bin";
        final ProcessBuilder processBuilder = new ProcessBuilder().directory(projectDirectory().toFile())
                                                                  .command(command);
        final Map<String, String> env = processBuilder.environment();
        final String path = javaHomeBin + File.pathSeparatorChar + env.get("PATH");
        env.put("PATH", path);
        env.put("JAVA_HOME", javaHome);

        ProcessMonitor.builder()
                      .processBuilder(processBuilder)
                      .stdOut(monitor().stdOutConsumer())
                      .stdErr(monitor().stdErrConsumer())
                      .capture(false)
                      .build()
                      .execute(maxBuildWaitSeconds, TimeUnit.SECONDS);
    }
}
