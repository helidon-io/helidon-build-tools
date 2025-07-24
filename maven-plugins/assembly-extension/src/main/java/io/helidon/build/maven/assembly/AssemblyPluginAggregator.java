/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.assembly;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

/**
 * Assembly Maven Plugin file aggregator.
 */
interface AssemblyPluginAggregator {

    /**
     * File name with path of the file being aggregated.
     *
     * @return file name with path
     */
    String path();

    /**
     * Aggregate provided file.
     *
     * @param fileInfo file to aggregate
     * @throws IOException when file could not be read
     */
    void aggregate(FileInfo fileInfo) throws IOException;

    /**
     * Write temporary file to be aggregated file to target archive.
     *
     * @return temporary file to be aggregated
     * @throws ArchiverException when file could not be written
     */
    File writeFile() throws ArchiverException;

    /**
     * Whether aggregator content is empty.
     * Called before {@link #writeFile()} to check whether file should be written or not.
     *
     * @return value of {@code false} when aggregator content is not empty and file should be written
     *         or {@code true} otherwise
     */
    boolean isEmpty();

}
