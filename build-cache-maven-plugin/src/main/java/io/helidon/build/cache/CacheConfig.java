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
package io.helidon.build.cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Cache config.
 */
final class CacheConfig {

    private static final String GROUPID = "io.helidon.build-tools";
    private static final String ARTIFACTID = "helidon-build-cache-maven-plugin";
    private static final String POM_PROPERTIES = "META-INF/maven/" + GROUPID + "/" + ARTIFACTID + "/pom.properties";
    private static String version;
    private static final Properties EMPTY_PROPS = new Properties();
    private static final CacheConfig EMPTY_CONFIG = new CacheConfig(null, null);
    private static final WeakHashMap<MavenProject, CacheConfig> CONFIG_CACHE = new WeakHashMap<>();

    private final List<String> executionsExcludes;
    private final List<String> executionsIncludes;
    private final List<String> projectFilesExcludes;
    private final List<String> buildFilesExcludes;
    private final boolean enableChecksums;
    private final boolean includeAllChecksums;
    private final Path archiveFile;
    private final boolean skip;
    private final boolean createArchive;
    private final boolean loadArchive;

    // TODO cache.executionsRules
    //          source as list of patterns to match against the executions
    //          sourceStatuses: list of statuses (NEW|CACHED|DIFF)
    //          target as list of patterns to match against the executions
    //          targetStatus: new value for the status
    private CacheConfig(Xpp3Dom config, MavenSession session) {
        Properties sysProps;
        Properties userProps;
        if (session == null) {
            sysProps = System.getProperties();
            userProps = EMPTY_PROPS;
        } else {
            sysProps = session.getSystemProperties();
            userProps = session.getUserProperties();
        }
        List<String> executionsExcludes = stringListProperty(sysProps, userProps, "cache.executionsExcludes");
        List<String> executionsIncludes = stringListProperty(sysProps, userProps, "cache.executionsIncludes");
        List<String> projectFilesExcludes = stringListProperty(sysProps, userProps, "cache.projectFilesExcludes");
        List<String> buildFilesExcludes = stringListProperty(sysProps, userProps, "cache.buildFilesExcludes");
        Path archiveFile = pathProperty(sysProps, userProps, "cache.archiveFile");
        Boolean enableChecksums = booleanProperty(sysProps, userProps, "cache.enableChecksums");
        Boolean includeAllChecksums = booleanProperty(sysProps, userProps, "cache.includeAllChecksums");
        Boolean skip = booleanProperty(sysProps, userProps, "cache.skip");
        Boolean createArchive = booleanProperty(sysProps, userProps, "cache.createArchive");
        Boolean loadArchive = booleanProperty(sysProps, userProps, "cache.loadArchive");
        if (config != null) {
            if (executionsIncludes == null) {
                executionsIncludes = stringListElement(config, "executionsIncludes");
            }
            if (executionsExcludes == null) {
                executionsExcludes = stringListElement(config, "executionsExcludes");
            }
            if (projectFilesExcludes == null) {
                projectFilesExcludes = stringListElement(config, "projectFilesExcludes");
            }
            if (buildFilesExcludes == null) {
                buildFilesExcludes = stringListElement(config, "buildFilesExcludes");
            }
            if (archiveFile == null) {
                archiveFile = pathElement(config, "archiveFile");
            }
            if (enableChecksums == null) {
                enableChecksums = booleanElement(config, "enableChecksums");
            }
            if (includeAllChecksums == null) {
                includeAllChecksums = booleanElement(config, "includeAllChecksums");
            }
            if (createArchive == null) {
                createArchive = booleanElement(config, "createArchive");
            }
            if (loadArchive == null) {
                loadArchive = booleanElement(config, "loadArchive");
            }
            if (skip == null) {
                skip = booleanElement(config, "skip");
            }
        }
        this.enableChecksums = enableChecksums != null && enableChecksums;
        this.includeAllChecksums = includeAllChecksums != null && includeAllChecksums;
        this.skip = skip != null && skip;
        this.archiveFile = archiveFile;
        this.createArchive = createArchive != null && createArchive;
        this.loadArchive = loadArchive != null && loadArchive;
        this.executionsExcludes = executionsExcludes != null ? executionsExcludes : List.of();
        this.executionsIncludes = executionsIncludes != null ? executionsIncludes : List.of();
        this.projectFilesExcludes = projectFilesExcludes != null ? projectFilesExcludes : List.of();
        this.buildFilesExcludes = buildFilesExcludes != null ? buildFilesExcludes : List.of();
    }

    /**
     * Get the build files excludes.
     *
     * @return list of exclude patterns
     */
    List<String> buildFilesExcludes() {
        return buildFilesExcludes;
    }

