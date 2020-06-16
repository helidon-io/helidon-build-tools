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
package io.helidon.build.stager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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
 * {@code stager:stage} mojo.
 */
@Mojo(name = "stage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public class StagerMojo extends AbstractMojo {

    /**
     * The archivers.
     */
    @Component
    private Map<String, Archiver> archivers;

    /**
     * The un-archivers.
     */
    @Component
    private Map<String, UnArchiver> unArchivers;

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
     * The directories to stage.
     */
    @Parameter(required = true)
    private List<StagedDirectory> directories;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (directories == null) {
            return;
        }
        StagingContext context = new StagingContextImpl(outputDirectory, getLog(), repoSystem, repoSession, remoteRepos,
                archiverManager);
        Path dir = outputDirectory.toPath();
        try {
            for (StagedDirectory stagedDir : directories) {
                stagedDir.execute(context, dir);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * Staging context implementation.
     */
    private static final class StagingContextImpl implements StagingContext {

        private final Log log;
        private final File outputDir;
        private final RepositorySystem repoSystem;
        private final RepositorySystemSession repoSession;
        private final List<RemoteRepository> remoteRepos;
        private final ArchiverManager archiverManager;

        StagingContextImpl(File outputDir,
                           Log log,
                           RepositorySystem repoSystem,
                           RepositorySystemSession repoSession,
                           List<RemoteRepository> remoteRepos,
                           ArchiverManager archiverManager) {

            this.outputDir = outputDir;
            this.log = log;
            this.repoSystem = repoSystem;
            this.repoSession = repoSession;
            this.remoteRepos = remoteRepos;
            this.archiverManager = Objects.requireNonNull(archiverManager, "archiverManager is null");
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
            Archiver archiver = null;
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
