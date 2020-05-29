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

import io.helidon.build.util.BuildToolsProperties;

/**
 * Utilities to access configuration.
 */
public class Config {

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

    private Config() {
    }
}
