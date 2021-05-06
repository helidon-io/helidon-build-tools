/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.common.test.utils.ConfigurationParameterSource;

import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.Strings.padding;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Integration test that verifies the projects under {@code src/it/projects}.
 */
final class ProjectsTestIT {

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        BuildLog buildLog = new BuildLog(new File(basedir, "build.log"));
        int index = buildLog.indexOf("BUILD SUCCESS", 0);
        assertThat(index > 0, is(true));
        index = buildLog.indexOf("BUILD SUCCESS", index);
        assertThat(index > 0, is(true));
        List<String> diffs = buildLog.diff(new File(basedir, "expected.log"), index);
        assertDiffs(diffs);
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test2(String basedir) throws IOException {
        BuildLog buildLog = new BuildLog(new File(basedir, "build.log"));
        int index = buildLog.indexOf("BUILD SUCCESS", 0);
        assertThat(index > 0, is(true));
        List<String> diffs1 = buildLog.diff(new File(basedir, "expected1.log"), index);
        assertDiffs(diffs1);
        List<String> diffs2 = buildLog.diff(new File(basedir, "expected2.log"), index);
        assertDiffs(diffs2);
    }

    private static void assertDiffs(List<String> diffs) {
        if (!diffs.isEmpty()) {
            throw new AssertionError("diffs: "
                    + System.lineSeparator()
                    + diffs.stream().collect(Collectors.joining(System.lineSeparator())));
        }
    }

    private static final class BuildLog {

        private final String[] actualLines;

        BuildLog(File log) throws IOException {
            actualLines = Files.readAllLines(log.toPath()).toArray(new String[0]);
        }

        int indexOf(String str, int fromIndex) {
            for (int actualIndex = fromIndex; actualIndex < actualLines.length; actualIndex++) {
                if (actualLines[actualIndex].contains(str)) {
                    return actualIndex;
                }
            }
            return -1;
        }

        List<String> diff(File expectedLog, int fromIndex) throws IOException {
            String[] expectedLines = Files.readAllLines(expectedLog.toPath()).toArray(new String[0]);
            List<String> diffs = new LinkedList<>();
            int actualIndex = fromIndex;
            while (actualIndex < actualLines.length - 1) {
                // seek
                for (; actualIndex < actualLines.length; actualIndex++) {
                    if (actualLines[actualIndex].endsWith(expectedLines[0])) {
                        break;
                    }
                }
                for (int index = 1; index < expectedLines.length && actualIndex < actualLines.length - 1; index++) {
                    String expected = expectedLines[index];
                    String actual = actualLines[++actualIndex];
                    if (!actual.endsWith(expected)) {
                        diffs.add(String.format("line: %s >>%s<< != >>%s<<",
                                padding(" ", 5, String.valueOf(actualIndex + 1)) + actualIndex + 1,
                                expected,
                                actual));
                        break;
                    }
                    if (index == expectedLines.length - 1) {
                        return Collections.emptyList();
                    }
                }
            }
            return diffs;
        }
    }
}
