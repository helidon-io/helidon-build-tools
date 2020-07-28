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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.helidon.build.util.FileUtils;

import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;

/**
 * Utility to manage user config.
 */
public class UserConfig {
    private static final String CACHE_DIR_NAME = "cache";
    private static final String PLUGINS_DIR_NAME = "plugins";
    private static final String CONFIG_FILE_NAME = "config";
    private static final String CONFIG_FILE_KEY = "config.file";
    private static final String UPDATE_INTERVAL_HOURS_KEY = "update.check.retry.hours";
    private static final String UPDATE_INTERVAL_HOURS_DEFAULT_VALUE = "12";
    private static final String DOWNLOAD_UPDATES_KEY = "download.new.releases";
    private static final String DOWNLOAD_UPDATES_DEFAULT_VALUE = "true";
    private static final String UPDATE_URL_KEY = "update.url";
    private static final String UPDATE_URL_DEFAULT_VALUE = Metadata.DEFAULT_URL;
    private static final String DEFAULT_CONFIG =
            "\n"
            + "# The CLI regularly updates information about new Helidon and/or CLI releases;\n"
            + "# this value controls the minimum number of hours between rechecks.\n"
            + "\n"
            + UPDATE_INTERVAL_HOURS_KEY + "=" + UPDATE_INTERVAL_HOURS_DEFAULT_VALUE + "\n"
            + "\n"
            + "# The CLI can download new releases to help reduce the number of installation\n"
            + "# steps; this value controls whether or not to do so.\n"
            + "\n"
            + DOWNLOAD_UPDATES_KEY + "=" + DOWNLOAD_UPDATES_DEFAULT_VALUE + "\n"
            + "\n";

    private final Path homeDir;
    private final Path configDir;
    private final Path cacheDir;
    private final Path pluginsDir;
    private final Path configFile;
    private final Properties config;

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
        this.homeDir = homeDir.toAbsolutePath();
        this.configDir = FileUtils.ensureDirectory(homeDir.resolve(DOT_HELIDON));
        this.cacheDir = FileUtils.ensureDirectory(configDir.resolve(CACHE_DIR_NAME));
        this.pluginsDir = FileUtils.ensureDirectory(configDir.resolve(PLUGINS_DIR_NAME));
        this.configFile = configDir.resolve(CONFIG_FILE_NAME);
        this.config = loadConfig();
    }

    /**
     * Returns the URL from which to get metadata.
     *
     * @return The url.
     */
    public String metadataUrl() {
        return config.getProperty(UPDATE_URL_KEY, UPDATE_URL_DEFAULT_VALUE);
    }

    /**
     * Returns the check for updates interval in hours.
     *
     * @return The interval.
     */
    public int checkForUpdatesIntervalHours() {
        int result = 0;
        String value = config.getProperty(UPDATE_INTERVAL_HOURS_KEY, UPDATE_INTERVAL_HOURS_DEFAULT_VALUE);
        try {
            result = Integer.parseInt(value);
            if (result < 0) {
                invalidConfiguration(UPDATE_INTERVAL_HOURS_KEY, "must not be negative", value);
            }
        } catch (NumberFormatException e) {
            invalidConfiguration(UPDATE_INTERVAL_HOURS_KEY, "must be a positive integer", value);
        }
        return result;
    }

    /**
     * Returns whether or not updates should be downloaded.
     *
     * @return {@code true} if updates should be downloaded.
     */
    public boolean downloadUpdates() {
        return Boolean.parseBoolean(config.getProperty(DOWNLOAD_UPDATES_KEY, DOWNLOAD_UPDATES_DEFAULT_VALUE));
    }

    /**
     * Returns all config properties.
     *
     * @return The config.
     */
    public Properties properties() {
        return config;
    }

    /**
     * Returns the config file.
     *
     * @return The file.
     */
    public Path path() {
        return configFile;
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

    /**
     * Returns the user plugins directory, normally {@code ${HOME}/.helidon/plugins}.
     *
     * @return The directory.
     */
    public Path pluginsDir() {
        return pluginsDir;
    }

    /**
     * Clear all cache content.
     *
     * @throws IOException if an error occurs.
     */
    public void clearCache() throws IOException {
        FileUtils.deleteDirectoryContent(cacheDir());
    }

    /**
     * Clear all plugins.
     *
     * @throws IOException if an error occurs.
     */
    public void clearPlugins() throws IOException {
        FileUtils.deleteDirectoryContent(pluginsDir());
    }

    private Properties loadConfig() {
        final Properties properties = new Properties();
        try {
            if (!Files.exists(configFile)) {
                Files.writeString(configFile, DEFAULT_CONFIG);
            }
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile))) {
                properties.load(reader);
            }
            properties.put(CONFIG_FILE_KEY, configFile.toRealPath());
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void invalidConfiguration(String key, String reason, String value) {
        throw new IllegalStateException(key + " in " + configFile + " " + reason + ": " + value);
    }
}
