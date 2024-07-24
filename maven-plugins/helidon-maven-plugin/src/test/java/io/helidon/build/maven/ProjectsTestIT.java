/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.common.test.utils.BuildLog;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.test.utils.BuildLog.assertDiffs;

/**
 * Integration test that verifies the projects under {@code src/it/projects}.
 */
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
class ProjectsTestIT {

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        File buildLog = new File(basedir, "build.log");
        BuildLog log = new BuildLog(buildLog);
        log.skipInvocations(1);
        List<String> diffs = moduleAndClassPathContains(buildLog, new File(basedir, "expected1.log"));
        assertDiffs(diffs);
        diffs = moduleAndClassPathContains(buildLog, new File(basedir, "expected2.log"));
        assertDiffs(diffs);
        diffs = moduleAndClassPathContains(buildLog, new File(basedir, "expected3.log"));
        assertDiffs(diffs);
        diffs = log.containsLines(new File(basedir, "expected4.log"));
        assertDiffs(diffs);
        diffs = log.containsLines(new File(basedir, "expected5.log"));
        assertDiffs(diffs);
        diffs = log.containsLines(new File(basedir, "expected6.log"));
        assertDiffs(diffs);
    }

    private static List<String> moduleAndClassPathContains(File log, File expected) {
        List<String> errors = new LinkedList<>();
        try {
            boolean found = false;
            int index = 0;
            String[] content = Files.readAllLines(log.toPath()).toArray(new String[0]);
            String[] expectedLines = Files.readAllLines(expected.toPath()).toArray(new String[0]);

            while (!found && index < content.length - 1) {
                if (content[index].contains("[DEBUG] Built module-path:")
                        && content[index].contains(expectedLines[0])
                        && content[index+1].contains("[DEBUG] Built class-path:")
                        && content[index+1].contains(expectedLines[1])) {
                    found = true;
                }
                index++;
            }
            if (!found) {
                errors.add("build.log does not contain " + expected.getName());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return errors;
    }
}
