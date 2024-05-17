/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;

/**
 * Artifact entry.
 */
final class ArtifactEntry {

    private final String extension;
    private final String classifier;
    private final String file;
    private final boolean includesDependencies;
    private final String language;
    private final boolean addedToClasspath;

    /**
     * Create a new artifact entry.
     *
     * @param file                 artifact file, must be non {@code null}
     * @param extension            artifact extension, must be non {@code null}
     * @param classifier           artifact classifier, may be {@code null} or empty
     * @param language             artifact handler language, may be {@code null} or empty
     * @param includesDependencies artifact handler includes dependencies flag
     * @param addedToClasspath     artifact handler added to classpath flag
     */
    ArtifactEntry(String file,
                  String extension,
                  String classifier,
                  String language,
                  boolean includesDependencies,
                  boolean addedToClasspath) {

        this.extension = Objects.requireNonNull(extension, "extension is null");
        this.classifier = classifier != null && classifier.isEmpty() ? null : classifier;
        this.file = Objects.requireNonNull(file, "file is null");
        this.language = language != null && language.isEmpty() ? null : language;
        this.includesDependencies = includesDependencies;
        this.addedToClasspath = addedToClasspath;
    }

    /**
     * Get the artifact file.
     *
     * @return file path, never {@code null}
     */
    String file() {
        return file;
    }

    /**
     * Get the artifact classifier.
     *
     * @return classifier, may be {@code null}
     */
    String classifier() {
        return classifier;
    }

    /**
     * Get the artifact extension.
     *
     * @return extension, never {@code null}
     */
    String extension() {
        return extension;
    }

    /**
     * Get the artifact handler language.
     *
     * @return language, may be {@code null}
     */
    String language() {
        return language;
    }

    /**
     * Get the added to classpath flag.
     *
     * @return added to classpath flag
     */
    boolean addedToClasspath() {
        return addedToClasspath;
    }

    /**
     * Get the includes dependencies flag.
     *
     * @return includes dependencies flag
     */
    boolean includesDependencies() {
        return includesDependencies;
    }

    /**
     * Create a new artifact entry from a Maven artifact.
     *
     * @param artifact maven artifact
     * @param project  maven project
     * @return ArtifactEntry
     */
    static ArtifactEntry create(Artifact artifact, MavenProject project) {
        if (artifact == null || artifact.getFile() == null) {
            return null;
        }
        ArtifactHandler handler = artifact.getArtifactHandler();
        String file = project.getModel().getProjectDirectory().toPath()
                .resolve(project.getModel().getBuild().getDirectory())
                .relativize(artifact.getFile().toPath()).toString();
        return new ArtifactEntry(file, artifact.getType(), artifact.getClassifier(), handler.getLanguage(),
                handler.isIncludesDependencies(), handler.isAddedToClasspath());
    }

    /**
     * Convert this artifact entry to a Maven artifact.
     *
     * @param project maven project
     * @return Artifact
     */
    Artifact toArtifact(MavenProject project) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler(extension);
        if (language != null) {
            handler.setLanguage(language);
        }
        handler.setIncludesDependencies(includesDependencies);
        handler.setAddedToClasspath(addedToClasspath);
        Artifact artifact = new DefaultArtifact(
                project.getGroupId(), project.getArtifactId(), project.getVersion(), "compile",
                extension, classifier, handler);
        artifact.setFile(project.getModel().getProjectDirectory().toPath()
                .resolve(project.getModel().getBuild().getDirectory())
                .resolve(file)
                .toFile());
        return artifact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArtifactEntry that = (ArtifactEntry) o;
        return includesDependencies == that.includesDependencies
               && addedToClasspath == that.addedToClasspath
               && Objects.equals(extension, that.extension)
               && Objects.equals(classifier, that.classifier)
               && Objects.equals(file, that.file)
               && Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extension, classifier, file, includesDependencies, language, addedToClasspath);
    }

    @Override
    public String toString() {
        return "ArtifactEntry{"
               + "extension='" + (extension == null ? "" : extension) + '\''
               + ", classifier='" + (classifier == null ? "" : classifier) + '\''
               + ", file='" + file + '\''
               + ", includesDependencies=" + includesDependencies
               + ", language='" + (language == null ? "" : language) + '\''
               + ", addedToClasspath=" + addedToClasspath
               + '}';
    }
}
