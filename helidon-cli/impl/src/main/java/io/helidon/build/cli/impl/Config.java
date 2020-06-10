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

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.util.BuildToolsProperties;
import io.helidon.build.util.UserConfig;

/**
 * Utilities to access configuration.
 */
public class Config {
    private static final AtomicReference<Path> USER_HOME_DIR = new AtomicReference<>();
    private static final AtomicReference<UserConfig> USER_CONFIG = new AtomicReference<>();

    /**
     * Returns the build version.
     *
     * @return The version.
     */
    public static String latestPluginVersion() {
        return buildVersion(); // replace this with metadata!
    }

    /**
     * Returns the build version.
     *
     * @return The version.
     */
    public static String buildVersion() {
        return buildProperties().version();
    }

    /**
     * Returns the build tools properties.
     *
     * @return The properties.
     */
    public static BuildToolsProperties buildProperties() {
        return BuildToolsProperties.instance();
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
                config = Config.userConfig();
            } else {
                config = UserConfig.create(USER_HOME_DIR.get());
            }
            USER_CONFIG.set(config);
        }
        return config;
    }

    /**
     * Set the user home dir.
     *
     * @param dir The directory.
     * @return The directory.
     */
    static Path setUserHome(Path dir) {
        USER_HOME_DIR.set(dir);
        return dir;
    }

    private Config() {
    }
}
