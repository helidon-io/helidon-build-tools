/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer;

/**
 * Rule failure.
 */
public class RuleFailure {
    private final FileRequest fr;
    private final String message;
    private final int line;

    private RuleFailure(FileRequest fr, int line, String message) {
        this.fr = fr;
        this.line = line;
        this.message = message;
    }

    /**
     * Create a new rule failure.
     *
     * @param fr File request
     * @param line line within the file
     * @param message description of the failure
     * @return a new rule failure
     */
    public static RuleFailure create(FileRequest fr, int line, String message) {
        return new RuleFailure(fr, line, message);
    }

    /**
     * Create new rule failure.
     *
     * @param message description of the failure
     * @return a new rule failure
     */
    public static RuleFailure create(String message) {
        return new RuleFailure(null, 0, message);
    }

    /**
     * File request.
     *
     * @return file request causing this failure
     */
    public FileRequest fr() {
        return fr;
    }

    /**
     * Descriptive message.
     * @return message
     */
    public String message() {
        return message;
    }

    /**
     * Line within the file, or {@code 0} if cannot be determined.
     *
     * @return line number starting from {@code 1}
     */
    public int line() {
        return line;
    }

    /**
     * Print Rule failure content.
     *
     * @return rule failure description
     */
    public String print() {
        return fr != null
                ? "  " + this.fr.relativePath() + ":" + this.line + ": " + this.message
                : "  " + this.message;
    }
}
