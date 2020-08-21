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
package io.helidon.build.cli.harness;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.util.BuildToolsProperties;

import static java.util.Objects.requireNonNull;

/**
 * Utilities to access configuration.
 */
public class Config {
    private static final AtomicReference<Path> USER_HOME_DIR = new AtomicReference<>();
    private static final AtomicReference<UserConfig> USER_CONFIG = new AtomicReference<>();
    private static final AtomicReference<BuildToolsProperties> BUILD_TOOLS_PROPERTIES = new AtomicReference<>();

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
        BuildToolsProperties result = BUILD_TOOLS_PROPERTIES.get();
        if (result == null) {
            result = BuildToolsProperties.instance();
            BUILD_TOOLS_PROPERTIES.set(result);
        }
        return result;
    }

    /**
     * Returns the user config instance.
     *
     * @return The instance.
     */
    public static UserConfig setUserConfig() {
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

    /**
     * Sets the build tools properties from the given resource path.
     *
     * @param resourcePath The resource path.
     */
    static void setBuildToolsProperties(String resourcePath) {
        BUILD_TOOLS_PROPERTIES.set(BuildToolsProperties.from(resourcePath));
    }

    private Config() {
    }
}
