/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * {@code stager:stage} mojo.
 */
@Mojo(name = "stage", defaultPhase = LifecyclePhase.PACKAGE)
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
    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File baseDirectory;

    /**
     * The directories to stage.
     */
    @Parameter(required = true)
    private PlexusConfiguration directories;

    /**
     * Dry-run, if {@code true} the tasks are no-ops.
     */
    @Parameter(defaultValue = "false", property = "stager.dryRun")
    private boolean dryRun;

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

    @Parameter
    private ExecutorConfig executor = new ExecutorConfig();

    @Override
    public void execute() {
        if (directories == null) {
            return;
        }
        ExecutorService executorService = executor.select();
        StagingContext context = new StagingContextImpl(
                baseDirectory,
                outputDirectory,
                getLog(),
                repoSystem,
                repoSession,
                remoteRepos,
                archiverManager,
                executorService,
                this::resolveProperty);
        Path dir = outputDirectory.toPath();

        StagingElementFactory factory;
        if (dryRun) {
            getLog().info("Dry run mode");
            factory = new DryRunStagingElementFactory(this);
        } else {
            factory = new StagingElementFactory();
        }

        setProxyFromSettings();
        StagingTasks tasks = StagingAction.fromConfiguration(directories, factory);
        try {
            tasks.execute(context, dir, Map.of())
                 .toCompletableFuture()
                 .get();
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(ex);
        }
    }

    private String resolveProperty(String name) {
        if (name == null || name.isEmpty()) {
            return null;
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
            case StagingContext.MAX_RETRIES:
                if (maxRetries >= 0) {
                    return String.valueOf(maxRetries);
                }
                break;
            default:
                Object value = session.getCurrentProject().getProperties().get(name);
                if (value == null) {
                    value = session.getUserProperties().get(name);
                }
                if (value == null) {
                    value = session.getSystemProperties().getProperty(name);
                }
                if (value instanceof String) {
                    return (String) value;
                }
                return null;
        }
        return null;
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
