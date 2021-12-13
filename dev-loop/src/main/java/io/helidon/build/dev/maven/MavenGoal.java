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

import java.io.PrintStream;
import java.util.List;

import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.util.Log;
import io.helidon.build.util.Strings;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
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
 * An executable maven goal. Executions occur in process, in the context of the current project environment.
 */
public class MavenGoal implements BuildStep {

    private static final String DEFAULT_EXECUTION_ID_PREFIX = "default-";

    private final String name;
    private final String pluginKey;
    private final String executionId;
    private final MojoExecution execution;
    private final BuildPluginManager pluginManager;

    /**
     * Returns a new instance.
     *
     * @param pluginGroupId    The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName         The plugin goal to execute.
     * @param executionId      The execution id.
     * @param environment      The plugin execution environment.
     * @return The goal.
     * @throws Exception if an error occurs.
     */
    public static MavenGoal create(String pluginGroupId,
                                   String pluginArtifactId,
                                   String goalName,
                                   String executionId,
                                   MavenEnvironment environment) throws Exception {

        CurrentMavenSession.initialize(environment.session());

        String pluginKey = requireNonNull(pluginGroupId) + ":" + requireNonNull(pluginArtifactId);
        if (executionId == null) {
            executionId = DEFAULT_EXECUTION_ID_PREFIX + goalName;
        }
        Plugin plugin = plugin(environment.project(), pluginKey);
        MojoDescriptor mojoDescriptor = mojoDescriptor(environment, plugin, goalName);
        Xpp3Dom configuration = configuration(plugin, executionId);
        MojoExecution execution = mojoExecution(mojoDescriptor, executionId, configuration);
        BuildPluginManager pluginManager = environment.buildPluginManager();


        return new MavenGoal(pluginKey, goalName, executionId, execution, pluginManager);
    }

    /**
     * Constructor.
     *
     * @param pluginKey The plugin group id.
     * @param goalName The plugin goal to execute.
     * @param executionId The execution id.
     * @param execution The plugin execution.
     * @param pluginManager The plugin manager.
     */
    private MavenGoal(String pluginKey,
                      String goalName,
                      String executionId,
                      MojoExecution execution,
                      BuildPluginManager pluginManager) {
        this.name = goalName;
        this.pluginKey = pluginKey;
        this.executionId = executionId;
        this.execution = execution;
        this.pluginManager = pluginManager;
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes, PrintStream stdOut, PrintStream stdErr) throws Exception {
        if (!changes.isEmpty()) {
            execute();
        }
    }

    /**
     * Executes the goal.
     *
     * @throws Exception if an error occurs.
     */
    public void execute() throws Exception {
        Log.debug("Executing %s", this);
        MavenSession session = CurrentMavenSession.get();
        pluginManager.executeMojo(session, execution);
    }

    /**
     * Returns the plugin goal name.
     *
     * @return The goal name.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the plugin key.
     *
     * @return The key.
     */
    public String pluginKey() {
        return pluginKey;
    }

    /**
     * Returns the plugin key.
     *
     * @return The key.
     */
    public String executionId() {
        return executionId;
    }

    @Override
    public String toString() {
        return pluginKey() + ":" + name() + "@" + executionId();
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

    private static MojoDescriptor mojoDescriptor(MavenEnvironment environment, Plugin plugin, String goal) throws Exception {
        final MavenProject project = environment.project();
        final MavenSession session = environment.session();
        final BuildPluginManager pluginManager = environment.buildPluginManager();
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
     * @param mojoDescriptor The mojo descriptor. Must not be {@code null}.
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
