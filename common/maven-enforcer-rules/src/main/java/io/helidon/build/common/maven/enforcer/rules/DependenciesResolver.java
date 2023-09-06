/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven.enforcer.rules;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

class DependenciesResolver {
    private final File pomFile;
    private MavenProject project;
    private MavenSession session;

    DependenciesResolver(MavenProject project,
                         MavenSession session) {
        this(session.getCurrentProject().getFile());
        this.project = Objects.requireNonNull(project);
    }

    DependenciesResolver(File pomFile) {
        this.pomFile = Objects.requireNonNull(pomFile);
    }

//    DependenciesResolver(File pomFile) {
//        this.pomFile = Objects.requireNonNull(pomFile);
//        assert (pomFile.exists() && pomFile.canRead());
//    }

//    List<Gav> resolveOne(Gav gav) {
//        MavenResolverSystem resolver = Maven.resolver();
//        List<Gav> deps = resolver
//                 .resolve(gav.toCanonicalName())
////                .loadPomFromFile(pomFile).importCompileAndRuntimeDependencies().resolve()
//                .withTransitivity().asList(MavenCoordinate.class).stream()
//                .map(DependenciesResolver::toGav)
//                .filter(a -> a.group().startsWith("javax.") || a.group().startsWith("jakarta."))
//                .collect(Collectors.toList());
//        return deps;
//
//    }

    List<Gav> resolveProjectDependencies() {
        if (false) {
            ConfigurableMavenResolverSystem resolver = Maven.configureResolver()
                    .workOffline()
                    .withClassPathResolution(false);
            List<Gav> deps = resolver
                    // .resolve(project.getGroupId()+":"+project.getArtifactId()+":"+project.getVersion())
                    .loadPomFromFile(pomFile).importCompileAndRuntimeDependencies().resolve()
                    .withTransitivity().asList(MavenCoordinate.class).stream()
                    .map(DependenciesResolver::toGav)
                    .filter(a -> a.group().startsWith("javax.") || a.group().startsWith("jakarta."))
                    .collect(Collectors.toList());
            return deps;
        } else {
            //        List<Dependency> deps = project.getDependencies();
            SimpleAether aether = new SimpleAether(SimpleEngineFacade.getLocalRepository(session),
                                                   SimpleEngineFacade.getRemoteArtifactRepositories(project));
            Gav projectGav = toGav(project.getArtifact());
            List<ArtifactResult> result = aether.resolveDependencies(projectGav.toCanonicalName());
            return result.stream()
                    .map(ar -> toGav(ar.getArtifact()))
                    .collect(Collectors.toList());
        }
    }

    //        final File repo = new File(System.getProperty("java.io.tmpdir"), "tmp-repo");
//        final MavenProject project = new MavenProject();
//        project.setRemoteArtifactRepositories(List.of());
//        final Collection<Artifact> deps = new Aether(project, repo).resolve(
//                new DefaultArtifact("junit", "junit-dep", "", "jar", "4.10"),
//                JavaScopes.RUNTIME
//        );
//
//
//        DefaultArtifact pomArtifact = new DefaultArtifact(gav.group(), gav.artifact(), gav.version(), "", "", null, null);
//        Dependency dependency = new Dependency(da, "");
//        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
//
//        CollectRequest collectRequest = new CollectRequest();
//        collectRequest.setRoot(dependency);
//
//        DependencyRequest dependencyRequest = new DependencyRequest();
//        dependencyRequest.setCollectRequest(collectRequest);

//        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
//        request.setArtifact(da);
//
//        DefaultArtifactResolver resolver = new DefaultArtifactResolver();
//        resolver.initService(new DefaultServiceLocator());
//        ArtifactResolutionResult result = resolver.re(request);
//
//
//        return List.of();
//    }


        //    private final RepositorySystem repositorySystem;
        //
        //    public Resolver(MavenProject project,
        //                    MavenSession session) {
        //        RepositorySystemSession repoSession = MavenRepositorySystemUtils.newSession();
        //
        //                //        RepositorySystem repoSystem = null; // TODO
        //
        ////        List<RemoteRepository> remoteRepos = project.getRemoteProjectRepositories();
        //        List<RemoteRepository> remoteRepos = List.of();
        //        List<Dependency> ret = new ArrayList<>();
        //
        //        DefaultArtifact pomArtifact = new DefaultArtifact(project.getId());
        //        Dependency dependency = new Dependency(pomArtifact, "");
        //
        //        CollectRequest collectRequest = new CollectRequest();
        //        collectRequest.setRoot(dependency);
        //        collectRequest.setRepositories(remoteRepos);
        //
        //        DependencyNode node = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
        //        DependencyRequest projectDependencyRequest = new DependencyRequest(node, null);
        //
        //        repoSystem.resolveDependencies(repoSession, projectDependencyRequest);
        //
        //        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        //        node.accept(nlg);
        //
        //        ret.addAll(nlg.getDependencies(true));
        //    }


//    static RepositorySystem repositorySystem() {
//        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
//        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
//        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
//        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
//
//        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
//            @Override
//            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
//                LOGGER.error("Service creation failed for {} with implementation {}", type, impl, exception);
//            }
//        });
//        return locator.getService(RepositorySystem.class);
//    }

    static Gav toGav(MavenCoordinate c) {
        return new Gav(c.getGroupId(), c.getArtifactId(), c.getVersion());
    }

    static Gav toGav(Artifact a) {
        return new Gav(a.getGroupId(), a.getArtifactId(), a.getVersion());
    }

    Gav toGav(org.eclipse.aether.artifact.Artifact a) {
        return new Gav(a.getGroupId(), a.getArtifactId(), a.getVersion());
    }

}
