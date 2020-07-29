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
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.helidon.build.util.FileUtils;

import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static java.util.stream.Collectors.toMap;

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
    private static final String SYSTEM_PROPERTY_PREFIX = "system_";
    private static final String DEFAULT_CONFIG =
            "\n"
            + "# The CLI regularly updates information about new Helidon and/or CLI releases;\n"
            + "# this value controls the minimum number of hours between rechecks. Updates can\n"
            + "# be forced on every invocation with a 0 value or disabled with a negative value.\n"
            + "\n"
            + UPDATE_INTERVAL_HOURS_KEY + "=" + UPDATE_INTERVAL_HOURS_DEFAULT_VALUE + "\n"
            + "\n"
            + "# The CLI can download new releases to help reduce the number of installation\n"
            + "# steps; this value controls whether or not to do so.\n"
            + "\n"
            + DOWNLOAD_UPDATES_KEY + "=" + DOWNLOAD_UPDATES_DEFAULT_VALUE + "\n"
            + "\n"
            + "# System properties can be set by using the \"system_\" key prefix, e.g.:\n"
            + "\n"
            + "# " + "system_http.proxyHost=http://proxy.acme.com" + "\n"
            + "# " + "system_http.proxyPort=80" + "\n"
            + "# " + "system_http.nonProxyHosts=*.local|localhost|127.0.0.1|*.acme.com" + "\n"
            + "# " + "system_https.proxyHost=http://proxy.acme.com" + "\n"
            + "# " + "system_https.proxyPort=80" + "\n"
            + "# " + "system_https.nonProxyHosts=*.local|localhost|127.0.0.1|*.acme.com" + "\n"
            + "\n"
            + "# The CLI fetches update information from this location; setting this may be\n"
            + "# necessary in environments with restricted internet access.\n"
            + "\n"
            + "# " + UPDATE_URL_KEY + "=" + UPDATE_URL_DEFAULT_VALUE + "\n"
            + "\n";

    private final Path homeDir;
    private final Path configDir;
    private final Path cacheDir;
    private final Path pluginsDir;
    private final Path configFile;
    private final Map<String, String> allProperties;
    private final Map<String, String> systemProperties;

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
        this.allProperties = loadConfig();
        this.systemProperties = setSystemProperties();
    }

    /**
     * Returns the URL from which to get updates.
     *
     * @return The url.
     */
    public String updateUrl() {
        return allProperties.getOrDefault(UPDATE_URL_KEY, UPDATE_URL_DEFAULT_VALUE);
    }

    /**
     * Returns the check for updates interval in hours.
     *
     * @return The interval.
     */
    public int checkForUpdatesIntervalHours() {
        String value = allProperties.getOrDefault(UPDATE_INTERVAL_HOURS_KEY, UPDATE_INTERVAL_HOURS_DEFAULT_VALUE);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(UPDATE_INTERVAL_HOURS_KEY + " in " + configFile + ": " + e.getMessage());
        }
    }

    /**
     * Returns whether or not updates should be downloaded.
     *
     * @return {@code true} if updates should be downloaded.
     */
    public boolean downloadUpdates() {
        return Boolean.parseBoolean(allProperties.getOrDefault(DOWNLOAD_UPDATES_KEY, DOWNLOAD_UPDATES_DEFAULT_VALUE));
    }

    /**
     * Returns all user config properties.
     *
     * @return The config properties.
     */
    public Map<String, String> properties() {
        return allProperties;
    }

    /**
     * Returns all system properties set via user config.
     *
     * @return The system properties.
     */
    public Map<String, String> systemProperties() {
        return systemProperties;
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

    private Map<String, String> loadConfig() {
        Map<String, String> result = new HashMap<>();
        Properties properties = new Properties();
        try {
            Reader in = null;
            if (!Files.exists(configFile)) {
                Files.writeString(configFile, DEFAULT_CONFIG);
                in = new StringReader(DEFAULT_CONFIG);
            }
            try (Reader reader = in == null ? new InputStreamReader(Files.newInputStream(configFile)) : in) {
                properties.load(reader);
            }
            properties.forEach((key, value) -> result.put(key.toString(), value.toString()));
            result.put(CONFIG_FILE_KEY, configFile.toRealPath().toString());
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, String> setSystemProperties() {
        return properties().entrySet()
                           .stream()
                           .filter(e -> e.getKey().startsWith(SYSTEM_PROPERTY_PREFIX))
                           .map(e -> Map.entry(e.getKey().substring(SYSTEM_PROPERTY_PREFIX.length()), e.getValue()))
                           .peek(e -> System.setProperty(e.getKey(), e.getValue()))
                           .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
