/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * Staging context.
 */
@SuppressWarnings("unused")
interface StagingContext {

    /**
     * Constant for the readTimeout property.
     */
    String READ_TIMEOUT_PROP = "stager.readTimeout";

    /**
     * Constant for the connectTimeout property.
     */
    String CONNECT_TIMEOUT_PROP = "stager.connectTimeout";

    /**
     * Constant for the taskTimeout property.
     */
    String TASK_TIMEOUT_PROP = "stager.taskTimeout";

    /**
     * Constant for the maxRetries property.
     */
    String MAX_RETRIES = "stager.maxRetries";

    /**
     * Unpack the given archive to a target location.
     *
     * @param archive  archive to unpack
     * @param target   where to unpack the archive
     * @param excludes exclude filters
     * @param includes include filters
     */
    default void unpack(Path archive, Path target, String excludes, String includes) {
        throw new UnsupportedOperationException();
    }

    /**
     * Archive the given directory to a target archive.
     *
     * @param directory directory to archive
     * @param target    the archive to create
     * @param excludes  exclude filters
     * @param includes  include filters
     */
    default void archive(Path directory, Path target, String excludes, String includes) {
        throw new UnsupportedOperationException();
    }

    /**
     * Resolve a path in the project.
     *
     * @param path path to resolve
     * @return Path
     */
    default Path resolve(String path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Resolve the given GAV.
     *
     * @param gav the GAV to resolve
     * @return resolved artifact file
     */
    default Path resolve(ArtifactGAV gav) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a temporary directory.
     *
     * @param prefix directory prefix
     * @return created directory
     * @throws IOException if an IO error occurs
     */
    default Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * Create a temporary file.
     *
     * @param suffix suffix
     * @return created file
     * @throws IOException if an IO error occurs
     */
    default Path createTempFile(String suffix) throws IOException {
        return Files.createTempFile(null, suffix);
    }

    /**
     * Log an info message.
     *
     * @param msg  message, can use format
     * @param args message arguments
     */
    default void logInfo(String msg, Object... args) {
        System.out.println("INFO: " + String.format(msg, args));
    }

    /**
     * Log a warning message.
     *
     * @param msg  message, can use format
     * @param args message arguments
     */
    default void logWarning(String msg, Object... args) {
        System.out.println("WARNING: " + String.format(msg, args));
    }

    /**
     * Log an error message.
     *
     * @param msg  message, can use format
     * @param args message arguments
     */
    default void logError(String msg, Object... args) {
        System.out.println("ERROR: " + String.format(msg, args));
    }

    default void logError(Throwable ex) {
        logError(ex.toString());
    }

    default boolean isDebugEnabled() {
        return false;
    }

    /**
     * Log a debug message.
     *
     * @param msg  message, can use format
     * @param args message arguments
     */
    default void logDebug(String msg, Object... args) {
        System.out.println("DEBUG: " + String.format(msg, args));
    }

    /**
     * Get the executor.
     *
     * @return Executor
     */
    Executor executor();

    /**
     * Read timeout configuration.
     *
     * @return value greater than zero if set.
     */
    default int readTimeout() {
        return -1;
    }

    /**
     * Connect timeout configuration.
     *
     * @return value greater than zero if set.
     */
    default int connectTimeout() {
        return -1;
    }

    /**
     * Task timeout configuration.
     *
     * @return value greater than zero if set.
     */
    default int taskTimeout() {
        return -1;
    }

    /**
     * Max retries configuration.
     *
     * @return value greater than zero if set.
     */
    default int maxRetries() {
        return -1;
    }

    /**
     * Lookup a property.
     *
     * @param name property name
     * @return property value or {@code null}
     */
    default String property(String name) {
        return null;
    }
}
