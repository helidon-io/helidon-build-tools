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
package io.helidon.build.stager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * {@code stager:stage} mojo.
 */
@Mojo(name = "stage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
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
    @Parameter(defaultValue = "-1", property = DownloadTask.READ_TIMEOUT_PROP)
    private int readTimeout;

    /**
     * {@code connectTimeout} configuration (in ms) for the download task.
     */
    @Parameter(defaultValue = "-1", property = DownloadTask.CONNECT_TIMEOUT_PROP)
    private int connectTimeout;

    /**
     * {@code maxRetries} configuration for the download task.
     */
    @Parameter(defaultValue = "-1", property = DownloadTask.MAX_RETRIES)
    private int maxRetries;

    @Override
    public void execute() throws MojoExecutionException {
        if (directories == null) {
            return;
        }
        StagingContext context = new StagingContextImpl(
                baseDirectory,
                outputDirectory,
                getLog(),
                repoSystem,
                repoSession,
                remoteRepos,
                archiverManager,
                this::resolveProperty);
        Path dir = outputDirectory.toPath();

        StagingElementFactory factory;
        if (dryRun) {
            getLog().info("Dry run mode");
            factory = new DryRunStagingElementFactory();
        } else {
            factory = new StagingElementFactory();
        }

        setProxyFromSettings();
        try {
            for (StagingAction action : StagingAction.fromConfiguration(directories, factory)) {
                action.execute(context, dir);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private String resolveProperty(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        switch (name) {
            case DownloadTask.READ_TIMEOUT_PROP:
                if (readTimeout >= 0) {
                    return String.valueOf(readTimeout);
                }
                break;
            case DownloadTask.CONNECT_TIMEOUT_PROP:
                if (connectTimeout >= 0) {
                    return String.valueOf(connectTimeout);
                }
                break;
            case DownloadTask.MAX_RETRIES:
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
                if (value != null && (value instanceof String)) {
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

    /**
     * Custom staging element factory that wraps the staging tasks to overrides the execution to do a no-op.
     */
    private final class DryRunStagingElementFactory extends StagingElementFactory {

        @Override
        StagingAction createAction(String name,
                                   Map<String, String> attrs,
                                   Map<String, List<StagingElement>> children,
                                   String text) {

            StagingAction action = super.createAction(name, attrs, children, text);
            if (action instanceof StagingTask) {
                return new DryRunTask((StagingTask) action);
            }
            return new DryRunAction(action);
        }
    }

    /**
     * Staging action that prints information about the action being executed.
     */
    private final class DryRunAction implements StagingAction {

        private final StagingAction delegate;

        DryRunAction(StagingAction delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
            getLog().info(describe(dir, variables));
            delegate.execute(context, dir, variables);
        }

        @Override
        public String elementName() {
            return delegate.elementName();
        }

        @Override
        public String describe(Path dir, Map<String, String> variables) {
            return delegate.describe(dir, variables);
        }
    }

    /**
     * Staging task that prints information about the task being executed and makes the actual task execution a no-op.
     */
    private final class DryRunTask extends StagingTask {

        private final StagingTask delegate;

        DryRunTask(StagingTask delegate) {
            super(delegate.iterators(), delegate.target());
            this.delegate = delegate;
        }

        @Override
        public String elementName() {
            return delegate.elementName();
        }

        @Override
        protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) {
            getLog().info(describe(dir, variables));
        }

        @Override
        public String describe(Path dir, Map<String, String> variables) {
            return delegate.describe(dir, variables);
        }
    }

    /**
     * Staging context implementation.
     */
    private static final class StagingContextImpl implements StagingContext {

        private final Log log;
        private final File baseDir;
        private final File outputDir;
        private final RepositorySystem repoSystem;
        private final RepositorySystemSession repoSession;
        private final List<RemoteRepository> remoteRepos;
        private final ArchiverManager archiverManager;
        private final Function<String, String> propertyResolver;

        StagingContextImpl(File baseDir,
                           File outputDir,
                           Log log,
                           RepositorySystem repoSystem,
                           RepositorySystemSession repoSession,
                           List<RemoteRepository> remoteRepos,
                           ArchiverManager archiverManager,
                           Function<String, String> propertyResolver) {

            this.baseDir = baseDir;
            this.outputDir = outputDir;
            this.log = log;
            this.repoSystem = repoSystem;
            this.repoSession = repoSession;
            this.remoteRepos = remoteRepos;
            this.propertyResolver = propertyResolver;
            this.archiverManager = Objects.requireNonNull(archiverManager, "archiverManager is null");
        }

        @Override
        public String property(String name) {
            return propertyResolver.apply(name);
        }

        @Override
        public void unpack(Path archive, Path target, String excludes, String includes) {
            File archiveFile = archive.toFile();
            UnArchiver unArchiver = null;
            try {
                unArchiver = archiverManager.getUnArchiver(archiveFile);
            } catch (NoSuchArchiverException ex) {
                throw new IllegalStateException(ex);
            }
            unArchiver.setSourceFile(archiveFile);
            unArchiver.setDestDirectory(target.toFile());
            if (StringUtils.isNotEmpty(excludes) || StringUtils.isNotEmpty(includes)) {
                IncludeExcludeFileSelector[] selectors = new IncludeExcludeFileSelector[]{
                        new IncludeExcludeFileSelector()
                };
                if (StringUtils.isNotEmpty(excludes)) {
                    selectors[0].setExcludes(excludes.split(","));
                }
                if (StringUtils.isNotEmpty(includes)) {
                    selectors[0].setIncludes(includes.split(","));
                }
                unArchiver.setFileSelectors(selectors);
            }
            unArchiver.extract();
        }

        @Override
        public void archive(Path directory, Path target, String excludes, String includes) {
            File archiveFile = target.toFile();
            Archiver archiver;
            try {
                archiver = archiverManager.getArchiver(archiveFile);
            } catch (NoSuchArchiverException ex) {
                throw new IllegalStateException(ex);
            }
            DefaultFileSet fileSet = new DefaultFileSet(directory.toFile());
            if (StringUtils.isNotEmpty(excludes) || StringUtils.isNotEmpty(includes)) {
                if (StringUtils.isNotEmpty(excludes)) {
                    fileSet.setExcludes(excludes.split(","));
                }
                if (StringUtils.isNotEmpty(includes)) {
                    fileSet.setIncludes(includes.split(","));
                }
            }
            archiver.addFileSet(fileSet);
            archiver.setDestFile(archiveFile);
            try {
                archiver.createArchive();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Path resolve(String path) {
            return baseDir.toPath().resolve(path);
        }

        @Override
        public Path resolve(ArtifactGAV gav) {
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(new DefaultArtifact(gav.groupId(), gav.artifactId(), gav.classifier(),
                    gav.type(), gav.version()));
            request.setRepositories(remoteRepos);
            ArtifactResult result = null;
            try {
                result = repoSystem.resolveArtifact(repoSession, request);
            } catch (ArtifactResolutionException ex) {
                throw new RuntimeException(ex);
            }
            return result.getArtifact().getFile().toPath();
        }

        @Override
        public Path createTempDirectory(String prefix) throws IOException {
            return Files.createTempDirectory(outputDir.toPath(), prefix);
        }

        @Override
        public void logInfo(String msg, Object... args) {
            log.info(String.format(msg, args));
        }

        @Override
        public void logWarning(String msg, Object... args) {
            log.warn(String.format(msg, args));
        }

        @Override
        public void logError(String msg, Object... args) {
            log.error(String.format(msg, args));
        }

        @Override
        public void logDebug(String msg, Object... args) {
            log.debug(String.format(msg, args));
        }
    }
}
