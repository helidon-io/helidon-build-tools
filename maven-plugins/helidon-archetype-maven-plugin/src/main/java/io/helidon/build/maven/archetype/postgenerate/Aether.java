/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype.postgenerate;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.internal.transport.wagon.PlexusWagonProvider;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import static java.util.stream.Collectors.toList;

/**
 * Standalone aether utility.
 */
final class Aether {

    private final List<RemoteRepository> remoteRepos;
    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;

    /**
     * Create a new instance.
     *
     * @param repoSession repository system session
     * @param remoteRepos remote repositories
     */
    Aether(RepositorySystemSession repoSession, List<RemoteRepository> remoteRepos) {
        this.repoSession = repoSession;
        this.repoSystem = repoSystem();
        this.remoteRepos = remoteRepos;
    }

    /**
     * Create a new instance.
     *
     * @param repoSession   repository system session
     * @param artifactRepos remote artifact repositories
     * @param ignored       used for pseudo overload of {@code artifactRepos}
     */
    Aether(RepositorySystemSession repoSession, List<ArtifactRepository> artifactRepos, boolean ignored) {
        this.repoSession = repoSession;
        this.repoSystem = repoSystem();
        this.remoteRepos = artifactRepos.stream().map(this::remoteRepo).collect(Collectors.toList());
    }

    /**
     * Get the repository system session.
     *
     * @return RepositorySystemSession
     */
    RepositorySystemSession repoSession() {
        return repoSession;
    }

    /**
     * Resolve the given artifact.
     *
     * @param groupId    the groupId
     * @param artifactId the artifactId
     * @param type       the type e.g. {@code "jar"}
     * @param version    the version
     * @return artifact file
     */
    @SuppressWarnings("SameParameterValue")
    File resolveArtifact(String groupId, String artifactId, String type, String version) {
        try {
            return repoSystem
                    .resolveArtifact(repoSession, new ArtifactRequest()
                            .setArtifact(new DefaultArtifact(groupId, artifactId, type, version)))
                    .getArtifact()
                    .getFile();
        } catch (ArtifactResolutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Resolve transitive dependencies of the given GAV.
     *
     * @param gav GAV string (groupId:artifactId:version)
     * @return list of files
     */
    List<File> resolveDependencies(String gav) {
        try {
            return repoSystem
                    .resolveDependencies(repoSession, new DependencyRequest()
                            .setCollectRequest(new CollectRequest()
                                    .setRoot(new Dependency(new DefaultArtifact(gav), "compile"))
                                    .setRepositories(remoteRepos))
                            .setFilter(DependencyFilterUtils.classpathFilter("runtime")))
                    .getArtifactResults()
                    .stream()
                    .map(ar -> ar.getArtifact().getFile())
                    .collect(toList());
        } catch (DependencyResolutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Resolve transitive dependencies of the given GAVs.
     *
     * @param coords list of GAV (groupId:artifactId:version)
     * @return list of files
     */
    List<File> resolveDependencies(List<String> coords) {
        List<File> files = new LinkedList<>();
        for (String gav : coords) {
            files.addAll(resolveDependencies(gav));
        }
        return files;
    }

    private RemoteRepository remoteRepo(ArtifactRepository repo) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(repo.getId(), repo.getLayout().getId(),
                repo.getUrl());
        ArtifactRepositoryPolicy releases = repo.getReleases();
        if (releases != null) {
            RepositoryPolicy releasePolicy = new RepositoryPolicy(releases.isEnabled(), releases.getUpdatePolicy(),
                    releases.getChecksumPolicy());
            builder.setReleasePolicy(releasePolicy);
        }
        ArtifactRepositoryPolicy snapshots = repo.getSnapshots();
        if (snapshots != null) {
            RepositoryPolicy snapshotPolicy = new RepositoryPolicy(snapshots.isEnabled(), snapshots.getUpdatePolicy(),
                    snapshots.getChecksumPolicy());
            builder.setSnapshotPolicy(snapshotPolicy);
        }
        RemoteRepository repository = builder.build();
        return new RemoteRepository.Builder(repository)
                .setProxy(repoSession.getProxySelector().getProxy(repository))
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes", "SameParameterValue"})
    private static ComponentDescriptor plexusDesc(Class<?> roleClass, String roleHint, Class<?> implClass) {
        ComponentDescriptor desc = new ComponentDescriptor();
        desc.setRoleClass(roleClass);
        desc.setRoleHint(roleHint);
        desc.setImplementationClass(implClass);
        desc.setInstantiationStrategy("per-lookup");
        return desc;
    }

    @SuppressWarnings("unchecked")
    private static RepositorySystem repoSystem() {
        try {
            PlexusContainer container = new DefaultPlexusContainer();
            container.addComponentDescriptor(plexusDesc(Wagon.class, "http", HttpWagon.class));
            container.addComponentDescriptor(plexusDesc(Wagon.class, "https", HttpWagon.class));
            return MavenRepositorySystemUtils
                    .newServiceLocator()
                    .setServices(WagonProvider.class, new PlexusWagonProvider(container))
                    .addService(TransporterFactory.class, WagonTransporterFactory.class)
                    .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
                    .getService(RepositorySystem.class);
        } catch (CycleDetectedInComponentGraphException | PlexusContainerException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
