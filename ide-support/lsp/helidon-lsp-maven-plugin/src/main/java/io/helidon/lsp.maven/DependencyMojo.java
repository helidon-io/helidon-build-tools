/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.maven;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;

import io.helidon.lsp.common.Dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

/**
 * Get information about dependencies of the project.
 */
@Mojo(name = "list-dependencies", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class DependencyMojo extends AbstractMojo {

    private static final String HOST = "127.0.0.1";
    private final static JsonBuilderFactory JSON_FACTORY = Json.createBuilderFactory(Map.of());

    /**
     * Scope to filter the dependencies.
     */
    @Parameter(property = "scope")
    private String scope;

    /**
     * The Maven project this mojo executes on if pomPath parameter is not specified.
     */
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * Port that will be used to send information.
     */
    @Parameter(property = "port", required = true)
    private Integer port;

    /**
     * Path to the pom file of the project.
     */
    @Parameter(property = "pomPath")
    private String pomPath;

    /**
     * The current Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * ProjectBuilder component, used to create project if pomPath parameter is specified.
     */
    @Component
    private ProjectBuilder projectBuilder;

    /**
     * LifecycleDependencyResolver component, used to obtain all dependencies artifacts (including transitive) for the project.
     */
    @Component
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        MavenExecutionRequest request = session.getRequest();
        request.getProjectBuildingRequest().setRepositorySession(session.getRepositorySession());
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        try {
            if (pomPath != null) {
                ProjectBuildingResult build = projectBuilder.build(new File(pomPath), buildingRequest);
                project = build.getProject();
            }
            session.setCurrentProject(project);
            Set<String> scopesToResolve = new TreeSet<>();
            if (scope == null) {
                scopesToResolve.addAll(List.of("compile", "provided", "runtime", "system", "test"));
            } else {
                scopesToResolve.add(scope);
            }
            lifeCycleDependencyResolver.resolveProjectDependencies(project, new TreeSet<>(), scopesToResolve, session, false,
                    Collections.emptySet());
            project.setArtifactFilter(new CumulativeScopeArtifactFilter(scopesToResolve));
        } catch (ProjectBuildingException | LifecycleExecutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        Set<Artifact> artifacts = project.getArtifacts();
        Set<Dependency> dependencies = artifacts.stream().map(artifact -> new Dependency(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getType(),
                artifact.getScope(),
                artifact.getFile().toString()
        )).collect(Collectors.toSet());

        try (
                Socket clientSocket = new Socket(HOST, port);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            JsonArrayBuilder arrayBuilder = JSON_FACTORY.createArrayBuilder();
            for (Dependency dependency : dependencies) {
                arrayBuilder.add(dependency.toJsonObject());
            }
            out.println(arrayBuilder.build().toString());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
