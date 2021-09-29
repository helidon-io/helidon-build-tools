/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.dev.maven;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.util.Strings;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.lifecycle.internal.ProjectIndex;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import static java.util.Objects.requireNonNull;

/**
 * An accessor for various Maven components.
 */
public class MavenEnvironment {

    private final MavenProject project;
    private final MavenSession session;
    private final MojoDescriptorCreator mojoDescriptorCreator;
    private final DefaultLifecycles defaultLifeCycles;
    private final LifecycleMappingDelegate standardLifecycleDelegate;
    private final Map<String, LifecycleMappingDelegate> lifecycleDelegates;
    private final BuildPluginManager pluginManager;
    private final MojoExecutor mojoExecutor;
    private final ProjectIndex projectIndex;

    /**
     * Constructor.
     *
     * @param project                   The project.
     * @param session                   The session.
     * @param mojoDescriptorCreator     The mojo descriptor creator.
     * @param defaultLifeCycles         The default lifecycles.
     * @param standardLifecycleDelegate The standard lifecycle mapping delegate.
     * @param lifecycleDelegates        The lifecycle delegates, by phase.
     * @param buildPluginManager        The build plugin manager.
     * @param mojoExecutor              The mojo executor
     */
    public MavenEnvironment(MavenProject project,
                            MavenSession session,
                            MojoDescriptorCreator mojoDescriptorCreator,
                            DefaultLifecycles defaultLifeCycles,
                            LifecycleMappingDelegate standardLifecycleDelegate,
                            Map<String, LifecycleMappingDelegate> lifecycleDelegates,
                            BuildPluginManager buildPluginManager,
                            MojoExecutor mojoExecutor) {

        this.project = project;
        this.session = session;
        this.projectIndex = new ProjectIndex(session.getProjects());
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.defaultLifeCycles = defaultLifeCycles;
        this.standardLifecycleDelegate = standardLifecycleDelegate;
        this.lifecycleDelegates = lifecycleDelegates;
        this.pluginManager = buildPluginManager;
        this.mojoExecutor = mojoExecutor;
    }

    /**
     * Returns the project.
     *
     * @return The project.
     */
    public MavenProject project() {
        return project;
    }

    /**
     * Returns the session.
     *
     * @return The session.
     */
    public MavenSession session() {
        return session;
    }

