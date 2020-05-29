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

/**
 * The {@code version} command.
 */
@Command(name = "version", description = "Print version information")
final class VersionCommand extends BaseCommand implements CommandExecution {
    private static final String FLAVOR = "flavor";
    private final CommonOptions commonOptions;

    @Creator
    VersionCommand(CommonOptions commonOptions) {
        this.commonOptions = commonOptions;
    }

    @Override
    public void execute(CommandContext context) {
        logBuildProperties();

        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());
        if (projectConfig.exists()) {
            Map<String, Object> projectProps = new LinkedHashMap<>();
            addProjectProperty("version", PROJECT_VERSION, projectConfig, projectProps);
            addProjectProperty("helidon", HELIDON_VERSION, projectConfig, projectProps);
            addProjectProperty("flavor", PROJECT_FLAVOR, projectConfig, projectProps);
            log("project", projectProps);
        }
    }

    /**
     * Add a project property, if available.
     *
     * @param key The property.
     * @param configKey The property name in the project config.
     * @param config The project config.
     * @param map The map to add to.
     */
    static void addProjectProperty(String key, String configKey, ProjectConfig config, Map<String, Object> map) {
        String value = config.property(configKey);
        if (value != null) {
            map.put(key, key.equals(FLAVOR) ? value.toUpperCase() : value);
        }
    }

    /**
     * Log the build properties.
     */
    static void logBuildProperties() {
        Map<String, Object> buildProps = new LinkedHashMap<>();
        Config.buildProperties().forEach((k, v) -> buildProps.put((String) k, v));
        log("build", buildProps);
    }

    /**
     * Log the properties under the given name, if not empty.
     *
     * @param name The name.
     * @param map The properties.
     */
    static void log(String name, Map<String, Object> map) {
        if (!map.isEmpty()) {
            Log.info(formatMapAsYaml(name, map));
        }
    }
}
