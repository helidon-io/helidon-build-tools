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
 * A plugin that logs build info.
 */
public class GetInfo extends Plugin {
    private static final String BUILD_PROPERTIES_PATH = "build.properties";
    private static final AtomicReference<Properties> BUILD_PROPERTIES = new AtomicReference<>();
    private static final String BUILD_PREFIX = "plugin.build.";
    private static final String MAX_WIDTH_ARG = "--maxWidth";
    private static final String PAD = " ";

    private final Map<String, String> info;
    private int maxWidth;

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

    /**
     * Constructor.
     */
    public GetInfo() {
        info = new LinkedHashMap<>();
    }

    @Override
    int parseArg(String arg, int argIndex, String[] allArgs) {
        if (arg.equals(MAX_WIDTH_ARG)) {
            maxWidth = Integer.parseInt(nextArg(argIndex, allArgs));
            return argIndex + 1;
        }
        return -1;
    }

    @Override
    void validateArgs() {
        if (maxWidth == 0) {
            missingRequiredArg(MAX_WIDTH_ARG);
        }
    }

    @Override
    void execute() {
        buildProperties().forEach((key, value) -> info.put(BUILD_PREFIX + key, value.toString()));
        addSystemProperty("os.name", false);
        addSystemProperty("os.version", false);
        addSystemProperty("os.arch", false);
        addSystemProperty("java.version", false);
        addSystemProperty("java.vm.name", false);
        addSystemProperty("java.home", true);
        addSystemProperty("user.home", true);
        log(info);
    }

    private void addSystemProperty(String name, boolean verboseOnly) {
        if (Log.isVerbose() || !verboseOnly) {
            info.put(name, System.getProperty(name));
        }
    }

    private void log(Map<String, String> info) {
        info.keySet().stream().sorted().forEach(key -> {
            final String padding = padding(maxWidth, key);
            final String value = info.get(key).replace(")", "\\)");
            Log.info("%s %s %s", italic(key), padding, boldBlue(value));
        });
    }

    /**
     * Returns a padding string.
     *
     * @param maxKeyWidth The maximum key width.
     * @param key The key.
     * @return The padding.
     */
    static String padding(int maxKeyWidth, String key) {
        final int keyLen = key.length();
        if (maxKeyWidth > keyLen) {
            return PAD.repeat(maxKeyWidth - keyLen);
        } else {
            return "";
        }
    }
}
