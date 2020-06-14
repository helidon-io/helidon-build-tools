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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Build tools properties.
 */
public class BuildToolsProperties extends Properties {
    private static final Instance<BuildToolsProperties> INSTANCE = new Instance<>(BuildToolsProperties::newInstance);
    private static final String RESOURCE_PATH = "build-tools.properties";
    private static final String VERSION_KEY = "version";
    private static final String BUILD_REVISION_KEY = "revision";
    private static final String BUILD_DATE_KEY = "date";

    /**
     * Returns the instance.
     *
     * @return The instance.
     */
    public static BuildToolsProperties instance() {
        return INSTANCE.instance();
    }

    /**
     * Returns a new instance using the given resource path.
     *
     * @param resourcePath The resource path.
     * @return The instance.
     */
    public static BuildToolsProperties from(String resourcePath) {
        return new BuildToolsProperties(requireNonNull(resourcePath));
    }

    /**
     * Returns the build tools version.
     *
     * @return The version.
     */
    public String version() {
        return requireNonNull(getProperty(VERSION_KEY));
    }

    /**
     * Returns the build tools build revision.
     *
     * @return The build revision.
     */
    public String buildRevision() {
        return requireNonNull(getProperty(BUILD_REVISION_KEY));
    }

    /**
     * Returns the build tools build date.
     *
     * @return The date.
     */
    public String buildDate() {
        return requireNonNull(getProperty(BUILD_DATE_KEY));
    }

    private BuildToolsProperties(String resourcePath) {
        try {
            InputStream stream = BuildToolsProperties.class.getResourceAsStream(resourcePath);
            Requirements.requireNonNull(stream, "%s resource not found", resourcePath);
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                load(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BuildToolsProperties newInstance() {
        return new BuildToolsProperties(RESOURCE_PATH);
    }
}
