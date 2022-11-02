/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Strings;
import io.helidon.build.util.SubstitutionVariables;

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
    private static final String DEFAULT_PROJECT_NAME_KEY = "default.project.name";
    private static final String DEFAULT_PROJECT_NAME_DEFAULT_VALUE = "${init_archetype}-${init_flavor}";
    private static final String DEFAULT_GROUP_ID_KEY = "default.group.id";
    private static final String DEFAULT_GROUP_ID_DEFAULT_VALUE = "me.${user.name}-helidon";
    private static final String DEFAULT_ARTIFACT_ID_KEY = "default.artifact.id";
    private static final String DEFAULT_ARTIFACT_DEFAULT_VALUE = "${init_archetype}-${init_flavor}";
    private static final String DEFAULT_PACKAGE_NAME_KEY = "default.package.name";
    private static final String DEFAULT_PACKAGE_NAME_DEFAULT_VALUE = "me.${user.name}.${init_flavor}.${init_archetype}";
    private static final String FAIL_ON_PROJECT_NAME_COLLISION_KEY = "fail.on.project.name.collision";
    private static final String RICH_TEXT_KEY = CommandContext.InternalOptions.RICH_TEXT_KEY;
    private static final String RICH_TEXT_DEFAULT_VALUE = CommandContext.InternalOptions.RICH_TEXT_DEFAULT_VALUE;
    private static final String FAIL_ON_PROJECT_NAME_COLLISION_DEFAULT_VALUE = "false";
    private static final String UPDATE_INTERVAL_HOURS_KEY = "update.check.retry.hours";
    private static final String UPDATE_INTERVAL_HOURS_DEFAULT_VALUE = "12";
    private static final String DOWNLOAD_UPDATES_KEY = "download.new.releases";
    private static final String DOWNLOAD_UPDATES_DEFAULT_VALUE = "true";
    private static final String UPDATE_URL_KEY = "update.url";
    private static final String UPDATE_URL_DEFAULT_VALUE = "https://helidon.io/cli-data";
    private static final String SYSTEM_PROPERTY_PREFIX = "system_";
    private static final String DEFAULT_CONFIG =
            "\n"
                    + "# When using the init command to create a new project, default values for the\n"
                    + "# project name, group id and artifact id are defined here. Property substitution\n"
                    + "# is performed with values looked up first from system properties then environment\n"
                    + "# variables. A few special properties are defined and resolved during init command\n"
                    + "# execution and are distinguished with an \"init_\" prefix:\n"
                    + "#\n"
                    + "#  init_flavor     the selected Helidon flavor, e.g. \"SE\", converted to lowercase\n"
                    + "#  init_archetype  the name of the selected archetype, e.g. \"quickstart\"\n"
                    + "#  init_build      the selected build type, e.g. \"maven\", converted to lowercase\n"
                    + "\n"
                    + DEFAULT_PROJECT_NAME_KEY + "=" + DEFAULT_PROJECT_NAME_DEFAULT_VALUE + "\n"
                    + DEFAULT_GROUP_ID_KEY + "=" + DEFAULT_GROUP_ID_DEFAULT_VALUE + "\n"
                    + DEFAULT_ARTIFACT_ID_KEY + "=" + DEFAULT_ARTIFACT_DEFAULT_VALUE + "\n"
                    + DEFAULT_PACKAGE_NAME_KEY + "=" + DEFAULT_PACKAGE_NAME_DEFAULT_VALUE + "\n"
                    + "\n"
                    + "# When using the init command and a project with the same name already exists,\n"
                    + "# this value controls whether it should fail or if the name should be made unique \n"
                    + "# by appending a unique digit, e.g. \"quickstart-se-1\".\n"
                    + "\n"
                    + FAIL_ON_PROJECT_NAME_COLLISION_KEY + "=" + FAIL_ON_PROJECT_NAME_COLLISION_DEFAULT_VALUE + "\n"
                    + "\n"
                    + "# The CLI can use rich text (color, italic, etc.) where supported; setting this\n"
                    + "# value to \"false\" will disable this feature and is equivalent to using the\n"
                    + "# \"--plain\" option on all commands.\n"
                    + "\n"
                    + RICH_TEXT_KEY + "=" + RICH_TEXT_DEFAULT_VALUE + "\n"
                    + "\n"
                    + "# The CLI regularly updates information about new Helidon and/or CLI releases, and\n"
                    + "# this value controls the minimum number of hours between rechecks. Update checks\n"
                    + "# can be forced on every invocation with a zero value or disabled with a negative\n"
                    + "# value.\n"
                    + "\n"
                    + UPDATE_INTERVAL_HOURS_KEY + "=" + UPDATE_INTERVAL_HOURS_DEFAULT_VALUE + "\n"
                    + "\n"
                    + "# The CLI can download new releases to help reduce the number of installation\n"
                    + "# steps, and this value controls whether or not to do so.\n"
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
                    + "# The CLI fetches update information from this location. Setting this may be\n"
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
     * Lookup a property.
     *
     * @param key          property key
     * @param defaultValue default value
     * @return the resolve value, or the supplied default value if not found
     */
    public String property(String key, String defaultValue) {
        return allProperties.getOrDefault(key, defaultValue);
    }

    /**
     * Returns whether or not rich text should be disabled.
     *
     * @return {@code true} if rich text should not be used (equivalent to {@code --plain} option).
     */
    public boolean richTextDisabled() {
        return !Boolean.parseBoolean(property(RICH_TEXT_KEY, RICH_TEXT_DEFAULT_VALUE));
    }

    /**
     * Returns the URL from which to get updates.
     *
     * @return The url.
     */
    public String updateUrl() {
        return property(UPDATE_URL_KEY, UPDATE_URL_DEFAULT_VALUE);
    }

    /**
     * Returns the check for updates interval in hours.
     *
     * @return The interval.
     */
    public int checkForUpdatesIntervalHours() {
        String value = property(UPDATE_INTERVAL_HOURS_KEY, UPDATE_INTERVAL_HOURS_DEFAULT_VALUE);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(UPDATE_INTERVAL_HOURS_KEY + " in " + configFile + ": " + e.getMessage());
        }
    }

    /**
     * Returns the project name to use given the command line arguments, preferring {@code --name}, then {@code --artifactid}
     * and {@link #defaultProjectName(SubstitutionVariables)} if neither are provided.
     *
     * @param nameArg The {@code --name} argument or {@code null} if not provided.
     * @param artifactIdArg The {@code --artifactid} argument or {@code null} if not provided.
     * @param substitutions The substitution variables.
     * @return The project name.
     */
    public String projectName(String nameArg, String artifactIdArg, SubstitutionVariables substitutions) {
        if (nameArg != null) {
            return nameArg;
        } else if (artifactIdArg != null) {
            return artifactIdArg;
        } else {
            return defaultProjectName(substitutions);
        }
    }

    /**
     * Returns the group id to use given the command line arguments, using {@code --groupid} if provided
     * and {@link #defaultGroupId(SubstitutionVariables)} if not.
     *
     * @param groupIdArg The {@code --groupid} argument or {@code null} if not provided.
     * @param substitutions The substitution variables.
     * @return The artifactId.
     */
    public String groupId(String groupIdArg, SubstitutionVariables substitutions) {
        if (groupIdArg != null) {
            return Strings.replace(groupIdArg, Map.of("\\s+", "."));
        } else {
            return defaultGroupId(substitutions);
        }
    }

    /**
     * Returns the artifact id to use given the command line arguments, preferring {@code --artifactid}, then {@code --name}
     * and {@link #defaultArtifactId(SubstitutionVariables)} if neither are provided.
     *
     * @param artifactIdArg The {@code --artifactid} argument or {@code null} if not provided.
     * @param nameArg The {@code --name} argument or {@code null} if not provided.
     * @param substitutions The substitution variables.
     * @return The artifactId.
     */
    public String artifactId(String artifactIdArg, String nameArg, SubstitutionVariables substitutions) {
        if (nameArg != null) {
            return Strings.replace(nameArg, Map.of("\\s+", "-"));
        } else if (artifactIdArg != null) {
            return Strings.replace(artifactIdArg, Map.of("\\s+", "."));
        } else {
            return defaultArtifactId(substitutions);
        }
    }

    /**
     * Returns the package name to use given the command line arguments, using {@code --package} if provided
     * and {@link #defaultPackageName(SubstitutionVariables)} if not.
     *
     * @param packageArg    The {@code --package} argument or {@code null} if not provided.
     * @param substitutions The substitution variables.
     * @return The artifactId.
     */
    public String packageName(String packageArg, SubstitutionVariables substitutions) {
        if (packageArg != null) {
            return Strings.replace(packageArg, Map.of("\\s+", "."));
        } else {
            return defaultPackageName(substitutions);
        }
    }

    /**
     * Returns the default project name using the given substitution variables.
     *
     * @param substitutions The substitution variables.
     * @return The default project name.
     */
    public String defaultProjectName(SubstitutionVariables substitutions) {
        return substitutions.resolve(property(DEFAULT_PROJECT_NAME_KEY, DEFAULT_PROJECT_NAME_DEFAULT_VALUE));
    }

    /**
     * Returns the default group id using the given substitution variables.
     *
     * @param substitutions The substitution variables.
     * @return The default group id.
     */
    public String defaultGroupId(SubstitutionVariables substitutions) {
        String groupId = substitutions.resolve(property(DEFAULT_GROUP_ID_KEY, DEFAULT_GROUP_ID_DEFAULT_VALUE));
        return Strings.replace(groupId, Map.of("\\s+", "."));
    }

    /**
     * Returns the default artifact id using the given substitution variables.
     *
     * @param substitutions The substitution variables.
     * @return The default artifact id.
     */
    public String defaultArtifactId(SubstitutionVariables substitutions) {
        String artifactId = substitutions.resolve(property(DEFAULT_ARTIFACT_ID_KEY, DEFAULT_ARTIFACT_DEFAULT_VALUE));
        return Strings.replace(artifactId, Map.of("\\s+", "."));
    }

    /**
     * Returns the default package name using the given substitution variables.
     *
     * @param substitutions The substitution variables.
     * @return The default package name.
     */
    public String defaultPackageName(SubstitutionVariables substitutions) {
        String result = substitutions.resolve(property(DEFAULT_PACKAGE_NAME_KEY, DEFAULT_PACKAGE_NAME_DEFAULT_VALUE));
        if (result.length() > 0 && Character.isJavaIdentifierStart(result.charAt(0))) {
            for (String name : result.split("\\.")) {
                for (int i = 1; i < name.length(); i++) {
                    if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                        illegalPackageName(result);
                    }
                }
            }
        } else {
            illegalPackageName(result);
        }
        return result;
    }

    private void illegalPackageName(String name) {
        throw new IllegalStateException(DEFAULT_PACKAGE_NAME_KEY + " in " + configFile + " does not resolve to a valid "
                + "package name: " + name);
    }

    /**
     * Returns whether the init command should fail if the project name already exists or if
     * a unique name should be created.
     *
     * @return {@code true} if the init command should fail.
     */
    public boolean failOnProjectNameCollision() {
        return Boolean.parseBoolean(property(FAIL_ON_PROJECT_NAME_COLLISION_KEY, DOWNLOAD_UPDATES_DEFAULT_VALUE));
    }

    /**
     * Returns whether or not updates should be downloaded.
     *
     * @return {@code true} if updates should be downloaded.
     */
    public boolean downloadUpdates() {
        return Boolean.parseBoolean(property(DOWNLOAD_UPDATES_KEY, DOWNLOAD_UPDATES_DEFAULT_VALUE));
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
