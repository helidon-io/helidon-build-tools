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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.common.xml.XMLException;
import io.helidon.build.common.xml.XMLWriter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.DefaultMavenProjectHelper;
import org.apache.maven.project.MavenProject;

import static java.util.function.Predicate.not;

/**
 * Project state.
 */
final class ProjectState {

    private static final String STATE_FILE_NAME = "state.xml";
    private static final DefaultMavenProjectHelper PROJECT_HELPER = new DefaultMavenProjectHelper();

    private final Properties properties;
    private final ArtifactEntry artifact;
    private final List<ArtifactEntry> attachedArtifacts;
    private final List<String> compileSourceRoots;
    private final List<String> testCompileSourceRoots;
    private final ProjectFiles projectFiles;
    private final List<ExecutionEntry> executions;
    private final Map<ExecutionEntry, Optional<ExecutionEntry>> executionMatches;

    ProjectState(Properties properties,
                 ArtifactEntry artifact,
                 List<ArtifactEntry> attachedArtifacts,
                 List<String> compileSourceRoots,
                 List<String> testCompileSourceRoots,
                 ProjectFiles projectFiles,
                 List<ExecutionEntry> executions) {

        this.properties = Objects.requireNonNull(properties, "properties is null");
        this.artifact = artifact;
        this.attachedArtifacts = attachedArtifacts == null ? List.of() : attachedArtifacts;
        this.compileSourceRoots = compileSourceRoots == null ? List.of() : compileSourceRoots;
        this.testCompileSourceRoots = testCompileSourceRoots == null ? List.of() : testCompileSourceRoots;
        this.projectFiles = Objects.requireNonNull(projectFiles, "projectFiles is null");
        this.executions = executions == null ? List.of() : executions;
        this.executionMatches = new HashMap<>();
    }

    /**
     * Get the properties.
     *
     * @return properties, never {@code null}
     */
    Properties properties() {
        return properties;
    }

    /**
     * Get the artifact.
     *
     * @return ArtifactEntry, may be {@code null}
     */
    ArtifactEntry artifact() {
        return artifact;
    }

    /**
     * Get the attached artifacts.
     *
     * @return list, never {@code null}
     */
    List<ArtifactEntry> attachedArtifacts() {
        return attachedArtifacts;
    }

    /**
     * Get the compile source roots.
     *
     * @return list, never {@code null}
     */
    List<String> compileSourceRoots() {
        return compileSourceRoots;
    }

    /**
     * Get the test compile source roots.
     *
     * @return list, never {@code null}
     */
    List<String> testCompileSourceRoots() {
        return testCompileSourceRoots;
    }

    /**
     * Get the project files.
     *
     * @return project files, never {@code null}
     */
    ProjectFiles projectFiles() {
        return projectFiles;
    }

    /**
     * Get the executions.
     *
     * @return list, never {@code null}
     */
    List<ExecutionEntry> executions() {
        return executions;
    }

    /**
     * Load the project state from file.
     *
     * @param project maven project
     * @return state if state file exists, or {@code null}
     * @throws IOException  if an IO error occurs
     * @throws XMLException if a parsing error occurs
     */
    static ProjectState load(MavenProject project) throws IOException, XMLException {
        return load(project.getModel().getProjectDirectory().toPath()
                .resolve(project.getModel().getBuild().getDirectory())
                .resolve(STATE_FILE_NAME));
    }

    /**
     * Load the project state from file.
     *
     * @param stateFile state file
     * @return state if state file exists, or {@code null}
     * @throws IOException  if an IO error occurs
     * @throws XMLException if a parsing error occurs
     */
    static ProjectState load(Path stateFile) throws IOException, XMLException {
        if (!Files.exists(stateFile)) {
            return null;
        }
        Properties properties = new Properties();
        XMLElement rootElt = XMLElement.parse(Files.newInputStream(stateFile));
        for (XMLElement elt : rootElt.childrenAt("properties", "property")) {
            String name = elt.attribute("name", null);
            String value = elt.attribute("value", null);
            if (Strings.isValid(name) && value != null) {
                properties.setProperty(name, value);
            }
        }
        ArtifactEntry artifact = rootElt.child("artifact").map(ProjectState::readArtifact).orElse(null);
        List<ArtifactEntry> attachedArtifacts = Lists.map(rootElt.childrenAt("attached-artifacts", "artifact"),
                ProjectState::readArtifact);
        List<String> compileSourceRoots = rootElt.childrenAt("compile-source-roots", "path").stream()
                .map(XMLElement::value)
                .filter(Strings::isValid)
                .collect(Collectors.toList());
        List<String> testCompileSourceRoots = rootElt.childrenAt("test-compile-source-roots", "path").stream()
                .map(XMLElement::value)
                .filter(Strings::isValid)
                .collect(Collectors.toList());

        ProjectFiles projectFiles = ProjectFiles.fromXml(rootElt.child("project-files").orElse(null));
        List<ExecutionEntry> executions = Lists.map(rootElt.childrenAt("executions", "execution"), e -> {
            Map<String, String> attributes = e.attributes();
            return new ExecutionEntry(
                    attributes.get("groupId"),
                    attributes.get("artifactId"),
                    attributes.get("version"),
                    attributes.get("goal"),
                    attributes.get("id"),
                    e.child("configuration")
                            .map(XMLElement::detach)
                            .orElseGet(() -> XMLElement.builder().build()));
        });
        return new ProjectState(properties, artifact, attachedArtifacts, compileSourceRoots, testCompileSourceRoots,
                projectFiles, executions);
    }

