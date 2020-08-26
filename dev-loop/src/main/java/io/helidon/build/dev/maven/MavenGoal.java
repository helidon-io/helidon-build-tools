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
package io.helidon.build.dev.maven;

import java.util.List;
import java.util.function.Consumer;

import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.util.Log;

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
    private final MavenSession session;

    /**
     * Returns a new instance.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
     * @param executionId The execution id.
     * @param environment The plugin execution environment.
     * @return The goal.
     * @throws Exception if an error occurs.
     */
    public static MavenGoal create(String pluginGroupId,
                                   String pluginArtifactId,
                                   String goalName,
                                   String executionId,
                                   MavenEnvironment environment) throws Exception {
        return new MavenGoal(pluginGroupId, pluginArtifactId, goalName, executionId, environment);
    }

    /**
     * Constructor.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
     * @param executionId The execution id.
     * @param environment The plugin execution environment.
     * @throws Exception if an error occurs.
     */
    private MavenGoal(String pluginGroupId,
                      String pluginArtifactId,
                      String goalName,
                      String executionId,
                      MavenEnvironment environment) throws Exception {
        this.name = requireNonNull(goalName);
        this.pluginKey = requireNonNull(pluginGroupId) + ":" + requireNonNull(pluginArtifactId);
        this.executionId = executionId == null ? DEFAULT_EXECUTION_ID_PREFIX + goalName : executionId;
        final Plugin plugin = plugin(environment.project(), pluginKey);
        final MojoDescriptor mojoDescriptor = mojoDescriptor(environment, plugin, name);
        final Xpp3Dom configuration = configuration(plugin, this.executionId);
        this.execution = mojoExecution(mojoDescriptor, this.executionId, configuration);
        this.pluginManager = environment.buildPluginManager();
        this.session = environment.session();
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes,
                                 Consumer<String> stdOut,
                                 Consumer<String> stdErr) throws Exception {
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
        if ((plugin.getVersion() == null || plugin.getVersion().length() == 0)) {
            PluginManagement pm = project.getPluginManagement();
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

    private static Xpp3Dom configuration(Plugin plugin, String executionId) {
        // Lookup or create default
        final PluginExecution execution = plugin.getExecutionsAsMap().get(executionId);
        final Xpp3Dom configuration;
        if (execution != null && execution.getConfiguration() != null) {
            configuration = (Xpp3Dom) execution.getConfiguration();
        } else if (plugin.getConfiguration() != null) {
            configuration = (Xpp3Dom) plugin.getConfiguration();
        } else {
            configuration = new Xpp3Dom("configuration");
        }
        return configuration;
    }

    private static MojoDescriptor mojoDescriptor(MavenEnvironment environment, Plugin plugin, String goal) throws Exception {
        MavenProject project = environment.project();
        MavenSession session = environment.session();
        BuildPluginManager pluginManager = environment.buildPluginManager();
        RepositorySystemSession repositorySession = session.getRepositorySession();
        List<RemoteRepository> repositories = project.getRemotePluginRepositories();
        PluginDescriptor pluginDescriptor = pluginManager.loadPlugin(plugin, repositories, repositorySession);
        return pluginDescriptor.getMojo(goal);
    }

    private static MojoExecution mojoExecution(MojoDescriptor mojoDescriptor, String executionId, Xpp3Dom configuration) {
        MojoExecution result = new MojoExecution(mojoDescriptor, executionId);
        result.setConfiguration(configuration);
        finalizeMojoConfiguration(result);
        return result;
    }

    /**
     * Post-processes the effective configuration for the specified mojo execution. This step discards all parameters
     * from the configuration that are not applicable to the mojo and injects the default values for any missing
     * parameters.
     *
     * <em>NOTE</em>Copied/modified from {@code org.apache.maven.lifecycle.internal.DefaultLifecycleExecutionPlanCalculator}.
     *
     * @param mojoExecution The mojo execution whose configuration should be finalized, must not be {@code null}.
     */
    private static void finalizeMojoConfiguration(MojoExecution mojoExecution) {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        Xpp3Dom executionConfiguration = mojoExecution.getConfiguration();
        if (executionConfiguration == null) {
            executionConfiguration = new Xpp3Dom("configuration");
        }

        Xpp3Dom defaultConfiguration = MojoDescriptorCreator.convert(mojoDescriptor);

        Xpp3Dom finalConfiguration = new Xpp3Dom("configuration");

        if (mojoDescriptor.getParameters() != null) {
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                Xpp3Dom parameterConfiguration = executionConfiguration.getChild(parameter.getName());

                if (parameterConfiguration == null) {
                    parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
                }

                Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());

                parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults,
                                                              Boolean.TRUE);

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

        mojoExecution.setConfiguration(finalConfiguration);
    }
}
