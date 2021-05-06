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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * Utilities to access configuration.
 */
public class Config {

    private static final String VERSION_KEY = "version";
    private static final String BUILD_REVISION_KEY = "revision";
    private static final String BUILD_PROPERTIES_PATH = "build.properties";
    private static final AtomicReference<Properties> BUILD_PROPERTIES = new AtomicReference<>();
    private static final AtomicReference<Path> USER_HOME_DIR = new AtomicReference<>();
    private static final AtomicReference<UserConfig> USER_CONFIG = new AtomicReference<>();

    /**
     * Returns the build tools build revision.
     *
     * @return The build revision.
     */
    public static String buildRevision() {
        return requireNonNull(buildProperties().getProperty(BUILD_REVISION_KEY));
    }

    /**
     * Returns the build version.
     *
     * @return The version.
     */
    public static String buildVersion() {
        return requireNonNull(buildProperties().getProperty(VERSION_KEY));
    }

    /**
     * Returns the build properties.
     *
     * @return The properties.
     */
    public static Properties buildProperties() {
        Properties result = BUILD_PROPERTIES.get();
        if (result == null) {
            try {
                InputStream stream = Config.class.getResourceAsStream(BUILD_PROPERTIES_PATH);
                requireNonNull(stream, BUILD_PROPERTIES_PATH + " resource not found");
                try (InputStreamReader reader = new InputStreamReader(stream)) {
                    result = new Properties();
                    result.load(reader);
                    BUILD_PROPERTIES.set(result);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return result;
    }

    /**
     * Returns the user config instance.
     *
     * @return The instance.
     */
    public static UserConfig userConfig() {
        UserConfig config = USER_CONFIG.get();
        if (config == null) {
            if (USER_HOME_DIR.get() == null) {
                config = UserConfig.create();
            } else {
                config = UserConfig.create(USER_HOME_DIR.get());
            }
            USER_CONFIG.set(config);
        }
        return config;
    }

    /**
     * Set the user config instance.
     *
     * @param config user config to set
     */
    public static void setUserConfig(UserConfig config) {
        USER_CONFIG.set(config);
    }

    /**
     * Set the user home dir.
     *
     * @param dir The directory.
     */
    public static void setUserHome(Path dir) {
        USER_HOME_DIR.set(requireNonNull(dir));
    }

    private Config() {
    }
}
