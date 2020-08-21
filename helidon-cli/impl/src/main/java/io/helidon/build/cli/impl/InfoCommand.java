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

import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.Config;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.TimeUtils;

import static io.helidon.build.cli.impl.VersionCommand.addProjectProperty;
import static io.helidon.build.util.ProjectConfig.HELIDON_VERSION;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.util.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_VERSION;

/**
 * The {@code info} command.
 */
@Command(name = "info", description = "Print project information")
public final class InfoCommand extends BaseCommand {
    private static final int MIN_WIDTH = "plugin.build.revision".length();

    private final boolean verbose;
    private final boolean plain;

    @Creator
    InfoCommand(CommonOptions commonOptions) {
        super(commonOptions, true);
        this.verbose = commonOptions.verbose();
        this.plain = commonOptions.plain();
    }

    @Override
    protected void assertPreconditions() {
    }

    @Override
    protected void invoke(CommandContext context) throws Exception {

        // User config

        Map<Object, Object> userConfigProps = new LinkedHashMap<>();
        Map<String, String> properties = Config.userConfig().properties();
        properties.keySet().stream().sorted().forEach(key -> userConfigProps.put(key, properties.get(key)));

        // Build properties

        Map<Object, Object> buildProps = new LinkedHashMap<>();
        VersionCommand.addBuildProperties(buildProps);

        // System properties

        Map<Object, Object> systemProps = new LinkedHashMap<>();
        if (verbose) {
            System.getProperties().keySet().stream().sorted().forEach(key -> {
                String name = key.toString();
                String value = System.getProperty(name);
                value = value.replace("\n", "\\n");
                value = value.replace("\r", "\\r");
                value = value.replace("\b", "\\b");
                systemProps.put(key.toString(), value);
            });
        }

        // Env vars

        Map<Object, Object> envVars = new LinkedHashMap<>();
        if (verbose) {
            System.getenv().keySet().stream().sorted().forEach(key -> {
                String value = System.getenv(key);
                envVars.put(key, value);
            });
        }

        // Metadata

        Map<Object, Object> metaProps = new LinkedHashMap<>();
        if (verbose) {
            try {
                Metadata meta = metadata();
                metaProps.put("cache.dir", meta.rootDir());

                FileTime lastUpdateTime = meta.lastUpdateTime();
                String formattedTime = TimeUtils.toDateTime(lastUpdateTime);
                metaProps.put("last.update.time", formattedTime);

                MavenVersion latestVersion = meta.latestVersion(true);
                metaProps.put("latest.version", latestVersion.toString());

                ConfigProperties props = meta.propertiesOf(latestVersion);
                props.keySet().stream().sorted().forEach(key -> metaProps.put(key, props.property(key)));

                ArchetypeCatalog catalog = meta.catalogOf(latestVersion);
                AtomicInteger counter = new AtomicInteger(0);
                catalog.entries().forEach(e -> {
                    String prefix = "archetype." + counter.incrementAndGet();
                    metaProps.put(prefix + ".artifactId", e.artifactId());
                    metaProps.put(prefix + ".version", e.version());
                    metaProps.put(prefix + ".title", e.summary());
                    metaProps.put(prefix + ".name", e.name());
                    metaProps.put(prefix + ".tags", toString(e.tags()));
                });
            } catch (Exception ignore) {
                // message has already been logged
            }
        }

        // Project config

        Map<Object, Object> projectProps = new LinkedHashMap<>();
        ProjectConfig projectConfig = projectConfig();
        if (projectConfig.exists()) {
            addProjectProperty("version", PROJECT_VERSION, projectConfig, projectProps);
            addProjectProperty("helidon.version", HELIDON_VERSION, projectConfig, projectProps);
            addProjectProperty("flavor", PROJECT_FLAVOR, projectConfig, projectProps);
            addProjectProperty("directory", PROJECT_DIRECTORY, projectConfig, projectProps);
            addProjectProperty("version", PROJECT_VERSION, projectConfig, projectProps);
            addProjectProperty("main.class", PROJECT_MAINCLASS, projectConfig, projectProps);
            addProjectProperty("source.dirs", PROJECT_SOURCEDIRS, projectConfig, projectProps);
            addProjectProperty("classes.dirs", PROJECT_CLASSDIRS, projectConfig, projectProps);
            addProjectProperty("resource.dirs", PROJECT_RESOURCEDIRS, projectConfig, projectProps);
        }

        // Log them all

        int maxWidth = Math.max(Log.maxKeyWidth(userConfigProps, buildProps, systemProps, envVars, projectProps), MIN_WIDTH);
        log("User Config", userConfigProps, maxWidth);
        log("Project Config", projectProps, maxWidth);
        log("General", buildProps, maxWidth);
        Plugins.execute("GetInfo", pluginArgs(maxWidth), 5, Log::info);
        log("Metadata", metaProps, maxWidth);
        log("System Properties", systemProps, maxWidth);
        log("Environment Variables", envVars, maxWidth);

        if (!verbose) {
            Log.info("%nRun 'helidon info --verbose' for more detail.");
        }
    }

    private List<String> pluginArgs(int maxWidth) {
        List<String> args = new ArrayList<>();
        args.add("--maxWidth");
        args.add(Integer.toString(maxWidth));
        if (plain) {
            args.add("--plain");
        }
        return args;
    }

    private String toString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        list.forEach(entry -> {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry);
        });
        return sb.toString();
    }

    private static void logHeader(String header) {
        Log.info();
        Log.info("$(bold | %s)", header);
        Log.info();
    }

    private static void log(String header, Map<Object, Object> map, int maxKeyWidth) {
        if (!map.isEmpty()) {
            logHeader(header);
            Log.info(map, maxKeyWidth);
        }
    }
}