    /**
     * Save the project state.
     *
     * @param project Maven project
     * @throws IOException if an IO error occurs
     */
    void save(MavenProject project) throws IOException {
        Model model = project.getModel();
        Path buildDir = model.getProjectDirectory().toPath().resolve(model.getBuild().getDirectory());
        if (!Files.exists(buildDir)) {
            Files.createDirectories(buildDir);
        }
        save(buildDir.resolve(STATE_FILE_NAME));
    }

    /**
     * Save the project state.
     *
     * @param stateFile state file
     * @throws IOException if an IO error occurs
     */
    void save(Path stateFile) throws IOException {
        XMLWriter writer = new XMLWriter(Files.newBufferedWriter(stateFile));
        writer.prolog().startElement("project-state");

        writer.startElement("properties");
        properties.forEach((k, v) -> writer
                .startElement("property")
                .attribute("name", k.toString())
                .attribute("value", v.toString())
                .endElement());
        writer.endElement();

        if (artifact != null) {
            writeArtifact(writer, artifact);
        }

        writer.startElement("attached-artifacts");
        for (ArtifactEntry artifact : attachedArtifacts) {
            writeArtifact(writer, artifact);
        }
        writer.endElement();

        writer.startElement("compile-source-roots");
        for (String path : compileSourceRoots) {
            writer.startElement("path").value(path).endElement();
        }
        writer.endElement();

        writer.startElement("test-compile-source-roots");
        for (String path : testCompileSourceRoots) {
            writer.startElement("path").value(path).endElement();
        }
        writer.endElement();

        writer.startElement("project-files");
        writer.attribute("count", projectFiles.filesCount());
        writer.attribute("last-modified", projectFiles.lastModified());
        String checksum = projectFiles.checksum();
        if (checksum != null) {
            writer.attribute("checksum", checksum);
        }
        projectFiles.allChecksums().forEach((k, v) -> writer
                .startElement("file")
                .attribute("checksum", v)
                .value(k)
                .endElement());

        writer.endElement();

        writer.startElement("executions");
        for (ExecutionEntry execution : executions) {
            writer.startElement("execution")
                    .attribute("groupId", execution.groupId())
                    .attribute("artifactId", execution.artifactId())
                    .attribute("version", execution.version())
                    .attribute("goal", execution.goal())
                    .attribute("id", execution.executionId());
            writer.append(execution.config());
            writer.endElement();
        }
        writer.endElement();
        writer.endElement();
        writer.close();
    }

    private static ArtifactEntry readArtifact(XMLElement elt) {
        Map<String, String> attributes = elt.attributes();
        return new ArtifactEntry(
                attributes.get("file"),
                attributes.get("type"),
                attributes.get("extension"),
                attributes.get("classifier"),
                attributes.get("language"),
                Boolean.parseBoolean(attributes.get("includesDependencies")),
                Boolean.parseBoolean(attributes.get("addedToClasspath")));
    }

    private static void writeArtifact(XMLWriter writer, ArtifactEntry artifact) {
        writer.startElement("artifact").attribute("file", artifact.file());
        writer.attribute("type", artifact.type());
        writer.attribute("extension", artifact.extension());
        String classifier = artifact.classifier();
        if (Strings.isValid(classifier)) {
            writer.attribute("classifier", classifier);
        }
        String language = artifact.language();
        if (Strings.isValid(language)) {
            writer.attribute("language", language);
        }
        writer.attribute("includesDependencies", artifact.includesDependencies());
        writer.attribute("addedToClasspath", artifact.addedToClasspath());
        writer.endElement();
    }

