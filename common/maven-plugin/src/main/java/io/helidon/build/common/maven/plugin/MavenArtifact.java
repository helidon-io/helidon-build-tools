/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.plugin;

import java.nio.file.Path;
import java.util.function.Supplier;

import io.helidon.build.common.maven.MavenModel;

/**
 * Maven Artifact.
 *
 * @param groupId    groupId
 * @param artifactId artifactId
 * @param version    version
 * @param classifier classifier
 * @param type       type
 * @param file       file
 */
public record MavenArtifact(String groupId, String artifactId, String version, String classifier, String type, Path file) {

    /**
     * Create a new instance.
     *
     * @param groupId    groupId
     * @param artifactId artifactId
     * @param version    version
     * @param classifier classifier
     * @param type       type
     */
    public MavenArtifact(String groupId, String artifactId, String version, String classifier, String type) {
        this(groupId, artifactId, version, classifier, type, null);
    }

    /**
     * Create a new instance.
     *
     * @param a artifact
     */
    public MavenArtifact(org.eclipse.aether.artifact.Artifact a) {
        this(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getExtension(),
                a.getFile() != null ? a.getFile().toPath() : null);
    }

    /**
     * Create a new instance.
     *
     * @param a artifact
     */
    public MavenArtifact(org.apache.maven.artifact.Artifact a) {
        this(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getType(),
                a.getFile() != null ? a.getFile().toPath() : null);
    }

    /**
     * Create a new instance.
     *
     * @param d dependency
     */
    public MavenArtifact(org.apache.maven.model.Dependency d) {
        this(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getClassifier(), d.getType());
    }

    /**
     * Create a new instance.
     *
     * @param d        dependency
     * @param supplier version supplier
     */
    public MavenArtifact(org.apache.maven.model.Dependency d, Supplier<String> supplier) {
        this(d.getGroupId(), d.getArtifactId(), d.getVersion() == null ? supplier.get() : d.getVersion(),
                d.getClassifier(), d.getType());
    }

    /**
     * Create a new instance.
     *
     * @param p plugin
     */
    public MavenArtifact(org.apache.maven.model.Plugin p) {
        this(p.getGroupId(), p.getArtifactId(), p.getVersion(), null, "jar");
    }

    /**
     * Create a new instance.
     *
     * @param p        plugin
     * @param supplier supplier resolver
     */
    public MavenArtifact(org.apache.maven.model.Plugin p, Supplier<String> supplier) {
        this(p.getGroupId(), p.getArtifactId(), p.getVersion() == null ? supplier.get() : p.getVersion(), null, "jar");
    }

    /**
     * Create a new instance.
     *
     * @param m model
     */
    public MavenArtifact(MavenModel m) {
        this(m.groupId(), m.artifactId(), m.version(), null, "jar");
    }

    /**
     * Convert this instance to {@link org.eclipse.aether.artifact.Artifact}.
     *
     * @return artifact
     */
    public org.eclipse.aether.artifact.Artifact toAetherArtifact() {
        return new org.eclipse.aether.artifact.DefaultArtifact(groupId, artifactId, classifier, type, version);
    }

    /**
     * Get the corresponding {@code .pom} artifact.
     *
     * @return artifact
     */
    public MavenArtifact pom() {
        return new MavenArtifact(groupId, artifactId, version, null, "pom");
    }

    /**
     * Get the corresponding {@code -sources.jar} artifact.
     *
     * @return artifact
     */
    public MavenArtifact sourcesJar() {
        return new MavenArtifact(groupId, artifactId, version, "sources", "jar");
    }

    /**
     * Get the coordinates.
     *
     * @return coordinates
     */
    public String coordinates() {
        String coords = groupId
               + ":" + artifactId
               + ":" + type;
        if (classifier != null) {
            coords += ":" + classifier;
        }
        coords += ":" + version;
        return coords;
    }
}
