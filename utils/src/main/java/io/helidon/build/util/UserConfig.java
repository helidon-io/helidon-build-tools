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
package io.helidon.build.util;

import java.nio.file.Path;

import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;

/**
 * Utility to manage user config.
 */
public class UserConfig {
    private static final String CACHE_DIR_NAME = "cache";
    private final Path homeDir;
    private final Path configDir;
    private final Path cacheDir;

    /**
     * Returns a new instance using {@link #homeDir()} as the root.
     *
     * @return The instance.
     */
    public static UserConfig create() {
        return create(FileUtils.USER_HOME_DIR);
    }

    /**
     * Returns a new instance using the given home directory.
     *
     * @param homeDir The home directory.
     * @return The instance.
     */
    public static UserConfig create(Path homeDir) {
        return new UserConfig(homeDir);
    }

    private UserConfig(Path homeDir) {
        this.homeDir = homeDir;
        this.configDir = FileUtils.ensureDirectory(homeDir.resolve(DOT_HELIDON));
        this.cacheDir = FileUtils.ensureDirectory(configDir.resolve(CACHE_DIR_NAME));
    }

    /**
     * Returns the user config directory, normally {@code ${HOME}}.
     *
     * @return The directory.
     */
    public Path homeDir() {
        return homeDir;
    }

    /**
     * Returns the user config directory, normally {@code ${HOME}/.helidon}.
     *
     * @return The directory.
     */
    public Path configDir() {
        return configDir;
    }

    /**
     * Returns the user cache directory, normally {@code ${HOME}/.helidon/cache}.
     *
     * @return The directory.
     */
    public Path cacheDir() {
        return cacheDir;
    }
}
