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
import java.io.UncheckedIOException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Build tools properties.
 */
public class BuildToolsProperties extends Properties {
    private static final Instance<BuildToolsProperties> INSTANCE = new Instance<>(BuildToolsProperties::newInstance);
    private static final String RESOURCE_PATH = "build-tools.properties";
    private static final String VERSION_KEY = "build.version";
    private static final String BUILD_NUMBER_KEY = "build.number";
    private static final String BUILD_DATE_KEY = "build.date";

    /**
     * Returns the instance.
     *
     * @return The instance.
     */
    public static BuildToolsProperties instance() {
        return INSTANCE.instance();
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
     * Returns the build tools build number.
     *
     * @return The build number.
     */
    public String buildNumber() {
        return requireNonNull(getProperty(BUILD_NUMBER_KEY));
    }

    /**
     * Returns the build tools build date.
     *
     * @return The date.
     */
    public String buildDate() {
        return requireNonNull(getProperty(BUILD_DATE_KEY));
    }

    private BuildToolsProperties(InputStream stream) {
        if (stream == null) {
            throw new IllegalStateException(RESOURCE_PATH + " resource not found");
        }
        try (InputStream in = stream) {
            load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BuildToolsProperties newInstance() {
        return new BuildToolsProperties(BuildToolsProperties.class.getResourceAsStream(RESOURCE_PATH));
    }
}