    /**
     * Get the project files excludes.
     *
     * @return list of exclude patterns
     */
    List<String> projectFilesExcludes() {
        return projectFilesExcludes;
    }

    /**
     * Indicate if the project files checksum should be computed.
     *
     * @return projectFilesChecksums flag
     */
    boolean enableChecksums() {
        return enableChecksums;
    }

    /**
     * Indicate if the all the individual project file checksums should be computed.
     *
     * @return projectFilesChecksums flag
     */
    boolean includeAllChecksums() {
        return includeAllChecksums;
    }

    /**
     * Get the cache archive file.
     *
     * @return cache archive file or {@code null} if not configured.
     */
    Path archiveFile() {
        return archiveFile;
    }

    /**
     * If the archive file is configured and present, indicate if it should be loaded.
     *
     * @return load archive flag
     */
    boolean loadArchive() {
        return loadArchive;
    }

    /**
     * If the archive file is configured, indicate if it should be created.
     *
     * @return create archive flag
     */
    boolean createArchive() {
        return createArchive;
    }

    /**
     * Get the skip flag.
     *
     * @return skip flag
     */
    boolean skip() {
        return skip;
    }

    /**
     * Get the executions exclude patterns.
     *
     * @return list of patterns, never {@code null}
     */
    List<String> executionsExcludes() {
        return executionsExcludes;
    }

    /**
     * Get the executions include patterns.
     *
     * @return list of patterns, never {@code null}
     */
    List<String> executionsIncludes() {
        return executionsIncludes;
    }

    /**
     * Get the plugin configuration for a given project.
     *
     * @param project project to derive the plugin configuration
     * @param session Maven session to derive the properties
     * @return PluginConfig
     */
    static CacheConfig of(MavenProject project, MavenSession session) {
        return CONFIG_CACHE.computeIfAbsent(project, p -> loadConfig(p, session));
    }

    private static CacheConfig loadConfig(MavenProject project, MavenSession session) {
        loadVersion();
        for (Plugin buildPlugin : project.getBuildPlugins()) {
            if (GROUPID.equals(buildPlugin.getGroupId()) && ARTIFACTID.equals(buildPlugin.getArtifactId())) {
                if (version != null && buildPlugin.getVersion().equals(version)) {
                    Object config = buildPlugin.getConfiguration();
                    if (config instanceof Xpp3Dom) {
                        return new CacheConfig((Xpp3Dom) config, session);
                    } else {
                        break;
                    }
                }
            }
        }
        return EMPTY_CONFIG;
    }

    private static void loadVersion() {
        if (version == null) {
            synchronized (CacheConfig.class) {
                if (version == null) {
                    Properties props = new Properties();
                    InputStream is = CacheConfig.class.getClassLoader().getResourceAsStream(POM_PROPERTIES);
                    if (is != null) {
                        try {
                            props.load(is);
                            version = props.getProperty("version");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    private static List<String> stringListElement(Xpp3Dom config, String eltName) {
        Xpp3Dom elt = config.getChild(eltName);
        if (elt != null) {
            LinkedList<String> list = new LinkedList<>();
            for (Xpp3Dom exclude : elt.getChildren()) {
                String value = exclude.getValue();
                if (value != null && !value.isEmpty()) {
                    list.add(value);
                }
            }
            return list;
        }
        return null;
    }

    private static Path pathElement(Xpp3Dom config, String eltName) {
        Xpp3Dom elt = config.getChild(eltName);
        if (elt != null) {
            String value = elt.getValue();
            if (value != null && !value.isEmpty()) {
                return Path.of(value);
            }
        }
        return null;
    }

    private static Boolean booleanElement(Xpp3Dom config, String eltName) {
        Xpp3Dom elt = config.getChild(eltName);
        if (elt != null) {
            String value = elt.getValue();
            return value.isEmpty() || Boolean.parseBoolean(value);
        }
        return null;
    }

    private static Boolean booleanProperty(Properties sysProps, Properties userProps, String prop) {
        String value = sysProps.getProperty(prop);
        if (value == null) {
            value = userProps.getProperty(prop);
        }
        return value == null ? null : value.isEmpty() || Boolean.parseBoolean(value);
    }

    private static Path pathProperty(Properties sysProps, Properties userProps, String prop) {
        String value = sysProps.getProperty(prop);
        if (value == null) {
            value = userProps.getProperty(prop);
        }
        return value == null ? null : Path.of(value);
    }

    private static List<String> stringListProperty(Properties sysProps, Properties userProps, String prop) {
        String value = sysProps.getProperty(prop);
        if (value == null) {
            value = userProps.getProperty(prop);
        }
        if (value != null && value.isEmpty()) {
            return Arrays.stream(value.split(",")).collect(Collectors.toList());
        }
        return null;
    }
}
