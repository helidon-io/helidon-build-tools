/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.common.test.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Compare file content.
 */
public class BuildLog {
    private final String[] actualLines;

    /**
     * Creates an instance of {@link BuildLog}.
     *
     * @param log   logging file
     * @throws IOException  if an error occurs during file reading
     */
    public BuildLog(File log) throws IOException {
        actualLines = Files.readAllLines(log.toPath()).toArray(new String[0]);
    }

    /**
     * Find index of provided string into file.
     *
     * @param str   string to find
     * @param fromIndex the index to start from
     * @return the position of string into the file
     */
    public int indexOf(String str, int fromIndex) {
        for (int actualIndex = fromIndex; actualIndex < actualLines.length; actualIndex++) {
            if (actualLines[actualIndex].contains(str)) {
                return actualIndex;
            }
        }
        return -1;
    }

    /**
     * Look for differences between provided file and this file.
     *
     * @param expectedLog   provided file
     * @param fromIndex     the index to start from
     * @return  list of errors found
     * @throws IOException  if an error occurs during reading
     */
    public List<String> diff(File expectedLog, int fromIndex) throws IOException {
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
                            padding(String.valueOf(actualIndex + 1)) + actualIndex + 1,
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

    /**
     * Check that the lines within provided file are contained in this file.
     *
     * @param expectedFile  provided file
     * @param fromIndex     the index to start from
     * @return  list of errors found
     * @throws IOException  if an error occurs during reading
     */
    public List<String> containsLines(File expectedFile, int fromIndex) throws IOException {
        List<String> errors = new LinkedList<>();
        String[] expectedLines = Files.readAllLines(expectedFile.toPath()).toArray(new String[0]);
        long finalIndex = fromIndex + expectedFile.length();
        if (finalIndex > actualLines.length) {
            errors.add(String.format("Trying to read out of file, fromIndex + expectedLine : %d, log lines: %d",
                    finalIndex,
                    actualLines.length));
            return errors;
        }
        for (String expectedLine : expectedLines) {
            if (Arrays.stream(actualLines)
                    .skip(fromIndex)
                    .noneMatch(line -> line.endsWith(expectedLine))) {
                errors.add(String.format("line: %s is not found into log", expectedLine));
                break;
            }
        }
        return errors;
    }

    /**
     * Assert list of error is empty.
     *
     * @param diffs list of errors
     */
    public static void assertDiffs(List<String> diffs) {
        if (!diffs.isEmpty()) {
            throw new AssertionError("diffs: "
                    + System.lineSeparator()
                    + diffs.stream().collect(Collectors.joining(System.lineSeparator())));
        }
    }

    private String padding(String key) {
        final int keyLen = key.length();
        if (5 > keyLen) {
            return " ".repeat(5 - keyLen);
        } else {
            return "";
        }
    }
}
