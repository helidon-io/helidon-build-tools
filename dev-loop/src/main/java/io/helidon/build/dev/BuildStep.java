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

import io.helidon.build.util.ConsolePrinter;

/**
 * A project build step.
 */
public interface BuildStep {

    /**
     * Returns the name of this build step.
     *
     * @return the name.
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Execute the build step for the given changed files only. Any component that does not match this predicate is ignored.
     *
     * @param changes The changes.
     * @param stdOut A consumer for stdout.
     * @param stdErr A consumer for stderr.
     * @throws Exception on error.
     */
    void incrementalBuild(BuildRoot.Changes changes,
                          ConsolePrinter stdOut,
                          ConsolePrinter stdErr) throws Exception;
}
