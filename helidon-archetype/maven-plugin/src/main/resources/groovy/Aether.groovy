/*
* Copyright (c) 2020 Oracle and/or its affiliates.
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

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.apache.maven.wagon.Wagon
import org.apache.maven.wagon.providers.http.HttpWagon
import org.codehaus.plexus.DefaultPlexusContainer
import org.codehaus.plexus.component.repository.ComponentDescriptor
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.internal.transport.wagon.PlexusWagonProvider
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.Proxy
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.wagon.WagonProvider
import org.eclipse.aether.transport.wagon.WagonTransporterFactory
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.util.repository.AuthenticationBuilder

/**
 * Standalone aether utility.
 */
class AetherImpl {

    private List<RemoteRepository> remoteRepos
    private RepositorySystem repoSystem
    private RepositorySystemSession repoSession

    @SuppressWarnings("GroovyAssignabilityCheck")
    AetherImpl(File localRepoDir, remoteArtifactRepos) {
        def plexusContainer = new DefaultPlexusContainer()
        plexusContainer.addComponentDescriptor(plexusDesc(Wagon.class, "http", HttpWagon.class, "per-lookup"))
        plexusContainer.addComponentDescriptor(plexusDesc(Wagon.class, "https", HttpWagon.class, "per-lookup"))
        repoSystem = MavenRepositorySystemUtils.newServiceLocator()
                .setServices(WagonProvider.class, new PlexusWagonProvider(plexusContainer))
                .addService(TransporterFactory.class, WagonTransporterFactory.class)
                .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
                .getService(RepositorySystem.class)
        repoSession = MavenRepositorySystemUtils.newSession()
        repoSession.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(repoSession,
                new LocalRepository(localRepoDir)))
        remoteRepos = remoteArtifactRepos.collect { remoteRepo(it) }
    }

    private static ComponentDescriptor plexusDesc(Class<?>roleClass,
                                                  String roleHint,
                                                  Class<?> implClass,
                                                  String strategy) {
        def desc = new ComponentDescriptor()
        desc.setRoleClass(roleClass)
        desc.setRoleHint(roleHint)
        desc.setImplementationClass(implClass)
        desc.setInstantiationStrategy(strategy)
        return desc
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private static RemoteRepository remoteRepo(aRepo) {
        def builder = new RemoteRepository.Builder(aRepo.getId(), aRepo.getLayout().getId(), aRepo.getUrl());
        def releases = aRepo.getReleases()
        if (releases != null) {
            def releasePolicy = new RepositoryPolicy(releases.isEnabled(), releases.getUpdatePolicy(),
                    releases.getChecksumPolicy())
            builder.setReleasePolicy(releasePolicy)
        }
        def snapshots = aRepo.getSnapshots()
        if (snapshots != null) {
            def snapshotPolicy = new RepositoryPolicy(snapshots.isEnabled(), snapshots.getUpdatePolicy(),
                    snapshots.getChecksumPolicy())
            builder.setSnapshotPolicy(snapshotPolicy)
        }
        def proxy = aRepo.getProxy()
        if (proxy != null) {
            def authentication = new AuthenticationBuilder()
                    .addUsername(proxy.getUserName())
                    .addPassword((String) proxy.getPassword())
                    .build()
            builder.setProxy(new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authentication));
        }
        return builder.build();
    }

    /**
     * Resolve the given artifact.
     *
     * @param groupId the groupId
     * @param artifactId the artifactId
     * @param type the type e.g. {@code "jar"}
     * @param version the version
     * @return artifact file
     */
    File resolveArtifact(String groupId, String artifactId, String type, String version) {
        return repoSystem
                .resolveArtifact(repoSession, new ArtifactRequest()
                        .setArtifact(new DefaultArtifact(groupId, artifactId, type, version)))
                .getArtifact()
                .getFile()
    }

    /**
     * Resolve transitive dependencies of the given GAV.
     *
     * @param gav GAV string (groupId:artifactId:version)
     * @return list of files
     */
    List<File> resolveDependencies(String gav) {
        return repoSystem
                .resolveDependencies(repoSession, new DependencyRequest()
                        .setCollectRequest(new CollectRequest()
                                .setRoot(new Dependency(new DefaultArtifact(gav), "compile"))
                                .setRepositories(remoteRepos))
                        .setFilter(DependencyFilterUtils.classpathFilter("runtime")))
                .getArtifactResults()
                .collect { it.getArtifact().getFile() }
    }
}

/**
 * Create a new Aether instance.
 * @param localRepoDir local repository directory
 * @param remoteArtifactRepos remote artifacts repositories
 * @return AetherImpl
 */
def create(localRepoDir, remoteArtifactRepos) {
    return new AetherImpl(localRepoDir, remoteArtifactRepos)
}

return this