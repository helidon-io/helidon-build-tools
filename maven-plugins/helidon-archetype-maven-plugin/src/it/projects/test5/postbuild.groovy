/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

def actualIndex = 0
def actualLines = new File(basedir, "build.log").readLines()


def findLines(actualIndex, actualLines, fname) {
    def expectedLines = new File(basedir, fname).readLines()
    def found = false
    def errors = ["build.log does not contain ${fname}"]
    while (!found && actualIndex < actualLines.size() - 1) {
        // seek
        for (; actualIndex < actualLines.size(); actualIndex++) {
            if (actualLines[actualIndex].contains(expectedLines[0])) {
                break;
            }
        }
        for (def expectedIndex = 1; expectedIndex < expectedLines.size() && actualIndex < actualLines.size() - 1; expectedIndex++) {
            def expected = expectedLines[expectedIndex]
            def actual = actualLines[++actualIndex]
            if (!actual.endsWith(expected)) {
                errors.add("line: ${('' + actualIndex).padRight(5)} >>${expected}<< != >>${actual}<<")
                break;
            }
            if (expectedIndex == expectedLines.size() - 1) {
                found = true
            }
        }
    }
    assertTrue(found, errors)
}

def assertTrue(found, errors) {
    if (!found) {
        throw new AssertionError("""

------------------------------------------------------------------------
${errors.join('\n')}
------------------------------------------------------------------------

""")
    }
}

findLines(actualIndex, actualLines, "expected.log")