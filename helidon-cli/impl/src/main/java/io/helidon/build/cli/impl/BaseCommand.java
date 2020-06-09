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

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.util.AnsiConsoleInstaller;
import io.helidon.build.util.ProjectConfig;

import static io.helidon.build.util.FileUtils.WORKING_DIR;
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.util.ProjectConfig.ensureProjectConfig;
import static io.helidon.build.util.Style.BoldBlue;
import static io.helidon.build.util.Style.Italic;

/**
 * Class BaseCommand.
 */
public abstract class BaseCommand {

    static final String HELIDON_VERSION_PROPERTY = "helidon.version";

    private ProjectConfig projectConfig;
    private Path projectDir;

    protected BaseCommand() {
        AnsiConsoleInstaller.ensureInstalled();
    }

    protected ProjectConfig projectConfig(Path dir) {
        if (projectConfig != null && dir.equals(projectDir)) {
            return projectConfig;
        }
        File dotHelidon = dir.resolve(DOT_HELIDON).toFile();
        projectConfig = new ProjectConfig(dotHelidon);
        projectDir = dir;
        return projectConfig;
    }

    protected void requireValidProjectConfig() {
        ensureProjectConfig(WORKING_DIR, null);
    }

    private static final String SPACES = "                                                        ";

    protected static String formatMapAsYaml(String top, Map<String, Object> map) {
        Map<String, Object> topLevel = new LinkedHashMap<>();
        topLevel.put(top, map);
        String yaml = formatMapAsYaml(topLevel, 0);
        return yaml.substring(0, yaml.length() - 1);        // remove last \n
    }

    @SuppressWarnings("unchecked")
    private static String formatMapAsYaml(Map<String, Object> map, int level) {
        int maxLen = 0;
        for (String key : map.keySet()) {
            final int len = key.length();
            if (len > maxLen) {
                maxLen = len;
            }
        }
        final int labelWidth = maxLen;

        StringBuilder builder = new StringBuilder();
        map.forEach((key, v) -> {
            int padding = labelWidth - key.length();
            builder.append(SPACES, 0, (2 * level) + padding);
            if (v instanceof Map<?, ?>) {
                builder.append(Italic.apply(key)).append(":\n");
                builder.append(formatMapAsYaml((Map<String, Object>) v, level + 1));
            } else if (v instanceof List<?>) {
                List<String> l = (List<String>) v;
                if (l.size() > 0) {
                    builder.append(Italic.apply(key)).append(":");
                    l.forEach(s -> builder.append("\n")
                            .append(SPACES, 0, 2 * (level + 1))
                            .append("- ").append(BoldBlue.apply(s)));
                    builder.append("\n");
                }
            } else if (v != null) {     // ignore key if value is null
                builder.append(Italic.apply(key)).append(":").append(" ")
                        .append(BoldBlue.apply(v.toString())).append("\n");
            }
        });
        return builder.toString();
    }
}
