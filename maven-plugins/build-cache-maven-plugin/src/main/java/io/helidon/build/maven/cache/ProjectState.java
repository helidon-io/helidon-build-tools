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
package io.helidon.build.maven.cache;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Project state.
 */
final class ProjectState {

    static final String STATE_FILE_NAME = "state.xml";

    private final Properties properties;
    private final ArtifactEntry artifact;
    private final List<ArtifactEntry> attachedArtifacts;
    private final List<String> compileSourceRoots;
    private final List<String> testCompileSourceRoots;
    private final ProjectFiles projectFiles;
    private final List<ExecutionEntry> executions;
    private final Map<ExecutionEntry, Optional<ExecutionEntry>> executionMatches;

    private ProjectState(Properties properties,
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
     * Get the project files.
     *
     * @return project files, never {@code null}
     */
    ProjectFiles projectFiles() {
        return projectFiles;
    }

    /**
     * Load the project state from file.
     *
     * @param project maven project
     * @return state if state file exists, or {@code null}
     * @throws IOException            if an IO error occurs
     * @throws XmlPullParserException if a parsing error occurs
     */
    static ProjectState load(MavenProject project) throws IOException, XmlPullParserException {
        Path stateFile = project.getModel().getProjectDirectory().toPath()
                                .resolve(project.getModel().getBuild().getDirectory())
                                .resolve(STATE_FILE_NAME);
        if (!Files.exists(stateFile)) {
            return null;
        }
        BufferedReader reader = Files.newBufferedReader(stateFile);
        Xpp3Dom rootElt = Xpp3DomBuilder.build(reader, false);
        Properties properties = new Properties();
        Xpp3Dom propertiesElt = rootElt.getChild("properties");
        if (propertiesElt != null) {
            for (Xpp3Dom propertyElt : propertiesElt.getChildren("property")) {
                String name = propertyElt.getAttribute("name");
                String value = propertyElt.getAttribute("value");
                if (name != null && !name.isEmpty() && value != null) {
                    properties.setProperty(name, value);
                }
            }
        }
        ArtifactEntry artifact = null;
        Xpp3Dom artifactElt = rootElt.getChild(ArtifactEntry.XML_ELEMENT_NAME);
        if (artifactElt != null) {
            artifact = ArtifactEntry.fromXml(artifactElt);
        }
        List<ArtifactEntry> attachedArtifacts = new LinkedList<>();
        Xpp3Dom attachedArtifactsElt = rootElt.getChild("attached-artifacts");
        if (attachedArtifactsElt != null) {
            for (Xpp3Dom attachedArtifactElt : attachedArtifactsElt.getChildren(ArtifactEntry.XML_ELEMENT_NAME)) {
                attachedArtifacts.add(ArtifactEntry.fromXml(attachedArtifactElt));
            }
        }
        List<String> compileSourceRoots = new LinkedList<>();
        Xpp3Dom compileSourceRootsElt = rootElt.getChild("compile-source-roots");
        if (compileSourceRootsElt != null) {
            for (Xpp3Dom pathElt : compileSourceRootsElt.getChildren("path")) {
                String path = pathElt.getValue();
                if (path != null && !path.isEmpty()) {
                    compileSourceRoots.add(path);
                }
            }
        }
        List<String> testCompileSourceRoots = new LinkedList<>();
        Xpp3Dom testCompileSourceRootsElt = rootElt.getChild("test-compile-source-roots");
        if (testCompileSourceRootsElt != null) {
            for (Xpp3Dom pathElt : testCompileSourceRootsElt.getChildren("path")) {
                String path = pathElt.getValue();
                if (path != null && !path.isEmpty()) {
                    testCompileSourceRoots.add(path);
                }
            }
        }
        ProjectFiles projectFiles = ProjectFiles.fromXml(rootElt.getChild(ProjectFiles.XML_ELEMENT_NAME));
        List<ExecutionEntry> executions = new LinkedList<>();
        Xpp3Dom executionsElt = rootElt.getChild("executions");
        if (executionsElt != null) {
            for (Xpp3Dom executionElt : executionsElt.getChildren(ExecutionEntry.XML_ELEMENT_NAME)) {
                executions.add(ExecutionEntry.fromXml(executionElt));
            }
        }
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
        Path buildDir = project.getModel().getProjectDirectory().toPath()
                               .resolve(project.getModel().getBuild().getDirectory());
        Xpp3Dom rootElt = new Xpp3Dom("project-state");
        Xpp3Dom propertiesElt = new Xpp3Dom("properties");
        for (String propName : properties.stringPropertyNames()) {
            Xpp3Dom propertyElt = new Xpp3Dom("property");
            propertyElt.setAttribute("name", propName);
            propertyElt.setAttribute("value", properties.getProperty(propName));
            propertiesElt.addChild(propertyElt);
        }
        rootElt.addChild(propertiesElt);
        if (artifact != null) {
            rootElt.addChild(artifact.toXml());
        }

        Xpp3Dom attachedArtifactsElt = new Xpp3Dom("attached-artifacts");
        for (ArtifactEntry artifact : attachedArtifacts) {
            attachedArtifactsElt.addChild(artifact.toXml());
        }
        rootElt.addChild(attachedArtifactsElt);
        Xpp3Dom compileSourceRootsElt = new Xpp3Dom("compile-source-roots");
        for (String path : compileSourceRoots) {
            Xpp3Dom pathElt = new Xpp3Dom("path");
            pathElt.setValue(path);
            compileSourceRootsElt.addChild(pathElt);
        }
        rootElt.addChild(compileSourceRootsElt);
        Xpp3Dom testCompileSourceRootsElt = new Xpp3Dom("test-compile-source-roots");
        for (String path : testCompileSourceRoots) {
            Xpp3Dom pathElt = new Xpp3Dom("path");
            pathElt.setValue(path);
            testCompileSourceRootsElt.addChild(pathElt);
        }
        rootElt.addChild(testCompileSourceRootsElt);
        rootElt.addChild(projectFiles.toXml());
        Xpp3Dom executionsElt = new Xpp3Dom("executions");
        for (ExecutionEntry execution : executions) {
            executionsElt.addChild(execution.toXml());
        }
        rootElt.addChild(executionsElt);
        if (!Files.exists(buildDir)) {
            Files.createDirectories(buildDir);
        }
        FileWriter writer = new FileWriter(buildDir.resolve(STATE_FILE_NAME).toFile());
        Xpp3DomWriter.write(writer, rootElt);
        writer.flush();
        writer.close();
    }

    /**
     * Apply this state to the given project.
     *
     * @param project Maven project
     * @param session Maven session
     */
    @SuppressWarnings("unchecked")
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
                .filter(project.getCompileSourceRoots()::contains)
                .forEach(project::addCompileSourceRoot);
        testCompileSourceRoots
                .stream()
                .map(projectDir::resolve)
                .map(Object::toString)
                .filter(project.getTestCompileSourceRoots()::contains)
                .forEach(project::addTestCompileSourceRoot);
        attachedArtifacts
                .stream()
                .map(a -> a.toArtifact(project))
                .forEach(project::addAttachedArtifact);
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
     * @param newExecutions   new executions
     * @param newProjectFiles current project files
     * @return ProjectState
     * @throws IOException if an IO error occurs while scanning project files
     */
    static ProjectState merge(ProjectState state,
                              MavenProject project,
                              MavenSession session,
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
                newProjectFiles == null ? ProjectFiles.of(project, session) : newProjectFiles,
                Stream.concat(executions.stream().filter(exec -> newExecutions.stream().noneMatch(exec::matches)),
                        newExecutions.stream()).collect(Collectors.toList()));
    }

    private static String loadPropValue(MavenSession session, String value) {
        return value.replace("#{exec.root.dir}", session.getExecutionRootDirectory());
    }

    private static String savePropValue(MavenSession session, String value) {
        return value.replace(session.getExecutionRootDirectory(), "#{exec.root.dir}");
    }

    private static Properties mergeProperties(Properties props1, Properties props2) {
        Properties properties = new Properties();
        props1.forEach(properties::put);
        props2.forEach(properties::put);
        return properties;
    }
}
