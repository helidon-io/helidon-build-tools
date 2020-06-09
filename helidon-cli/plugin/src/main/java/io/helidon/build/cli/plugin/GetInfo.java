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
package io.helidon.build.cli.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static io.helidon.build.cli.plugin.Style.boldBlue;
import static io.helidon.build.cli.plugin.Style.italic;
import static java.util.Objects.requireNonNull;

/**
 * An extension that logs build info.
 */
public class GetInfo extends Plugin {
    private static final String BUILD_PROPERTIES_PATH = "build.properties";
    private static final AtomicReference<Properties> BUILD_PROPERTIES = new AtomicReference<>();
    private static final String BUILD_PREFIX = "build";

    @Override
    void execute() {
        final Map<String, String> info = new LinkedHashMap<>();
        buildProperties().forEach((key, value) -> {
            final String name = key.toString();
            final String label = BUILD_PREFIX + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            info.put(label, value.toString());
        });
        info.put("JRE", Runtime.version().toString());
        info.put("OS", System.getProperty("os.name", "<unknown>"));
        log(info);
    }

    private void log(Map<String, String> info) {
        int maxLen = 0;
        for (String key : info.keySet()) {
            final int len = key.length();
            if (len > maxLen) {
                maxLen = len;
            }
        }
        final String labelFormat = "%" + maxLen + "s";
        Log.info("plugin:");
        info.forEach((key, value) -> {
            final String paddedLabel = String.format(labelFormat, key);
            Log.info("  %s: %s", italic(paddedLabel), boldBlue(value));
        });
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
                InputStream stream = GetInfo.class.getResourceAsStream(BUILD_PROPERTIES_PATH);
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
}
