/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
import java.util.Properties;

import io.helidon.build.common.LazyValue;

import static java.util.Objects.requireNonNull;

/**
 * Utilities to access build properties.
 */
public class BuildProperties {

    private static final LazyValue<Properties> BUILD_PROPERTIES = new LazyValue<>(BuildProperties::loadBuildProperties);

    private BuildProperties() {
    }

    /**
     * Returns the build tools build revision.
     *
     * @return The build revision.
     */
    public static String buildRevision() {
        return requireNonNull(buildProperties().getProperty("revision"));
    }

    /**
     * Returns the build version.
     *
     * @return The version.
     */
    public static String buildVersion() {
        return requireNonNull(buildProperties().getProperty("version"));
    }

    /**
     * Returns the build properties.
     *
     * @return The properties.
     */
    public static Properties buildProperties() {
        return BUILD_PROPERTIES.get();
    }

    private static Properties loadBuildProperties() {
        try {
            InputStream stream = BuildProperties.class.getResourceAsStream("build.properties");
            requireNonNull(stream, "build.properties resource not found");
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                Properties result = new Properties();
                result.load(reader);
                return result;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
