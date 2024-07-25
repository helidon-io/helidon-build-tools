/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.dependency;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Strings;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenModel;
import io.helidon.build.common.maven.plugin.MavenArtifact;
import io.helidon.build.common.maven.plugin.MavenFilters;
import io.helidon.build.common.maven.plugin.PlexusLoggerHolder;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import static java.util.stream.Collectors.toMap;

/**
 * A goal to aggressively cache all Maven dependencies.
 * Only supports JDK >= 17.
 */
@Mojo(name = "go-offline", threadSafe = true, requiresDirectInvocation = true, aggregator = true)
public class GoOfflineMojo extends AbstractMojo {

    @Component
    @SuppressWarnings("unused")
    private PlexusLoggerHolder plexusLogHolder;

    /**
     * The entry point to Aether.
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * Maven Project Builder component.
     */
    @Component
    private ProjectBuilder projectBuilder;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project remote repositories to use.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    /**
     * Pom identity.
     * List of relative paths that must exist for a directory to be resolved as a Maven module.
     */
    @Parameter(property = "helidon.dependency.offline.pomScanningIdentity", defaultValue = "pom.xml")
    private List<String> pomScanningIdentity = List.of();

    /**
     * Pom scanning includes.
     * List of glob expressions used as an include filter for directories that may contain {@code pom.xml} files.
     */
    @Parameter(property = "helidon.dependency.offline.pomScanningIncludes", defaultValue = "**/*")
    private List<String> pomScanningIncludes = List.of();

    /**
     * Pom scanning excludes.
     * List of glob expressions used as an exclude filter for directories that may contain {@code pom.xml} files.
     */
    @Parameter(property = "helidon.dependency.offline.pomScanningExcludes", defaultValue = "**/target/**,**/src/**")
    private List<String> pomScanningExcludes = List.of();

    /**
     * Pom include patterns.
     * List of include filters (format is {@code groupId:artifactId:packaging} with wildcard support)
     * of scanned {@code pom.xml} files.
     */
    @Parameter(property = "helidon.dependency.offline.pomIncludes", defaultValue = "*:*:*")
    private List<String> pomIncludes = List.of();

    /**
     * Pom exclude patterns.
     * List of exclude filters (format is {@code groupId:artifactId:packaging} with wildcard support)
     * of scanned {@code pom.xml} files.
     */
    @Parameter(property = "helidon.dependency.offline.pomExcludes")
    private List<String> pomExcludes = List.of();

    /**
     * Specifies if {@code -SNAPSHOT} artifacts should be processed.
     */
    @Parameter(property = "helidon.dependency.offline.includeSnapshots", defaultValue = "false")
    private boolean includeSnapshots;

    /**
     * Specifies if dependencies should be processed.
     */
    @Parameter(property = "helidon.dependency.offline.includeDependencies", defaultValue = "true")
    private boolean includeDeps;

    /**
     * Specifies if dependency management should be processed.
     */
    @Parameter(property = "helidon.dependency.offline.includeDependencyManagement", defaultValue = "false")
    private boolean includeDepsMgmt;

    /**
     * Specifies if plugins should be processed.
     */
    @Parameter(property = "helidon.dependency.offline.includePlugins", defaultValue = "true")
    private boolean includePlugins;

    /**
     * Specifies if plugin management should be processed.
     */
    @Parameter(property = "helidon.dependency.offline.includePluginManagement", defaultValue = "false")
    private boolean includePluginMgmt;

    /**
     * Specifies if the resolution should traverse.
     */
    @Parameter(property = "helidon.dependency.offline.traverse", defaultValue = "true")
    private boolean traverse;

    /**
     * Profile include patterns.
     * List of include filters (format is {@code groupId:artifactId:packaging} with wildcard support).
     */
    @Parameter(property = "helidon.dependency.offline.profileIncludes", defaultValue = "*")
    private List<String> profileIncludes = List.of();

    /**
     * Profile exclude patterns.
     * List of exclude filters (format is {@code groupId:artifactId:packaging} with wildcard support).
     */
    @Parameter(property = "helidon.dependency.offline.profileExcludes")
    private List<String> profileExcludes = List.of();

    /**
     * Transitive scope include patterns.
     * List of include filters (format is {@code groupId:artifactId:packaging} with wildcard support).
     */
    @Parameter(property = "helidon.dependency.offline.scopeIncludes", defaultValue = "*")
    private List<String> scopeIncludes = List.of();

