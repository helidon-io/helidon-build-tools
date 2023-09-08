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
package io.helidon.build.maven.archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;

import org.apache.maven.model.Plugin;

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

    private static final String MAVEN_ARCHETYPE_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String MAVEN_ARCHETYPE_PLUGIN_ARTIFACT_ID = "maven-archetype-plugin";
    private static final String MAVEN_ARCHETYPE_PLUGIN_VERSION = version(MAVEN_ARCHETYPE_PLUGIN_GROUP_ID,
                                                                         MAVEN_ARCHETYPE_PLUGIN_ARTIFACT_ID);

    /**
     * The Maven archetype plugin coordinates.
     */
    static final Plugin MAVEN_ARCHETYPE_PLUGIN = plugin(MAVEN_ARCHETYPE_PLUGIN_GROUP_ID,
                                                        MAVEN_ARCHETYPE_PLUGIN_ARTIFACT_ID,
                                                        MAVEN_ARCHETYPE_PLUGIN_VERSION);

    private MojoHelper() {
    }

    private static String version(String groupId, String artifactId) {
        try {
            String path = String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
            URL resource = MojoHelper.class.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            try (InputStream is = resource.openStream()) {
                Properties props = new Properties();
                props.load(is);
                String version = props.getProperty("version");
                if (version == null) {
                    throw new IllegalStateException("Unable to resolve engine version");
                }
                return version;
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Plugin plugin(String groupId, String artifactId, String version) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        return plugin;
    }
}
