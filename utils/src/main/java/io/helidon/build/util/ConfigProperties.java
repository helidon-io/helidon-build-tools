/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class ConfigProperties.
 */
public class ConfigProperties {

    private static final String DELIMITER = ",";

    private final Path file;
    private final Properties properties;

    /**
     * Constructor from file.
     *
     * @param file The file.
     */
    public ConfigProperties(Path file) {
        this.file = file;
        this.properties = new Properties();
        load();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigProperties)) return false;
        final ConfigProperties that = (ConfigProperties) o;
        return Objects.equals(file, that.file)
               && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, properties);
    }

    /**
     * Returns the properties file.
     *
     * @return The file.
     */
    public Path file() {
        return file;
    }

    /**
     * Checks if properties file exists.
     *
     * @return Outcome of test.
     */
    public boolean exists() {
        return Files.exists(file) && Files.isRegularFile(file);
    }

    /**
     * Get a property.
     *
     * @param key The key.
     * @return The value or {@code null}.
     */
    public String property(String key) {
        return properties.getProperty(key);
    }

    /**
     * Set a property.
     *
     * @param key The key.
     * @param value The value.
     */
    public void property(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Get property as string list using {@link #DELIMITER}.
     *
     * @param key The key.
     * @return List of strings, possibly empty.
     */
    public List<String> propertyAsList(String key) {
        String value = properties.getProperty(key);
        if (Strings.isNotValid(value)) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(value.split(DELIMITER))
                         .filter(v -> !v.isEmpty())
                         .collect(Collectors.toList());
        }
    }

    /**
     * Set property using string collection and {@link #DELIMITER}.
     *
     * @param key The key.
     * @param values The strings.
     */
    public void property(String key, Collection<String> values) {
        properties.setProperty(key, String.join(DELIMITER, values));
    }

    /**
     * Check if property exists.
     *
     * @param key The key.
     * @return Outcome of test.
     */
    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    /**
     * Removes property.
     *
     * @param key The key.
     * @return {@code true} if present and removed.
     */
    public boolean remove(String key) {
        return properties.remove(key) != null;
    }

    /**
     * Returns set of keys as strings.
     *
     * @return Set of keys.
     */
    @SuppressWarnings("unchecked")
    public Set<String> keySet() {
        return (Set<String>) (Set<?>) properties.keySet();
    }

    /**
     * Returns set of entries.
     *
     * @return Set of entries.
     */
    @SuppressWarnings("unchecked")
    public Set<Map.Entry<String, String>> entrySet() {
        return (Set<Map.Entry<String, String>>) (Set<?>) properties.entrySet();
    }

    /**
     * Load properties from file.
     */
    public void load() {
        if (exists()) {
            try {
                try (FileReader reader = new FileReader(file.toFile())) {
                    properties.load(reader);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Stores properties to file.
     */
    public void store() {
        try {
            try (FileWriter writer = new FileWriter(file.toFile())) {
                properties.store(writer, "Helidon Project Configuration");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
