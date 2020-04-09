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
import java.util.concurrent.TimeUnit;

import io.helidon.build.dev.maven.DefaultProjectSupplier;
import io.helidon.build.dev.maven.EmbeddedMavenExecutor;
import io.helidon.build.dev.maven.ForkedMavenExecutor;
import io.helidon.build.util.Log;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Class TestUtils.
 */
class TestUtils {

    private TestUtils() {
    }

    static BuildLoop newLoop(Path projectRoot,
                             boolean initialClean,
                             boolean watchBinariesOnly,
                             int stopCycleNumber) {
        return newLoop(projectRoot, initialClean, watchBinariesOnly, new TestMonitor(stopCycleNumber));
    }

    static BuildLoop newLoop(Path projectRoot,
                             boolean initialClean,
                             boolean watchBinariesOnly,
                             BuildMonitor monitor) {
        return BuildLoop.builder()
                        .buildExecutor(buildExecutor(projectRoot, monitor))
                        .clean(initialClean)
                        .watchBinariesOnly(watchBinariesOnly)
                        .projectSupplier(new DefaultProjectSupplier())
                        .build();
    }

    static BuildExecutor buildExecutor(Path projectRoot, BuildMonitor monitor) {
        if ("true".equals(System.getProperty("use.embedded.maven.executor"))) {
            Log.info("Using embedded maven executor");
            return new EmbeddedMavenExecutor(projectRoot, monitor);
        } else {
            Log.info("Using forked maven executor");
            return new ForkedMavenExecutor(projectRoot, monitor, 60);

        }
    }

    static <T extends BuildMonitor> T run(BuildLoop loop) throws InterruptedException {
        return run(loop, 30);
    }

    @SuppressWarnings("unchecked")
    static <T extends BuildMonitor> T run(BuildLoop loop, int maxWaitSeconds) throws InterruptedException {
        loop.start();
        Log.info("Waiting up to %d seconds for build loop completion", maxWaitSeconds);
        if (!loop.waitForStopped(maxWaitSeconds, TimeUnit.SECONDS)) {
            loop.stop(0L);
            fail("Timeout");
        }
        return (T) loop.monitor();
    }
}
