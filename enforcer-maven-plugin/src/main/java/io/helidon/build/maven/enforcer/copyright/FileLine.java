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

package io.helidon.build.maven.enforcer.copyright;

/**
 * Numbered file line.
 */
class FileLine {
    private final int lineNumber;
    private final String line;

    private FileLine(int lineNumber, String line) {
        this.lineNumber = lineNumber;
        this.line = line;
    }

    static FileLine create(int lineNumber, String line) {
        return new FileLine(lineNumber, line);
    }

    int lineNumber() {
        return lineNumber;
    }

    String line() {
        return line;
    }
}
