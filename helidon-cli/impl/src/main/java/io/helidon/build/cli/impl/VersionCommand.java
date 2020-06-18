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

import java.util.LinkedHashMap;
import java.util.Map;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.util.ProjectConfig.HELIDON_VERSION;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.ProjectConfig.PROJECT_VERSION;
import static io.helidon.build.util.Style.BoldBlue;
import static io.helidon.build.util.Style.Italic;

/**
 * The {@code version} command.
 */
@Command(name = "version", description = "Print version information")
final class VersionCommand extends BaseCommand implements CommandExecution {
    private static final String FLAVOR = "flavor";
    private static final String PAD = " ";
    private final CommonOptions commonOptions;

    @Creator
    VersionCommand(CommonOptions commonOptions) {
        this.commonOptions = commonOptions;
    }

    @Override
    public void execute(CommandContext context) {
        Map<String, String> map = new LinkedHashMap<>();
        addBuildProperties(map);

        ProjectConfig projectConfig = projectConfig(commonOptions);
        if (projectConfig.exists()) {
            addProjectProperty("version", PROJECT_VERSION, projectConfig, map);
            addProjectProperty("helidon.version", HELIDON_VERSION, projectConfig, map);
            addProjectProperty("flavor", PROJECT_FLAVOR, projectConfig, map);
        }

        log(map, maxKeyWidth(map));
    }

    /**
     * Add a project property, if available.
     *
     * @param key The property.
     * @param configKey The property name in the project config.
     * @param config The project config.
     * @param map The map to add to.
     */
    static void addProjectProperty(String key, String configKey, ProjectConfig config, Map<String, String> map) {
        String value = config.property(configKey);
        if (value != null) {
            map.put("project." + key, key.equals(FLAVOR) ? value.toUpperCase() : value);
        }
    }

    /**
     * Add the build properties.
     *
     * @param map The map to add to.
     */
    static void addBuildProperties(Map<String, String> map) {
        Config.buildProperties().forEach((k, v) -> map.put("build." + k, v.toString()));
    }

    /**
     * Log the entries, if not empty.
     *
     * @param map The entries.
     * @param maxKeyWidth The maximum key width.
     */
    static void log(Map<String, String> map, int maxKeyWidth) {
        if (!map.isEmpty()) {
            map.forEach((key, value) -> {
                final String padding = padding(maxKeyWidth, key);
                Log.info("%s %s %s", Italic.apply(key), padding, BoldBlue.apply(value));
            });
        }
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

    /**
     * Returns the maximum key width.
     *
     * @param maps The maps.
     * @return The max key width.
     */
    @SafeVarargs
    static int maxKeyWidth(Map<String, String>... maps) {
        int maxLen = 0;
        for (Map<String, String> map : maps) {
            for (String key : map.keySet()) {
                final int len = key.length();
                if (len > maxLen) {
                    maxLen = len;
                }
            }
        }
        return maxLen;
    }
}
