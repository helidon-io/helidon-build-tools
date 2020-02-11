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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.OutputHelper;

/**
 * The {@code version} command.
 */
@Command(name = "version", description = "Print version information")
final class VersionCommand implements CommandExecution {

    private static final String CLI_VERSION_PROPS_RESOURCE = "version.properties";
    private static final String[] CLI_VERSION_PROP_NAMES = new String[] {"Version", "Revision", "Date"};
    private static final String CLI_KEY_PREFIX = "cli.";

    private final CommonOptions commonOptions;

    @Creator
    VersionCommand(CommonOptions commonOptions) {
        this.commonOptions = commonOptions;
    }

    @Override
    public void execute(CommandContext context) {
        InputStream is = VersionCommand.class.getResourceAsStream(CLI_VERSION_PROPS_RESOURCE);
        if (is == null) {
            throw new IllegalStateException("version.properties resource not found");
        }
        Properties props = new Properties();
        try {
            props.load(is);
            is.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        Map<String, String> versionProps = new LinkedHashMap<>();
        for (int i = 0; i < CLI_VERSION_PROP_NAMES.length; i++) {
            String propName = CLI_VERSION_PROP_NAMES[i];
            String propValue = getVersionProperty(props, CLI_KEY_PREFIX + propName.toLowerCase());
            versionProps.put(propName, propValue);
        }
        context.logInfo(OutputHelper.table(versionProps));
        context.logInfo("\nProject:");
        context.logInfo(String.format("// TODO info from project, project=%s", commonOptions.project()));
    }

    private static String getVersionProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(key + " property not found");
        }
        return value;
    }
}
