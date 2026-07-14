/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import io.helidon.build.common.Proxies;
import io.helidon.build.common.Unchecked;
import io.helidon.build.common.maven.plugin.Xpp3DomAdapter;
import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.maven.stager.ExecutorConfig.ExecutorKind;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * {@code stager:stage} mojo.
 */
@Mojo(name = "stage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false)
public class StagerMojo extends AbstractMojo {

    private static final String PROXY_HOST_PROP_SUFFIX = ".proxyHost";
    private static final String PROXY_PORT_PROP_SUFFIX = ".proxyPort";
    private static final String NON_PROXY_PROP_SUFFIX = ".nonProxyHosts";

    /**
     * Manager used to look up Archiver/UnArchiver implementations.
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * The entry point to Aether.
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The Maven settings.
     */
    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    /**
     * The current Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * The current mojo execution.
     */
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    private MojoExecution mojoExecution;

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

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
     * The project build output directory. (e.g. {@code target/})
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File outputDirectory;

    /**
     * The project base directory. (e.g. {@code ./})
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File baseDirectory;

    /**
     * The directories to stage.
     */
    @Parameter
    private PlexusConfiguration directories;

    /**
     * Stager properties for POM-root configuration.
     */
    @Parameter
    private PlexusConfiguration properties;

    /**
     * Stager variables for POM-root configuration.
     */
    @Parameter
    private PlexusConfiguration variables;

    /**
     * Stager include for POM-root configuration.
     */
    @Parameter
    private PlexusConfiguration include;

    /**
     * Standalone stager XML configuration file.
     */
    @Parameter(property = "stager.configFile")
    private File configFile;

    /**
     * {@code readTimeout} configuration (in ms) for the download task.
     */
    @Parameter(defaultValue = "-1", property = StagingContext.READ_TIMEOUT_PROP)
    private int readTimeout;

    /**
     * {@code connectTimeout} configuration (in ms) for the download task.
     */
    @Parameter(defaultValue = "-1", property = StagingContext.CONNECT_TIMEOUT_PROP)
    private int connectTimeout;

    /**
     * {@code taskTimeout} configuration (in ms) for the tasks that support timeouts.
     */
    @Parameter(defaultValue = "-1", property = StagingContext.TASK_TIMEOUT_PROP)
    private int taskTimeout;

    /**
     * {@code maxRetries} configuration for the tasks that support retries.
     */
    @Parameter(defaultValue = "5", property = StagingContext.MAX_RETRIES)
    private int maxRetries;

    /**
     * Skip this goal execution.
     */
    @Parameter(property = "stager.skip", defaultValue = "false")
    private boolean skip;

    @Parameter
    private ExecutorConfig executor;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            return;
        }

        XMLElement config = effectiveConfig();
        if (config.children("directory").isEmpty()) {
            return;
        }

        ExecutorService executorService = executor();
        Path outputDir = outputDir();
        StagingContext context = new StagingContextImpl(
                baseDir().toFile(),
                outputDir.toFile(),
                getLog(),
                repoSystem,
                repoSession,
                remoteRepos,
                archiverManager,
                executorService,
                this::resolveProperty);

        Proxies.setProxyPropertiesFromEnv();
        setProxyFromSettings();

        try {
            StagingTasks tasks = StagingFactory.create(config);
            tasks.execute(context, outputDir, Map.of())
                    .toCompletableFuture()
                    .get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            throw Unchecked.wrap(ex.getCause());
        }
    }

    ExecutorService executor() {
        if (executor == null) {
            Properties userProperties = session.getUserProperties();
            Map<String, String> parameters = new HashMap<>();
            for (String s : userProperties.stringPropertyNames()) {
                if (s.startsWith("stager.executor.")) {
                    parameters.put(s.substring("stager.executor.".length()), userProperties.getProperty(s));
                }
            }
            ExecutorKind kind = ExecutorKind.valueOf(parameters.getOrDefault("kind", "DEFAULT"));
            ExecutorConfig config = new ExecutorConfig(kind, parameters);
            return config.select();
        }
        return executor.select();
    }

    private XMLElement effectiveConfig() throws MojoExecutionException {
        if (configFile != null) {
            Path configPath = configFile.toPath().toAbsolutePath().normalize();
            XMLElement root = XMLElement.read(configPath, configFile.toString(), true);
            return ConfigProcessor.process(root, configPath.getParent(), this::resolveProperty);
        }
        XMLElement root = Xpp3DomAdapter.create(mojoConfiguration());
        return ConfigProcessor.process(root, baseDir(), this::resolveProperty);
    }

    private Xpp3Dom mojoConfiguration() throws MojoExecutionException {
        for (PluginExecution execution : mojoExecution.getPlugin().getExecutions()) {
            if (execution.getId().equals(mojoExecution.getExecutionId())) {
                if (execution.getConfiguration() instanceof Xpp3Dom mojoConfig) {
                    return mojoConfig;
                }
            }
        }
        throw new MojoExecutionException("Unable to resolve configuration");
    }

    private Path baseDir() {
        if (baseDirectory != null) {
            return baseDirectory.toPath();
        }
        if (project != null && project.getBasedir() != null) {
            return project.getBasedir().toPath();
        }
        return Path.of("").toAbsolutePath();
    }

    private Path outputDir() {
        if (outputDirectory != null) {
            Path path = outputDirectory.toPath();
            if (!path.toString().contains("${")) {
                return path;
            }
        }
        return baseDir().resolve("target");
    }

    private String resolveProperty(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String expression = "${" + name + "}";
        Object value = evaluateExpression(expression);
        if (value instanceof String result && !expression.equals(result)) {
            return result;
        }

        switch (name) {
            case StagingContext.READ_TIMEOUT_PROP:
                if (readTimeout >= 0) {
                    return String.valueOf(readTimeout);
                }
                break;
            case StagingContext.CONNECT_TIMEOUT_PROP:
                if (connectTimeout >= 0) {
                    return String.valueOf(connectTimeout);
                }
                break;
            case StagingContext.TASK_TIMEOUT_PROP:
                if (taskTimeout >= 0) {
                    return String.valueOf(taskTimeout);
                }
                break;
            case StagingContext.MAX_RETRIES:
                if (maxRetries >= 0) {
                    return String.valueOf(maxRetries);
                }
                break;
            default:
                return null;
        }
        return null;
    }

    private Object evaluateExpression(String expression) {
        if (session == null || mojoExecution == null) {
            return null;
        }
        try {
            return new PluginParameterExpressionEvaluator(session, mojoExecution).evaluate(expression);
        } catch (ExpressionEvaluationException ex) {
            return null;
        }
    }

    private void setProxyFromSettings() {
        Properties sysProps = System.getProperties();
        boolean httpProxy = false;
        boolean httpsProxy = false;
        for (Proxy proxy : settings.getProxies()) {
            String protocol = proxy.getProtocol();
            if ("http".equals(protocol)) {
                if (!httpProxy) {
                    httpProxy = true;
                } else {
                    continue;
                }
            } else if ("https".equals(protocol)) {
                if (!httpsProxy) {
                    httpsProxy = true;
                } else {
                    continue;
                }
            }
            String hostProp = protocol + PROXY_HOST_PROP_SUFFIX;
            if (!sysProps.containsKey(hostProp)) {
                sysProps.setProperty(hostProp, proxy.getHost());
                sysProps.setProperty(protocol + PROXY_PORT_PROP_SUFFIX, String.valueOf(proxy.getPort()));
                sysProps.setProperty(protocol + NON_PROXY_PROP_SUFFIX, proxy.getNonProxyHosts());
            }
            if (httpProxy && httpsProxy) {
                break;
            }
        }
    }
}
