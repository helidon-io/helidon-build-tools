package io.helidon.build.stager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Staging context.
 */
interface StagingContext {

    /**
     * Unpack the given archive to a target location.
     *
     * @param archive  archive to unpack
     * @param target   where to unpack the archive
     * @param excludes exclude filters
     * @param includes include filters
     */
    void unpack(Path archive, Path target, String excludes, String includes);

    /**
     * Archive the given directory to a target archive.
     *
     * @param directory directory to archive
     * @param target    the archive to create
     * @param excludes  exclude filters
     * @param includes  include filters
     */
    void archive(Path directory, Path target, String excludes, String includes);

    /**
     * Resolve the given GAV.
     *
     * @param gav the GAV to resolve
     * @return resolved artifact file
     */
    Path resolve(ArtifactGAV gav);

    /**
     * Create a temporary directory.
     *
     * @param prefix directory prefix
     * @return created directory
     * @throws IOException if an IO error occurs
     */
    Path createTempDirectory(String prefix) throws IOException;

    /**
     * Log an info message.
     *
     * @param msg message, can use format
     * @param args message arguments
     */
    void logInfo(String msg, Object... args);

    /**
     * Log a warning message.
     *
     * @param msg message, can use format
     * @param args message arguments
     */
    void logWarning(String msg, Object... args);

    /**
     * Log an error message.
     *
     * @param msg message, can use format
     * @param args message arguments
     */
    void logError(String msg, Object... args);

    /**
     * Log a debug message.
     *
     * @param msg message, can use format
     * @param args message arguments
     */
    void logDebug(String msg, Object... args);
}
