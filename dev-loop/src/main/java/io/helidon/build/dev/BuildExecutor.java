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

package io.helidon.build.dev;

import java.nio.file.Path;
import java.util.List;

import static io.helidon.build.util.FileUtils.assertDir;

/**
 * An abstract build executor.
 */
public abstract class BuildExecutor {
    private static final String[] EMPTY = new String[0];
    private final Path projectDir;
    private final BuildMonitor monitor;

    /**
     * Constructor.
     *
     * @param projectDir The project directory.
     * @param monitor The build monitor. All output is written to {@link BuildMonitor#stdOutConsumer()} and
     * {@link BuildMonitor#stdErrConsumer()}.
     */
    protected BuildExecutor(Path projectDir, BuildMonitor monitor) {
        this.projectDir = assertDir(projectDir);
        this.monitor = monitor;
    }

    /**
     * Execute maven with the given arguments.
     *
     * @param args The maven arguments.
     * @throws Exception on error.
     */
    public abstract void execute(String... args) throws Exception;

    /**
     * Execute maven with the given arguments.
     *
     * @param args The maven arguments.
     * @throws Exception on error.
     */
    public void execute(List<String> args) throws Exception {
        execute(args.toArray(EMPTY));
    }

    /**
     * Returns the build monitor.
     *
     * @return The monitor.
     */
    public BuildMonitor monitor() {
        return monitor;
    }

    /**
     * Returns the project directory.
     *
     * @return The directory.
     */
    public Path projectDirectory() {
        return projectDir;
    }
}