    /**
     * Apply this state to the given project.
     *
     * @param project Maven project
     * @param session Maven session
     */
    void apply(MavenProject project, MavenSession session) {
        Path projectDir = project.getModel().getProjectDirectory().toPath();
        properties.forEach((k, v) -> project.getProperties().put(k, loadPropValue(session, (String) v)));
        Optional.ofNullable(artifact)
                .map(a -> a.toArtifact(project))
                .ifPresent(project::setArtifact);
        compileSourceRoots
                .stream()
                .map(projectDir::resolve)
                .map(Object::toString)
                .filter(not(project.getCompileSourceRoots()::contains))
                .forEach(project::addCompileSourceRoot);
        testCompileSourceRoots
                .stream()
                .map(projectDir::resolve)
                .map(Object::toString)
                .filter(not(project.getTestCompileSourceRoots()::contains))
                .forEach(project::addTestCompileSourceRoot);
        attachedArtifacts
                .stream()
                .map(a -> a.toArtifact(project))
                .forEach(a -> PROJECT_HELPER.attachArtifact(project, a));
    }

    /**
     * Test if the given execution matches any of the ones in this state using
     * {@link ExecutionEntry#matches(ExecutionEntry)}.
     *
     * @param execution execution to match
     * @return {@code true} if found, {@code false} otherwise
     */
    boolean hasMatchingExecution(ExecutionEntry execution) {
        return findMatchingExecution(execution) != null;
    }

    /**
     * Find a recorded execution matching the given one using
     * {@link ExecutionEntry#matches(ExecutionEntry)}.
     *
     * @param execution execution to match
     * @return ExecutionEntry or {@code null} if not found
     */
    ExecutionEntry findMatchingExecution(ExecutionEntry execution) {
        return executionMatches.computeIfAbsent(execution, (key) -> {
            for (ExecutionEntry exec : executions) {
                if (exec.matches(key)) {
                    return Optional.of(exec);
                }
            }
            return Optional.empty();
        }).orElse(null);
    }

    /**
     * Create a state for the given project and merge it with the existing state for this project.
     *
     * @param state           existing state, may be {@code null}
     * @param project         Maven project
     * @param session         Maven session
     * @param configManager   cache config manager
     * @param newExecutions   new executions
     * @param newProjectFiles current project files
     * @return ProjectState
     * @throws IOException if an IO error occurs while scanning project files
     */
    static ProjectState merge(ProjectState state,
                              MavenProject project,
                              MavenSession session,
                              CacheConfigManager configManager,
                              List<ExecutionEntry> newExecutions,
                              ProjectFiles newProjectFiles)
            throws IOException {

        Path projectDir = project.getModel().getProjectDirectory().toPath();
        List<ExecutionEntry> executions;
        Properties properties;
        if (state == null) {
            executions = List.of();
            properties = new Properties();
        } else {
            executions = state.executions;
            properties = state.properties;
        }
        Properties projectProps = new Properties();
        project.getProperties().forEach((k, v) -> projectProps.put(k, savePropValue(session, (String) v)));
        return new ProjectState(
                mergeProperties(properties, projectProps),
                Optional.ofNullable(project.getArtifact())
                        .map(a -> ArtifactEntry.create(a, project))
                        .orElse(null),
                project.getAttachedArtifacts()
                        .stream()
                        .map(a -> ArtifactEntry.create(a, project))
                        .collect(Collectors.toList()),
                project.getCompileSourceRoots()
                        .stream()
                        .map(Paths::get)
                        .map(projectDir::relativize)
                        .map(Path::toString)
                        .collect(Collectors.toList()),
                project.getTestCompileSourceRoots()
                        .stream()
                        .map(Paths::get)
                        .map(projectDir::relativize)
                        .map(Path::toString)
                        .collect(Collectors.toList()),
                newProjectFiles == null ? ProjectFiles.of(project, configManager) : newProjectFiles,
                Stream.concat(executions.stream().filter(exec -> newExecutions.stream().noneMatch(exec::matches)),
                        newExecutions.stream()).collect(Collectors.toList()));
    }

    private static String loadPropValue(MavenSession session, String value) {
        String rootDir = session.getRequest().getMultiModuleProjectDirectory().toPath().toString();
        return value.replace("#{root.dir}", rootDir);
    }

    private static String savePropValue(MavenSession session, String value) {
        String rootDir = session.getRequest().getMultiModuleProjectDirectory().toPath().toString();
        return value.replace(rootDir, "#{root.dir}");
    }

    private static Properties mergeProperties(Properties props1, Properties props2) {
        Properties properties = new Properties();
        properties.putAll(props1);
        properties.putAll(props2);
        return properties;
    }
}
