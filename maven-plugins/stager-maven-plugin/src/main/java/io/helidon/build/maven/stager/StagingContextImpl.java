/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Staging context implementation.
 */
final class StagingContextImpl implements StagingContext {

    private final Log log;
    private final File baseDir;
    private final File outputDir;
    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> remoteRepos;
    private final ArchiverManager archiverManager;
    private final Function<String, String> propertyResolver;
    private final Executor executor;
    private final int connectTimeout;
    private final int readTimeout;
    private final int taskTimeout;
    private final int maxRetries;

    StagingContextImpl(File baseDir,
                       File outputDir,
                       Log log,
                       RepositorySystem repoSystem,
                       RepositorySystemSession repoSession,
                       List<RemoteRepository> remoteRepos,
                       ArchiverManager archiverManager,
                       Executor executor,
                       Function<String, String> propertyResolver) {

        this.baseDir = baseDir;
        this.outputDir = outputDir;
        this.log = log;
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
        this.propertyResolver = propertyResolver;
        this.archiverManager = Objects.requireNonNull(archiverManager, "archiverManager is null");
        this.executor = executor;
        this.readTimeout = Optional.ofNullable(propertyResolver.apply(StagingContext.READ_TIMEOUT_PROP))
                                   .map(Integer::parseInt)
                                   .orElse(-1);
        this.connectTimeout = Optional.ofNullable(propertyResolver.apply(StagingContext.CONNECT_TIMEOUT_PROP))
                                      .map(Integer::parseInt)
                                      .orElse(-1);
        this.taskTimeout = Optional.ofNullable(propertyResolver.apply(StagingContext.TASK_TIMEOUT_PROP))
                                   .map(Integer::parseInt)
                                   .orElse(-1);
        this.maxRetries = Optional.ofNullable(propertyResolver.apply(StagingContext.MAX_RETRIES))
                                  .map(Integer::parseInt)
                                  .orElse(-1);
    }

    @Override
    public String property(String name) {
        return propertyResolver.apply(name);
    }

    @Override
    public void unpack(Path archive, Path target, String excludes, String includes) {
        File archiveFile = archive.toFile();
        UnArchiver unArchiver;
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
        logInfo("Resolving %s", gav);
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(gav.groupId(), gav.artifactId(), gav.classifier(),
                gav.type(), gav.version()));
        request.setRepositories(remoteRepos);
        ArtifactResult result;
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
    public void logError(Throwable ex) {
        log.error(ex);
    }

    @Override
    public void logDebug(String msg, Object... args) {
        log.debug(String.format(msg, args));
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public int readTimeout() {
        return readTimeout;
    }

    @Override
    public int connectTimeout() {
        return connectTimeout;
    }

    @Override
    public int taskTimeout() {
        return taskTimeout;
    }

    @Override
    public int maxRetries() {
        return maxRetries;
    }
}
