/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.cli.common;

/**
 * Helidon Cli properties.
 */
public abstract class CliProperties {

    private CliProperties() {
    }

    /**
     * Property that must be set to "true" for cli extensions to be enabled.
     */
    public static final String HELIDON_CLI_PROPERTY = "helidon.cli";

    /**
     * The command line argument to enable cli extensions.
     */
    public static final String ENABLE_HELIDON_CLI = "-D" + HELIDON_CLI_PROPERTY + "=true";

    /**
     * The Helidon plugin version property name.
     */
    public static final String HELIDON_PLUGIN_VERSION_PROPERTY = "version.helidon.plugin";

    /**
     * The Helidon cli plugin version property name.
     */
    public static final String HELIDON_CLI_PLUGIN_VERSION_PROPERTY = "version.plugin.helidon-cli";

    /**
     * The Helidon version property name.
     */
    public static final String HELIDON_VERSION_PROPERTY = "helidon.version";
}
