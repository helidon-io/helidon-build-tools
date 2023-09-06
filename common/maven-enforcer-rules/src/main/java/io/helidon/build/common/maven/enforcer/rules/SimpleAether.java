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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.internal.transport.wagon.PlexusWagonProvider;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

import static java.util.stream.Collectors.toList;

/**
 * Standalone aether utility.
 */
class SimpleAether {
    private final Settings settings;
    private final List<RemoteRepository> remoteRepos;
    private final RepositorySystem repoSystem;
    private final DefaultRepositorySystemSession repoSession;

    /**
     * Create a new Aether instance.
     *
     * @param localRepoDir        local repository directory
     * @param remoteArtifactRepos remote artifacts repositories
     */
    @SuppressWarnings("unchecked")
    SimpleAether(File localRepoDir,
                 List<ArtifactRepository> remoteArtifactRepos) {
        try {
            PlexusContainer container = new DefaultPlexusContainer();
            container.addComponentDescriptor(plexusDesc(Wagon.class, "http", HttpWagon.class));
            container.addComponentDescriptor(plexusDesc(Wagon.class, "https", HttpWagon.class));
            repoSystem = MavenRepositorySystemUtils
                    .newServiceLocator()
                    .setServices(WagonProvider.class, new PlexusWagonProvider(container))
                    .addService(TransporterFactory.class, WagonTransporterFactory.class)
                    .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
                    .getService(RepositorySystem.class);
            settings = settings();
//            if (activeProfiles != null) {
//                settings.setActiveProfiles(activeProfiles);
//            }
            repoSession = MavenRepositorySystemUtils.newSession();
//            repoSession.setTransferListener(new Slf4jMavenTransferListener());
            repoSession.setProxySelector(proxySelector());
            repoSession.setMirrorSelector(mirrorSelector());
            repoSession.setLocalRepositoryManager(repoSystem
                                                          .newLocalRepositoryManager(repoSession, new LocalRepository(localRepoDir)));
            remoteRepos = remoteArtifactRepos.stream().map(this::remoteRepo).collect(toList());
        } catch (CycleDetectedInComponentGraphException | PlexusContainerException ex) {
            throw new IllegalStateException(ex);
        }
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

    private static Settings settings() {
        DefaultSettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        String userSettings = System.getProperty("org.apache.maven.user-settings");
        if (userSettings == null) {
            File userHome = new File(System.getProperty("user.home")).getAbsoluteFile();
            request.setUserSettingsFile(new File(userHome, "/.m2/settings.xml"));
        } else {
            request.setUserSettingsFile(new File(userSettings));
        }
        String globalSettings = System.getProperty("org.apache.maven.global-settings");
        if (globalSettings != null) {
            request.setGlobalSettingsFile(new File(globalSettings));
        }
        try {
            return builder.build(request).getEffectiveSettings();
        } catch (SettingsBuildingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private MirrorSelector mirrorSelector() {
        DefaultMirrorSelector selector = new DefaultMirrorSelector();
        List<Mirror> mirrors = settings.getMirrors();
        if (mirrors != null) {
            for (Mirror mirror : mirrors) {
                selector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false,
                             mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
            }
        }
        return selector;
    }

    private ProxySelector proxySelector() {
        org.apache.maven.settings.Proxy proxyDef = settings.getActiveProxy();
        if (proxyDef != null) {
            Authentication auth = null;
            if (proxyDef.getUsername() != null) {
                auth = new AuthenticationBuilder().addString(proxyDef.getUsername(), proxyDef.getPassword()).build();
            }
            DefaultProxySelector selector = new DefaultProxySelector();
            Proxy proxy = new Proxy(proxyDef.getProtocol(), proxyDef.getHost(), proxyDef.getPort(), auth);
            selector.add(proxy, proxyDef.getNonProxyHosts());
            return selector;
        }
        return null;
    }

    private RemoteRepository remoteRepo(ArtifactRepository aRepo) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(aRepo.getId(), aRepo.getLayout().getId(),
                                                                        aRepo.getUrl());
        ArtifactRepositoryPolicy releases = aRepo.getReleases();
        if (releases != null) {
            RepositoryPolicy releasePolicy = new RepositoryPolicy(releases.isEnabled(), releases.getUpdatePolicy(),
                                                                  releases.getChecksumPolicy());
            builder.setReleasePolicy(releasePolicy);
        }
        ArtifactRepositoryPolicy snapshots = aRepo.getSnapshots();
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

    /**
     * Resolve transitive dependencies of the given GAV.
     *
     * @param gav GAV string (groupId:artifactId:version)
     * @return list of artifact results
     */
    List<ArtifactResult> resolveDependencies(String gav) {
        try {
            return repoSystem
                    .resolveDependencies(repoSession, new DependencyRequest()
                            .setCollectRequest(new CollectRequest()
                                                       .setRoot(new Dependency(new DefaultArtifact(gav), "compile"))
                                                       .setRepositories(remoteRepos))
                            .setFilter(DependencyFilterUtils.classpathFilter("runtime")))
                    .getArtifactResults();
        } catch (DependencyResolutionException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
