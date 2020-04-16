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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Class ConfigProperties.
 */
public class ConfigProperties {

    private static final String DELIMITER = ",";

    private File file;
    private Properties properties;

    /**
     * Constructor from file name.
     *
     * @param fileName The file's name.
     */
    public ConfigProperties(String fileName) {
        this(new File(fileName));
    }

    /**
     * Constructor from file.
     *
     * @param file The file.
     */
    public ConfigProperties(File file) {
        this.file = file;
        this.properties = new Properties();
        load();
    }

    /**
     * Checks if properties file exists.
     *
     * @return Outcome of test.
     */
    public boolean exists() {
        return file.exists();
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
        return value != null ? Arrays.asList(value.split(DELIMITER)) : Collections.emptyList();
    }

    /**
     * Set property using string list and {@link #DELIMITER}.
     *
     * @param key The key.
     * @param values The string list.
     */
    public void property(String key, List<String> values) {
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
        if (file.exists()) {
            try {
                try (FileReader reader = new FileReader(file)) {
                    properties.load(reader);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Stores properties to file.
     */
    public void store() {
        try {
            try (FileWriter writer = new FileWriter(file)) {
                properties.store(writer, getClass().getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