    /**
     * Execute a given mojo execution.
     *
     * @param execution mojo execution
     */
    public void execute(MojoExecution execution) {
        try {
            mojoExecutor.execute(session, List.of(execution), projectIndex);
        } catch (LifecycleExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a mojo execution.
     *
     * @param pluginKey   plugin key (pluginGroupId : pluginArtifactId)
     * @param goal        plugin goal
     * @param executionId execution id
     * @return MojoExecution
     */
    public MojoExecution execution(String pluginKey, String goal, String executionId) {
        Plugin plugin = plugin(project, pluginKey);
        MojoDescriptor mojoDescriptor;
        try {
            mojoDescriptor = mojoDescriptor(plugin, goal);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        Xpp3Dom configuration = configuration(plugin, executionId);
        return mojoExecution(mojoDescriptor, executionId, configuration);
    }

    /**
     * Create a new goal.
     * @param prefix plugin prefix
     * @param goal plugin goal
     * @param executionId execution id
     * @return MavenGoal
     * @throws Exception if the plugin is not found using the prefix
     */
    public MavenGoal goal(String prefix, String goal, String executionId) throws Exception {
        final Plugin plugin = mojoDescriptorCreator.findPluginForPrefix(prefix, session);
        return MavenGoal.create(plugin.getGroupId(), plugin.getArtifactId(), goal, executionId, this);
    }

    /**
     * Resolve the goals for the lifecycle of the given phase.
     *
     * @param phase maven phase
     * @return list of MavenGoal, never {@code null}
     * @throws LifecyclePhaseNotFoundException if the phase is unknown
     */
    public List<MavenGoal> phase(String phase) throws Exception {
        LifecycleMappingDelegate delegate = standardLifecycleDelegate;
        Lifecycle lifecycle = lifecycle(phase);
        if (Arrays.binarySearch(DefaultLifecycles.STANDARD_LIFECYCLES, lifecycle.getId()) < 0) {
            delegate = lifecycleDelegates.get(lifecycle.getId());
            if (delegate == null) {
                delegate = standardLifecycleDelegate;
            }
        }
        // Collect the executions for this phase rather
        return delegate.calculateLifecycleMappings(session, project, lifecycle, phase)
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .flatMap(e -> e.getValue().stream())
                .map(this::goal)
                .collect(Collectors.toList());
    }

    /**
     * Get the lifecycle for the given phase.
     *
     * @param phase maven phase
     * @return lifecycle, never {@code null}
     * @throws LifecyclePhaseNotFoundException if the phase is unknown
     */
    public Lifecycle lifecycle(String phase) throws Exception {
        DefaultLifecycles defaultLifecycles = defaultLifeCycles;
        Lifecycle lifecycle = defaultLifecycles.get(phase);
        if (lifecycle == null) {
            throw new LifecyclePhaseNotFoundException("Unknown lifecycle phase \"" + phase
                    + "\". You must specify a valid lifecycle phase" + " or a goal in the "
                    + "format <plugin-prefix>:<goal> or "
                    + "<plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. "
                    + "Available lifecycle phases are: "
                    + defaultLifecycles.getLifecyclePhaseList() + ".", phase);
        }
        return lifecycle;
    }

    private MavenGoal goal(MojoExecution execution) {
        return MavenGoal.create(execution.getGroupId(), execution.getArtifactId(),
                execution.getGoal(), execution.getExecutionId(), this);
    }

    private static Plugin plugin(MavenProject project, String pluginKey) {
        final Plugin plugin = requireNonNull(project.getPlugin(pluginKey), "plugin " + pluginKey + " not found");
        if (Strings.isNotValid(plugin.getVersion())) {
            final PluginManagement pm = project.getPluginManagement();
            if (pm != null) {
                for (Plugin p : pm.getPlugins()) {
                    if (plugin.getGroupId().equals(p.getGroupId()) && plugin.getArtifactId().equals(p.getArtifactId())) {
                        plugin.setVersion(p.getVersion());
                        break;
                    }
                }
            }
        }
        return plugin;
    }

    private MojoDescriptor mojoDescriptor(Plugin plugin, String goal) throws Exception {
        final RepositorySystemSession repositorySession = session.getRepositorySession();
        final List<RemoteRepository> repositories = project.getRemotePluginRepositories();
        final PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(plugin, repositories, repositorySession);
        return pluginDescriptor.getMojo(goal);
    }

    private static Xpp3Dom configuration(Plugin plugin, String executionId) {
        final PluginExecution execution = plugin.getExecutionsAsMap().get(executionId);
        if (execution != null && execution.getConfiguration() != null) {
            return (Xpp3Dom) execution.getConfiguration();
        } else if (plugin.getConfiguration() != null) {
            return (Xpp3Dom) plugin.getConfiguration();
        } else {
            return new Xpp3Dom("configuration");
        }
    }

    private static MojoExecution mojoExecution(MojoDescriptor mojoDescriptor, String executionId, Xpp3Dom executionConfig) {
        final MojoExecution result = new MojoExecution(mojoDescriptor, executionId);
        final Xpp3Dom configuration = mojoConfiguration(mojoDescriptor, executionConfig);
        result.setConfiguration(configuration);
        return result;
    }

    /**
     * Returns the final mojo configuration, discarding all parameters that are not applicable to the mojo and injecting
     * the default values for any missing parameters.
     *
     * <em>NOTE</em>Copied/modified from {@code org.apache.maven.lifecycle.internal.DefaultLifecycleExecutionPlanCalculator}.
     *
     * @param mojoDescriptor         The mojo descriptor. Must not be {@code null}.
     * @param executionConfiguration The execution configuration. Must not be {@code null}.
     * @return The final configuration.
     */
    private static Xpp3Dom mojoConfiguration(MojoDescriptor mojoDescriptor, Xpp3Dom executionConfiguration) {
        final Xpp3Dom finalConfiguration = new Xpp3Dom("configuration");
        final Xpp3Dom defaultConfiguration = MojoDescriptorCreator.convert(mojoDescriptor);
        if (mojoDescriptor.getParameters() != null) {
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                Xpp3Dom parameterConfiguration = executionConfiguration.getChild(parameter.getName());
                if (parameterConfiguration == null) {
                    parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
                }

                Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());

                parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults, Boolean.TRUE);

                if (parameterConfiguration != null) {
                    parameterConfiguration = new Xpp3Dom(parameterConfiguration, parameter.getName());

                    if (StringUtils.isEmpty(parameterConfiguration.getAttribute("implementation"))
                            && StringUtils.isNotEmpty(parameter.getImplementation())) {
                        parameterConfiguration.setAttribute("implementation", parameter.getImplementation());
                    }

                    finalConfiguration.addChild(parameterConfiguration);
                }
            }
        }
        return finalConfiguration;
    }
}
