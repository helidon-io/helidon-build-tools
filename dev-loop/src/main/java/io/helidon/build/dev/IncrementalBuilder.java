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

import java.util.List;
import java.util.function.Consumer;

/**
 * Incremental builder.
 */
public interface IncrementalBuilder {

    /**
     * Perform an incremental build for the given changes.
     *
     * @param changes The changes.
     * @param stdOut A consumer for stdout.
     * @param stdErr A consumer for stderr.
     * @throws Exception on error.
     */
    void build(List<BuildRoot.Changes> changes,
               Consumer<String> stdOut,
               Consumer<String> stdErr) throws Exception;

    /**
     * The default incremental builder.
     */
    class Default implements IncrementalBuilder {
        @Override
        public void build(List<BuildRoot.Changes> changes, Consumer<String> stdOut, Consumer<String> stdErr) throws Exception {
            if (!changes.isEmpty()) {
                for (final BuildRoot.Changes changed : changes) {
                    changed.root().component().incrementalBuild(changed, stdOut, stdErr);
                }
            }
        }
    }
}
