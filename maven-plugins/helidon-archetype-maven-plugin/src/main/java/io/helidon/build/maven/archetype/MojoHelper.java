/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.io.IOException;
import java.util.Properties;

/**
 * Maven mojo helper class.
 */
final class MojoHelper {

    /**
     * The plugin groupId.
     */
    static final String PLUGIN_GROUP_ID = "io.helidon.build-tools";

    /**
     * The plugin artifactId.
     */
    static final String PLUGIN_ARTIFACT_ID = "helidon-archetype-maven-plugin";

    /**
     * The resource name for the {@code pom.properties} file included in the plugin JAR file.
     */
    static final String POM_PROPERTIES_RESOURCE_NAME = "/META-INF/maven/"
            + PLUGIN_GROUP_ID + "/"
            + PLUGIN_ARTIFACT_ID + "/pom.properties";

    /**
     * The plugin version.
     */
    static final String PLUGIN_VERSION = getPluginVersion();

    private MojoHelper() {
    }

    /**
     * Get the plugin version from the maven plugin JAR file.
     *
     * @return version, never {@code null}
     * @throws IllegalStateException if the version is {@code null} or if an IO error occurs
     */
    private static String getPluginVersion() {
        try {
            Properties props = new Properties();
            props.load(JarMojo.class.getResourceAsStream(POM_PROPERTIES_RESOURCE_NAME));
            String version = props.getProperty("version");
            if (version == null) {
                throw new IllegalStateException("Unable to resolve engine version");
            }
            return version;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
