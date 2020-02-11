/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

import java.nio.file.Path;

import io.helidon.build.test.TestFiles;
import io.helidon.dev.mode.ProjectExecutor;

import static io.helidon.build.test.TestFiles.helidonSeProject;
import static io.helidon.dev.build.TestUtils.newLoop;
import static io.helidon.dev.build.TestUtils.run;

/**
 * DevModeDemo class.
 */
class DevModeDemo {

    public static void main(String[] args) throws Exception {
        TestFiles.targetDirFromClass(DevModeDemo.class);
        Path rootDir = helidonSeProject();
        DevModeMonitor monitor = new DevModeMonitor();
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::onStopped));
        BuildLoop loop = newLoop(rootDir, false, false, monitor);
        run(loop, 60 * 60);
    }

    static class DevModeMonitor implements BuildMonitor {
        private static final int DELAY = 1;

        private boolean start;
        private boolean restart;
        private ProjectExecutor projectExecutor;

        @Override
        public void onStarted() {
            start = true;
        }

        @Override
        public void onCycleStart(int cycleNumber) {
        }

        @Override
        public void onChanged(int cycleNumber, boolean binariesOnly) {
        }

        @Override
        public void onBuildStart(int cycleNumber, BuildType type) {
            restart = true;
        }

        @Override
        public long onBuildFail(int cycleNumber, Throwable error) {
            restart = false;
            return 0;
        }

        @Override
        public long onReady(int cycleNumber, Project project) {
            ensureProjectExecutor(project);
            if (start) {
                projectExecutor.start();
                start = false;
            } else if (restart) {
                projectExecutor.restart();
                restart = false;
            }
            return DELAY;
        }

        @Override
        public boolean onCycleEnd(int cycleNumber) {
            return true;
        }

        @Override
        public void onStopped() {
            if (projectExecutor != null) {
                projectExecutor.stop();
            }
        }

        private void ensureProjectExecutor(Project project) {
            if (projectExecutor == null) {
                projectExecutor = new ProjectExecutor(project);
            }
        }
    }
}
