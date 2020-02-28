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

package io.helidon.dev.build;

import java.nio.file.Path;

/**
 * A {@code Project} supplier.
 */
public interface ProjectSupplier {

    /**
     * Returns a new {@code Project} instance from the given directory that has been successfully built.
     *
     * @param projectDir The project directory.
     * @param monitor The build monitor. Implementations must call {@link BuildMonitor#onBuildStart(int, BuildType)} if a build
     * is performed.
     * @param clean {@code true} if the project should be cleaned and built.
     * @param cycleNumber The cycle number.
     * @return The project instance, guaranteed to have been successfully built.
     * @throws IllegalArgumentException if the project directory is not a valid.
     * @throws Exception if the build fails.
     */
    Project get(Path projectDir, BuildMonitor monitor, boolean clean, int cycleNumber) throws Exception;
}
