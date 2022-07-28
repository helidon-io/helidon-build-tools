package io.helidon.lsp.maven;

import com.google.gson.Gson;
import io.helidon.lsp.common.Dependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
//import org.apache.maven.project.MavenProject;

import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Get information about dependencies of the project.
 */
@Mojo(name = "list-dependencies", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class DependencyMojo extends AbstractMojo
{
    private static final String HOST = "127.0.0.1";
    private static final Gson GSON = new Gson();
    /**
     * Scope to filter the dependencies.
     */
    @Parameter(property = "scope")
    private String scope;

//    @Component
//    ProjectBuilder defaultProjectBuilder;

//    @Component
//    private ModelReader modelReader;

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

    @Component
    protected ProjectBuilder projectBuilder;

    @Component
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    public void execute() throws MojoExecutionException, MojoFailureException {

        MavenExecutionRequest request = session.getRequest();
        request.getProjectBuildingRequest().setRepositorySession( session.getRepositorySession() );
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
            lifeCycleDependencyResolver.resolveProjectDependencies(project, new TreeSet<>(), scopesToResolve, session, false, Collections.emptySet());
            project.setArtifactFilter( new CumulativeScopeArtifactFilter(scopesToResolve) );
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

//         Socket clientSocket = null;
//         PrintWriter out = null;
//         BufferedReader in = null;

        try (
                Socket clientSocket = new Socket(HOST, port);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
//            clientSocket = new Socket(HOST, port);
//            out = new PrintWriter(clientSocket.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out.println(GSON.toJson(dependencies));
            getLog().info("STOP SENDING DATA");
//            String resp = in.readLine();
//            in.close();
            out.close();

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }




        System.out.println(artifacts.size());
    }
}
