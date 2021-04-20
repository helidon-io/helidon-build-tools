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

package io.helidon.build.maven.enforcer;

/**
 * Exception with failure information.
 */
public class RuleFailureException extends RuntimeException {

    private final RuleFailure failure;

    /**
     * Create a new exception that builds {@link io.helidon.build.maven.enforcer.RuleFailure}.
     *
     * @param file file request related
     * @param lineNumber line number within the file
     * @param error descriptive error message
     */
    public RuleFailureException(FileRequest file, int lineNumber, String error) {
        super(error);
        this.failure = RuleFailure.create(file, lineNumber, error);
    }

    /**
     * Get the failure associated with this exception.
     *
     * @return rule failure
     */
    public RuleFailure failure() {
        return failure;
    }
}
