/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.internal.PluginDependenciesResolver;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import static org.apache.maven.RepositoryUtils.toArtifacts;

/**
 * A plexus component to create plexus containers for plugins.
 */
@Component(role = PluginContainerManager.class, hint = "default")
public class PluginContainerManager {

    @Requirement
    private ClassRealmManager classRealmManager;

    @Requirement
    private PluginDependenciesResolver pluginDependenciesResolver;

    /**
     * Create a new plexus container for a given plugin.
     *
     * @param plugin      plugin coordinates
     * @param repos       remote plugin repositories
     * @param repoSession repository session
     * @return plexus container
     */
    @SuppressWarnings("SameParameterValue")
    public PlexusContainer create(Plugin plugin, List<RemoteRepository> repos, RepositorySystemSession repoSession) {
        try {
            ClassRealm classRealm = pluginClassRealm(plugin, repos, repoSession);
            ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration();
            containerConfiguration.setAutoWiring(true);
            containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
            containerConfiguration.setRealm(classRealm);
            PlexusContainer container = new DefaultPlexusContainer(containerConfiguration);
            container.discoverComponents(classRealm);
            container.discoverComponents(classRealm.getWorld().getRealm("maven.api"));
            container.discoverComponents(classRealm.getWorld().getRealm("plexus.core"));
            return container;
        } catch (PlexusContainerException | PlexusConfigurationException | NoSuchRealmException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private ClassRealm pluginClassRealm(Plugin plugin, List<RemoteRepository> repos, RepositorySystemSession repoSession) {
        try {
            Map<String, ClassLoader> foreignImports = Map.of("", classRealmManager.getMavenApiRealm());
            List<Artifact> artifacts = resolvePluginDependencies(plugin, repos, repoSession);
            return classRealmManager.createPluginRealm(plugin, null, null, foreignImports, artifacts);
        } catch (PluginResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<org.eclipse.aether.artifact.Artifact> resolvePluginDependencies(Plugin plugin,
                                                                                 List<RemoteRepository> repos,
                                                                                 RepositorySystemSession repoSession)
            throws PluginResolutionException {

        org.eclipse.aether.artifact.Artifact pluginArtifact = new DefaultArtifact(
                plugin.getGroupId(), plugin.getArtifactId(), "jar", plugin.getVersion());

        DependencyNode root = pluginDependenciesResolver.resolve(plugin, pluginArtifact, null, repos, repoSession);
        return new ArrayList<>(toArtifacts(expandDependency(root)));
    }

    private static List<org.apache.maven.artifact.Artifact> expandDependency(DependencyNode node) {
        PreorderNodeListGenerator visitor = new PreorderNodeListGenerator();
        node.accept(visitor);
        List<org.apache.maven.artifact.Artifact> artifacts = new ArrayList<>(visitor.getNodes().size());
        toArtifacts(artifacts, List.of(node), List.of(), null);
        artifacts.removeIf(artifact -> artifact.getFile() == null);
        return Collections.unmodifiableList(artifacts);
    }
}
