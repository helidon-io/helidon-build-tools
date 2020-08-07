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
import io.helidon.build.cli.harness.Config;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.util.ProjectConfig.HELIDON_VERSION;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.ProjectConfig.PROJECT_VERSION;

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
            map.put("latest.helidon.version", latest.toString());
        } catch (Exception ignore) {
            // message has already been logged
        }

        Log.info(map);
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
            map.put("project." + key, key.equals(FLAVOR) ? value.toUpperCase() : value);
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
