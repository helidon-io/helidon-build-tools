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
package io.helidon.build.archetype.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Class Maps. Utility class to convert between {@code Properties} and {@code Map}.
 */
public class Maps {

    private Maps() {
    }

    public static Map<String, String> fromProperties(Properties properties) {
        Objects.requireNonNull(properties);
        Map<String, String> map = new HashMap<>();
        properties.forEach((k, v) -> map.put(k.toString(), v.toString()));
        return map;
    }

    public static Properties toProperties(Map<String, String> map) {
        Objects.requireNonNull(map);
        Properties properties = new Properties();
        map.forEach(properties::setProperty);
        return properties;
    }
}
