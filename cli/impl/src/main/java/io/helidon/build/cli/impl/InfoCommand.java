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

import java.io.PrintStream;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;
import io.helidon.build.cli.common.ProjectConfig;
import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.common.ConfigProperties;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.Strings;
import io.helidon.build.common.Time;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.cli.common.ProjectConfig.HELIDON_VERSION;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_DIRECTORY;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_FLAVOR;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.cli.common.ProjectConfig.PROJECT_VERSION;
import static io.helidon.build.cli.impl.Metadata.HELIDON_3;
import static io.helidon.build.cli.impl.UserConfig.DEFAULT_PROJECT_NAME_KEY;
import static io.helidon.build.cli.impl.VersionCommand.addProjectProperty;
import static io.helidon.build.common.PrintStreams.DEVNULL;
import static io.helidon.build.common.Strings.padding;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;
import static io.helidon.build.common.logging.Log.maxKeyWidth;

/**
 * The {@code info} command.
 */
@Command(name = "info", description = "Print project information")
public final class InfoCommand extends BaseCommand {
    private static final int MIN_WIDTH = "plugin.build.revision".length();
    private static final String EOL = System.getProperty("line.separator");
    private static final int DEFAULT_BUILDER_SIZE = 2048;
    private static final int VERBOSE_BUILDER_SIZE = 16384;
    private static final String EOL_MARKER = "~@~";
    private static final String PAD = " ";

    @Creator
    InfoCommand(CommonOptions commonOptions) {
        super(commonOptions, true);
    }

    @Override
    protected void assertPreconditions() {
    }

    @Override
    protected void invoke(CommandContext context) {
        boolean verbose = context.globalOptions().verbose();
        boolean plain = context.globalOptions().plain();
        StringBuilder builder = new StringBuilder(verbose ? VERBOSE_BUILDER_SIZE : DEFAULT_BUILDER_SIZE);

        // User config

        Map<Object, Object> userConfigProps = new LinkedHashMap<>();
        Map<String, String> properties = Config.userConfig().properties();
        properties.keySet()
                  .stream()
                  .filter(key -> !key.equals(DEFAULT_PROJECT_NAME_KEY))
                  .sorted()
                  .forEach(key -> userConfigProps.put(key, properties.get(key)));

        // Build properties

        Map<Object, Object> buildProps = new LinkedHashMap<>();
        VersionCommand.addBuildProperties(buildProps);

        // System properties

        Map<Object, Object> systemProps = new LinkedHashMap<>();
        if (verbose) {
            System.getProperties().keySet().stream().sorted().forEach(key -> {
                String name = key.toString();
                String value = System.getProperty(name);
                value = Strings.replaceAll(value, "\n", "\\n", "\r", "\\r", "\b", "\\b");
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
                Metadata metadata = metadata();
                metaProps.put("cache.dir", metadata.rootDir());

                FileTime lastUpdateTime = metadata.lastUpdateTime();
                String formattedTime = Time.toDateTime(lastUpdateTime);
                metaProps.put("last.update.time", formattedTime);

                MavenVersion latestVersion = metadata.latestVersion(true);
                metaProps.put("latest.version", latestVersion.toString());

                ConfigProperties props = metadata.propertiesOf(latestVersion);
                props.keySet().stream().sorted().forEach(key -> metaProps.put(key, props.property(key)));

                if (latestVersion.isLessThan(HELIDON_3)) {
                    ArchetypeCatalog catalog = metadata.catalogOf(latestVersion);
                    AtomicInteger counter = new AtomicInteger(0);
                    catalog.entries().forEach(e -> {
                        String prefix = "archetype." + counter.incrementAndGet();
                        metaProps.put(prefix + ".artifactId", e.artifactId());
                        metaProps.put(prefix + ".version", e.version());
                        metaProps.put(prefix + ".title", e.summary());
                        metaProps.put(prefix + ".name", e.name());
                        metaProps.put(prefix + ".tags", toString(e.tags()));
                    });
                }
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

        int maxWidth = Math.max(maxKeyWidth(userConfigProps, buildProps, systemProps, envVars, projectProps), MIN_WIDTH);
        append(builder, "User Config", userConfigProps, maxWidth);
        append(builder, "Project Config", projectProps, maxWidth);
        append(builder, "General", buildProps, maxWidth);

        try {
            PrintStream stdOut = PrintStreams.delegate(DEVNULL, (p, s) -> builder.append(s.replace(EOL_MARKER, EOL)));
            Plugins.execute("GetInfo", pluginArgs(maxWidth, plain), 5, stdOut);
        } catch (Plugins.PluginFailed e) {
            Log.error(e, "Unable to get system info");
        }
        append(builder, "Metadata", metaProps, maxWidth);
        append(builder, "System Properties", systemProps, maxWidth);
        append(builder, "Environment Variables", envVars, maxWidth);

        if (!verbose) {
            appendLine(builder, "%nRun 'helidon info --verbose' for more detail.");
        }
        Log.info(builder.toString());
    }

    private List<String> pluginArgs(int maxWidth, boolean plain) {
        List<String> args = new ArrayList<>();
        args.add("--maxWidth");
        args.add(Integer.toString(maxWidth));
        if (plain) {
            args.add("--plain");
        }
        return args;
    }

    private static String toString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        list.forEach(entry -> {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry);
        });
        return sb.toString();
    }

    private void appendLine(StringBuilder builder, String line, Object... args) {
        builder.append(String.format(line, args));
        appendLine(builder);
    }

    private void appendLine(StringBuilder builder) {
        builder.append(EOL);
    }

    private void append(StringBuilder builder, String header, Map<Object, Object> map, int maxKeyWidth) {
        if (!map.isEmpty()) {
            appendLine(builder);
            appendLine(builder, "$(bold | %s)", header);
            appendLine(builder);
            map.forEach((key, value) -> {
                key = key.toString().replace("%", "%%");
                value = value.toString().replace("%", "%%");
                String padding = padding(PAD, maxKeyWidth, key.toString());
                appendLine(builder, "%s %s %s", Italic.apply(key), padding, BoldBlue.apply(value));
            });
        }
    }
}