    /**
     * Transitive scope exclude patterns.
     * List of exclude filters (format is {@code groupId:artifactId:packaging} with wildcard support).
     */
    @Parameter(property = "helidon.dependency.offline.scopeExcludes", defaultValue = "test")
    private List<String> scopeExcludes = List.of();

    /**
     * Specifies if optional transitive dependencies should be processed.
     */
    @Parameter(property = "helidon.dependency.offline.includeOptional", defaultValue = "true")
    private boolean includeOptional;

    /**
     * Skip this goal execution.
     */
    @Parameter(property = "helidon.dependency.offline.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Specifies if the build will fail if there are errors during execution or not.
     */
    @Parameter(property = "helidon.dependency.offline.failOnError", defaultValue = "false")
    private boolean failOnError;

    private Predicate<MavenModel> pomFilter;
    private Predicate<Path> pomIdentityFilter;
    private Predicate<Path> pomScanningFilter;
    private Predicate<String> profileFilter;
    private Predicate<String> scopeFilter;
    private Map<MavenArtifact, List<Path>> workspace;
    private final Set<MavenArtifact> resolved = new HashSet<>();

    @Override
    public void execute() {
        if (skip) {
            Log.info("processing is skipped.");
            return;
        }

        // init filters
        pomFilter = MavenFilters.pomFilter(pomIncludes, pomExcludes);
        pomIdentityFilter = MavenFilters.dirFilter(pomScanningIdentity);
        pomScanningFilter = MavenFilters.pathFilter(pomScanningIncludes, pomScanningExcludes, Path.of(""));
        profileFilter = MavenFilters.stringFilter(profileIncludes, profileExcludes);
        scopeFilter = MavenFilters.stringFilter(scopeIncludes, scopeExcludes);

        // scan workspace manually
        workspace = scanWorkspace();

        // derive projects from workspace
        List<MavenProject> allProjects = collectProjects();

        // expand projects into artifacts to traverse (via effective pom)
        List<ResolvableArtifacts> initial = allProjects.stream()
                .map(it -> collect(it, false, includeDeps, includeDepsMgmt, includePlugins, includePluginMgmt))
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        // mark workspace artifacts as pre-resolved
        resolved.addAll(workspace.keySet());

        // depth-first traversal
        Deque<ResolvableArtifacts> stack = new ArrayDeque<>(initial);
        while (!stack.isEmpty()) {
            ResolvableArtifacts e = stack.pop();
            List<MavenArtifact> requests = Lists.filter(e.artifacts, this::filterArtifact);
            if (requests.isEmpty()) {
                continue;
            }
            List<MavenArtifact> result = resolve(requests, e.repos);
            resolved.addAll(requests);
            if (traverse) {
                for (MavenArtifact artifact : result) {
                    if (!filterArtifact(artifact)) {
                        continue;
                    }
                    MavenArtifact pom = resolve(artifact.pom(), e.repos);
                    if (pom != null) {
                        Log.debug("Resolving for traversal: %s, requests: %s", artifact, requests);
                        MavenProject project = effectivePom(pom.file());
                        if (project != null) {
                            Log.info("Traversing %s", pom);
                            List<ResolvableArtifacts> downstream = collect(project, true, includeDeps, false, false, false);
                            downstream.forEach(stack::push);
                        }
                    }
                }
            }
        }
    }

    private boolean filterArtifact(MavenArtifact artifact) {
        return !resolved.contains(artifact) && artifact.version() != null
               && (includeSnapshots || !artifact.version().endsWith("-SNAPSHOT"));
    }

    private boolean filterDependency(Dependency dep, MavenProject project, String profileId) {
        String scope = dep.getScope() == null ? scope(project, dep, profileId) : dep.getScope();
        String optional = dep.getOptional() == null ? optional(project, dep, profileId) : dep.getOptional();
        return scopeFilter.test(scope == null ? "" : scope)
               && (!Boolean.parseBoolean(optional) || includeOptional);
    }

    private List<MavenProject> collectProjects() {
        return workspace.values().stream()
                .flatMap(Collection::stream)
                .map(p -> Optional.ofNullable(effectivePom(p)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Map<MavenArtifact, List<Path>> scanWorkspace() {
        try (Stream<Path> stream = Files.walk(Path.of(""))) {
            return stream
                    .filter(pomIdentityFilter)
                    .filter(p -> pomScanningFilter.test(p))
                    .map(it -> {
                        Path file = it.resolve("pom.xml");
                        Log.debug("Reading model: %s", file);
                        return Map.entry(MavenModel.read(file), Lists.of(file));
                    })
                    .filter(it -> pomFilter.test(it.getKey()))
                    .collect(toMap(it -> new MavenArtifact(it.getKey()), Map.Entry::getValue, Lists::addAll));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private MavenArtifact resolve(MavenArtifact artifact, Set<RemoteRepository> repositories) {
        try {
            Log.debug("Resolving artifact: %s, repositories: %s", artifact, repositories);
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(artifact.toAetherArtifact());
            request.setRepositories(new ArrayList<>(repositories));
            ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
            return new MavenArtifact(result.getArtifact());
        } catch (ArtifactResolutionException ex) {
            if (failOnError) {
                throw new RuntimeException(ex);
            }
            Log.log(LogLevel.DEBUG, ex, "Unable to resolve artifact: %s, repositories: %s", artifact, repositories);
            return null;
        }
    }

    private List<MavenArtifact> resolve(List<MavenArtifact> artifacts, Set<RemoteRepository> repositories) {
        try {
            Log.debug("Resolving artifacts: %s, repositories: %s", artifacts, repositories);
            List<RemoteRepository> repos = new ArrayList<>(repositories);
            List<ArtifactRequest> requests = Lists.map(artifacts, it -> {
                ArtifactRequest request = new ArtifactRequest();
                request.setArtifact(it.toAetherArtifact());
                request.setRepositories(repos);
                return request;
            });
            List<ArtifactResult> results = repoSystem.resolveArtifacts(repoSession, requests);
            return Lists.map(results, it -> new MavenArtifact(it.getArtifact()));
        } catch (ArtifactResolutionException ex) {
            if (failOnError) {
                throw new RuntimeException(ex);
            }
            Log.log(LogLevel.DEBUG, ex, "Unable to resolve artifacts: %s, repositories: %s", artifacts, repositories);
            return List.of();
        }
    }

    private MavenProject effectivePom(Path pomFile) {
        try {
            Log.debug("resolving effective pom " + pomFile);
            ProjectBuildingRequest orig = session.getProjectBuildingRequest();
            ProjectBuildingRequest pbr = new DefaultProjectBuildingRequest(orig);
            pbr.setRemoteRepositories(orig.getRemoteRepositories());
            pbr.setPluginArtifactRepositories(orig.getPluginArtifactRepositories());
            pbr.setProject(null);
            pbr.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            pbr.setResolveDependencies(false);
            return projectBuilder.build(pomFile.toFile(), pbr).getProject();
        } catch (ProjectBuildingException ex) {
            if (failOnError) {
                throw new RuntimeException(ex);
            }
            Log.log(LogLevel.DEBUG, ex, "Unable to build effective model for: %s", pomFile);
            return null;
        }
    }

    private List<ResolvableArtifacts> collect(MavenProject project,
                                              boolean traversing,
                                              boolean includeDeps,
                                              boolean includeDepsMgmt,
                                              boolean includePlugins,
                                              boolean includePluginMgmt) {

        List<ResolvableArtifacts> artifacts = new ArrayList<>();

        Set<RemoteRepository> repos = repos(project.getRepositories(), Set.of());
        Set<RemoteRepository> pluginRepos = repos(project.getPluginRepositories(), Set.of());

        if (includeDeps) {
            List<Dependency> deps = project.getDependencies();
            if (traversing) {
                deps = Lists.filter(deps, it -> filterDependency(it, project, null));
            }
            artifacts.add(collectDeps(project, deps, repos, null));
        }
        if (includeDepsMgmt) {
            artifacts.add(collectDepMgmt(project, project.getDependencyManagement(), repos, null));
        }
        if (includePlugins) {
            artifacts.add(collectPlugins(project, project.getBuildPlugins(), pluginRepos, null));
        }
        if (includePluginMgmt) {
            artifacts.add(collectPluginMgmt(project, project.getPluginManagement(), pluginRepos, null));
        }

        MavenProject currentProject = project;
        while (currentProject != null) {
            List<String> activeProfileIds = Lists.map(currentProject.getActiveProfiles(), Profile::getId);
            for (Profile profile : currentProject.getModel().getProfiles()) {
                String profileId = profile.getId();
                if (activeProfileIds.contains(profileId) && profileFilter.test(profileId)) {
                    continue;
                }

                Set<RemoteRepository> profileRepos = repos(profile.getRepositories(), repos);
                Set<RemoteRepository> profilePluginRepos = repos(profile.getPluginRepositories(), pluginRepos);

                if (includeDeps) {
                    List<Dependency> deps = profile.getDependencies();
                    if (traversing) {
                        deps = Lists.filter(deps, it -> filterDependency(it, project, profileId));
                    }
                    artifacts.add(collectDeps(currentProject, deps, profileRepos, profileId));
                }
                if (includeDepsMgmt) {
                    DependencyManagement mgmt = profile.getDependencyManagement();
                    artifacts.add(collectDepMgmt(currentProject, mgmt, profileRepos, profileId));
                }

                BuildBase profileBuild = profile.getBuild();
                if (includePlugins) {
                    List<Plugin> plugins = profileBuild != null ? profileBuild.getPlugins() : List.of();
                    artifacts.add(collectPlugins(currentProject, plugins, profilePluginRepos, profileId));
                }
                if (includePluginMgmt) {
                    PluginManagement mgmt = profileBuild != null ? profileBuild.getPluginManagement() : null;
                    artifacts.add(collectPluginMgmt(currentProject, mgmt, profilePluginRepos, profileId));
                }
            }
            currentProject = currentProject.getParent();
        }
        return Lists.filter(artifacts, it -> !it.artifacts.isEmpty());
    }

    private ResolvableArtifacts collectDeps(MavenProject project,
                                            List<Dependency> deps,
                                            Set<RemoteRepository> repos,
                                            String profileId) {

        List<MavenArtifact> artifacts = new ArrayList<>();
        List<Dependency> filtered = Lists.filter(deps, it -> filterDependency(it, project, profileId));
        filtered.forEach(it -> artifacts.add(new MavenArtifact(it, () -> version(project, it, profileId))));
        return new ResolvableArtifacts(artifacts, repos);
    }

    private ResolvableArtifacts collectDepMgmt(MavenProject project,
                                               DependencyManagement mgmt,
                                               Set<RemoteRepository> repos,
                                               String profileId) {

        List<MavenArtifact> artifacts = new ArrayList<>();
        if (mgmt != null) {
            mgmt.getDependencies().forEach(it -> artifacts.add(new MavenArtifact(it, () -> version(project, it, profileId))));
        }
        return new ResolvableArtifacts(artifacts, repos);
    }

    private ResolvableArtifacts collectPlugins(MavenProject project,
                                               List<Plugin> plugins,
                                               Set<RemoteRepository> repos,
                                               String profileId) {

        List<MavenArtifact> artifacts = new ArrayList<>();
        plugins.forEach(it -> artifacts.add(new MavenArtifact(it, () -> version(project, it, profileId))));
        return new ResolvableArtifacts(artifacts, repos);
    }

    private ResolvableArtifacts collectPluginMgmt(MavenProject project,
                                                  PluginManagement mgmt,
                                                  Set<RemoteRepository> repos,
                                                  String profileId) {

        List<MavenArtifact> artifacts = new ArrayList<>();
        if (mgmt != null) {
            mgmt.getPlugins().forEach(it -> artifacts.add(new MavenArtifact(it, () -> version(project, it, profileId))));
        }
        return new ResolvableArtifacts(artifacts, repos);
    }

    private String optional(MavenProject project, Dependency d, String profileId) {
        return effectiveValue(project, d, profileId, this::optional, MavenProject::getDependencyManagement,
                Profile::getDependencyManagement);
    }

    private String scope(MavenProject project, Dependency d, String profileId) {
        return effectiveValue(project, d, profileId, this::scope, MavenProject::getDependencyManagement,
                Profile::getDependencyManagement);
    }

    private String version(MavenProject project, Dependency d, String profileId) {
        return effectiveValue(project, d, profileId, this::version, MavenProject::getDependencyManagement,
                Profile::getDependencyManagement);
    }

    private String version(MavenProject project, Plugin p, String profileId) {
        return effectiveValue(project, p, profileId, this::version, MavenProject::getPluginManagement,
                it -> it.getBuild() != null ? it.getBuild().getPluginManagement() : null);
    }

    private <T, U, R> R effectiveValue(MavenProject project,
                                       U u,
                                       String profileId,
                                       BiFunction<T, U, R> f0,
                                       Function<MavenProject, T> f1,
                                       Function<Profile, T> f2) {
        R value = f0.apply(f1.apply(project), u);
        if (value == null && profileId != null) {
            MavenProject currentProject = project;
            while (value == null && currentProject != null) {
                for (Profile profile : currentProject.getModel().getProfiles()) {
                    if (profile.getId().equals(profileId)) {
                        value = f0.apply(f2.apply(profile), u);
                        break;
                    }
                }
                currentProject = currentProject.getParent();
            }
        }
        return value;
    }

    private String optional(DependencyManagement mgmt, Dependency d) {
        if (mgmt != null) {
            return mgmt.getDependencies().stream()
                    .filter(it -> it.getGroupId().equals(d.getGroupId()) && it.getArtifactId().equals(d.getArtifactId()))
                    .findFirst()
                    .map(Dependency::getOptional)
                    .orElse(null);
        }
        return null;
    }

    private String scope(DependencyManagement mgmt, Dependency d) {
        if (mgmt != null) {
            return mgmt.getDependencies().stream()
                    .filter(it -> it.getGroupId().equals(d.getGroupId()) && it.getArtifactId().equals(d.getArtifactId()))
                    .findFirst()
                    .map(Dependency::getScope)
                    .orElse(null);
        }
        return null;
    }

    private String version(DependencyManagement mgmt, Dependency d) {
        if (mgmt != null) {
            return mgmt.getDependencies().stream()
                    .filter(it -> it.getGroupId().equals(d.getGroupId()) && it.getArtifactId().equals(d.getArtifactId()))
                    .findFirst()
                    .map(Dependency::getVersion)
                    .orElse(null);
        }
        return null;
    }

    private String version(PluginManagement mgmt, Plugin p) {
        if (mgmt != null) {
            return mgmt.getPlugins().stream()
                    .filter(it -> it.getGroupId().equals(p.getGroupId()) && it.getArtifactId().equals(p.getArtifactId()))
                    .findFirst()
                    .map(Plugin::getVersion)
                    .orElse(null);
        }
        return null;
    }

    private RemoteRepository repository(Repository repo) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(repo.getId(), repo.getLayout(), repo.getUrl());
        org.apache.maven.model.RepositoryPolicy releasePolicy = repo.getReleases();
        if ((releasePolicy == null || !releasePolicy.isEnabled()) && !includeSnapshots) {
            // discard the repo if snapshot only and includeSnapshots is false
            return null;
        }
        if (releasePolicy != null) {
            String updatePolicy = releasePolicy.getUpdatePolicy();
            String checksumPolicy = releasePolicy.getChecksumPolicy();
            builder.setReleasePolicy(new RepositoryPolicy(
                    releasePolicy.isEnabled(),
                    Strings.isValid(updatePolicy) ? updatePolicy : "always",
                    Strings.isValid(checksumPolicy) ? checksumPolicy : "ignore"));
        }
        org.apache.maven.model.RepositoryPolicy snapshotPolicy = repo.getSnapshots();
        if (snapshotPolicy != null) {
            String updatePolicy = snapshotPolicy.getUpdatePolicy();
            String checksumPolicy = snapshotPolicy.getChecksumPolicy();
            builder.setSnapshotPolicy(new RepositoryPolicy(
                    snapshotPolicy.isEnabled() && includeSnapshots,
                    Strings.isValid(updatePolicy) ? updatePolicy : "always",
                    Strings.isValid(checksumPolicy) ? checksumPolicy : "ignore"));
        }
        return builder.build();
    }

    private Set<RemoteRepository> repos(List<Repository> repositories, Set<RemoteRepository> initial) {
        Set<RemoteRepository> result = new HashSet<>(initial);
        for (Repository repo : repositories) {
            RemoteRepository remote = repository(repo);
            if (remote != null) {
                result.add(remote);
            }
        }
        return result;
    }

    private record ResolvableArtifacts(List<MavenArtifact> artifacts, Set<RemoteRepository> repos) {
    }
}
