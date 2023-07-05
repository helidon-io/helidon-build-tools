/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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

import io.helidon.build.cli.common.ProjectConfig;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.cli.common.ProjectConfig.HELIDON_VERSION;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_VERSION;
import static io.helidon.build.cli.common.ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_SEPARATOR;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;

/**
 * The {@code version} command.
 */
@Command(name = "version", description = "Print version information")
final class VersionCommand extends BaseCommand {
    private static final String FLAVOR = "flavor";

    @Creator
    VersionCommand(CommonOptions commonOptions) {
        super(commonOptions, true);
    }

    @Override
    protected void assertPreconditions() {
    }

    @Override
    protected void invoke(CommandContext context) {
        Map<Object, Object> map = new LinkedHashMap<>();
        addBuildProperties(map);

        ProjectConfig projectConfig = projectConfig();
        if (projectConfig.exists()) {
            addProjectProperty("version", PROJECT_VERSION, projectConfig, map);
            addProjectProperty("helidon.version", HELIDON_VERSION, projectConfig, map);
            addProjectProperty("flavor", PROJECT_FLAVOR, projectConfig, map);
        }

        try {
            MavenVersion latest = metadata().latestVersion(true);
            MavenVersion defaultVersion = metadata().defaultVersion(true);
            map.put("latest.helidon.version", latest.toString());
            map.put("default.helidon.version", defaultVersion.toString());
        } catch (Exception ignore) {
            // message has already been logged
        }

        Log.log(LogLevel.INFO, map, Italic, BoldBlue);
    }

    /**
     * Add a project property, if available.
     *
     * @param key The property.
     * @param configKey The property name in the project config.
     * @param config The project config.
     * @param map The map to add to.
     */
    static void addProjectProperty(String key, String configKey, ProjectConfig config, Map<Object, Object> map) {
        String value = config.property(configKey);
        if (value != null) {
            if (key.equals(FLAVOR)) {
                value = value.toUpperCase();
            } else if (key.equals(PROJECT_RESOURCEDIRS)) {
                value = value.split(RESOURCE_INCLUDE_EXCLUDE_SEPARATOR)[0];
            }
            map.put("project." + key, value);
        }
    }

    /**
     * Add the build properties.
     *
     * @param map The map to add to.
     */
    static void addBuildProperties(Map<Object, Object> map) {
        Config.buildProperties().forEach((k, v) -> map.put("build." + k, v.toString()));
    }
}
