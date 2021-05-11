/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.devloop;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

/**
 * A {@code Project} supplier.
 */
public interface ProjectSupplier {

    /**
     * Returns a new {@code Project} instance from the project directory that has been successfully built. Implementations
     * must call {@link BuildMonitor#onBuildStart(int, BuildType)} if a build is performed.
     *
     * @param executor The build executor.
     * @param clean {@code true} if the project should be cleaned and built.
     * @param allowSkip {@code true} if the project build can be skipped if up to date.
     * @param cycleNumber The cycle number.
     * @return The project instance, guaranteed to have been successfully built.
     * @throws IllegalArgumentException if the project directory is not a valid.
     * @throws Exception if the build fails.
     */
    Project newProject(BuildExecutor executor, boolean clean, boolean allowSkip, int cycleNumber) throws Exception;

    /**
     * Returns whether or not any project file has a modified time more recent than the given time.
     *
     * @param projectDir The project directory.
     * @param lastCheckTime The time to check against.
     * @return {@code true} if changed.
     */
    boolean hasChanges(Path projectDir, FileTime lastCheckTime);

    /**
     * Returns the most recent modification time if any project file has a modified time more recent than the given time.
     *
     * @param projectDir The project directory.
     * @param lastCheckTime The time to check against.
     * @return The time, if changed.
     */
    Optional<FileTime> changedSince(Path projectDir, FileTime lastCheckTime);

    /**
     * Returns the name of the build file supported by this supplier, e.g. "pom.xml".
     *
     * @return The name.
     */
    String buildFileName();
}
