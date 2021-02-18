/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.cache.CacheArchiveIndex.FileEntry;
import io.helidon.build.util.SourcePath;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoArchiveResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * A component to load / save cache from/to an archive.
 */
@Component(role = CacheArchiveManager.class, hint = "default")
public class CacheArchiveManager {

    @Requirement
    private ArchiverManager archiverManager;

    @Requirement
    private Logger logger;

    /**
     * Save the build cache to the given archive.
     *
     * @param session     maven session
     * @param archiveFile cache archive file
     */
    @SuppressWarnings("deprecation")
    public void save(MavenSession session, Path archiveFile) {
        logger.info("Creating build cache archive...");
        Archiver archiver;
        try {
            archiver = archiverManager.getArchiver("tar");
        } catch (NoSuchArchiverException ex) {
            throw new IllegalStateException(ex);
        }

        try {
            ArtifactRepository localRepository = session.getLocalRepository();
            Path localRepoDir = Path.of(localRepository.getBasedir());
            Path projectRootDir = Path.of(session.getExecutionRootDirectory());
            List<CacheArchiveIndex.ProjectEntry> projectEntries = new LinkedList<>();
            List<FileEntry> repoFiles = new LinkedList<>();
            CacheArchiveIndex index = new CacheArchiveIndex(projectEntries, repoFiles);

            if (logger.isDebugEnabled()) {
                logger.debug("Collecting files...");
            }

            AtomicInteger fileIndex = new AtomicInteger();
            for (MavenProject project : session.getProjects()) {
                CacheConfig cacheConfig = CacheConfig.of(project, session);
                if (cacheConfig.skip()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                                "[%s:%s] - cache.skip is true, not saving state",
                                project.getGroupId(),
                                project.getArtifactId()));
                    }
                    continue;
                }
                Path buildDir = project.getModel().getProjectDirectory().toPath()
                        .resolve(project.getModel().getBuild().getDirectory());
                List<String> buildFilesExcludes = cacheConfig.buildFilesExcludes();
                List<FileEntry> buildFiles = new LinkedList<>();
                if (Files.exists(buildDir)) {
                    Files.walk(buildDir)
                            .filter(f -> !Files.isDirectory(f)
                                    && !f.equals(archiveFile)
                                    && new SourcePath(buildDir, f).matches(null, buildFilesExcludes))
                            .forEach(f -> {
                                FileEntry buildFile = new FileEntry(buildDir.relativize(f).toString(),
                                        fileIndex.getAndIncrement());
                                buildFiles.add(buildFile);
                                archiver.addFile(f.toFile(), buildFile.index());
                            });
                }
                projectEntries.add(new CacheArchiveIndex.ProjectEntry(project.getGroupId(), project.getArtifactId(),
                        projectRootDir.relativize(buildDir).toString(), buildFiles));

                for (Artifact artifact : projectArtifacts(project)) {
                    for (ArtifactMetadata metadata : artifact.getMetadataList()) {
                        if (!(metadata instanceof ProjectArtifactMetadata)) {
                            continue;
                        }
                        String repoPath = localRepository.pathOfLocalRepositoryMetadata(metadata, null);
                        FileEntry repoFile = new FileEntry(repoPath, fileIndex.getAndIncrement());
                        archiver.addFile(localRepoDir.resolve(repoPath).toFile(), repoFile.index());
                        repoFiles.add(repoFile);
                    }
                    String repoPath = localRepository.pathOf(artifact);
                    Path localRepoPath = localRepoDir.resolve(repoPath);
                    if (Files.exists(localRepoPath)) {
                        FileEntry repoFile = new FileEntry(repoPath, fileIndex.getAndIncrement());
                        archiver.addFile(localRepoDir.resolve(repoPath).toFile(), repoFile.index());
                        repoFiles.add(repoFile);
                    }
                }
            }
            int numBuildFiles = projectEntries
                    .stream()
                    .map(p -> p.buildFiles().size()).mapToInt(Integer::intValue).sum();
            logger.info(String.format("Found %d build file(s)", numBuildFiles));
            logger.info(String.format("Found %d repository file(s)", repoFiles.size()));

            if (logger.isDebugEnabled()) {
                logger.debug("Creating cache index...");
            }
            Path indexPath = Files.createTempFile("cacheIndex", ".xml");
            if (logger.isDebugEnabled()) {
                logger.debug("Cache index file: " + indexPath);
            }
            index.save(indexPath);
            File indexFile = indexPath.toFile();
            indexFile.deleteOnExit();
            archiver.addFile(indexPath.toFile(), "index.xml");
            archiver.setDestFile(archiveFile.toFile());
            archiver.createArchive();
        } catch (IOException ex) {
            logger.error("An error occurred while creating build cache archive", ex);
        }
    }

    private CacheArchiveIndex load(Path cacheArchive) throws IOException, XmlPullParserException {
        PlexusIoResourceCollection resources;
        try {
            resources = archiverManager.getResourceCollection("tar");
        } catch (NoSuchArchiverException ex) {
            throw new IllegalStateException(ex);
        }
        if (resources instanceof AbstractPlexusIoArchiveResourceCollection) {
            ((AbstractPlexusIoArchiveResourceCollection) resources).setFile(cacheArchive.toFile());
        }
        for (PlexusIoResource resource : resources) {
            if (resource.getName().equals("index.xml")) {
                return CacheArchiveIndex.load(resource.getContents());
            }
        }
        return null;
    }

    /**
     * Load the build cache from a cache archive.
     *
     * @param session     maven session
     * @param archiveFile cache archive file
     */
    public void loadCache(MavenSession session, Path archiveFile) {
        logger.info("Loading build cache from file...");
        CacheArchiveIndex index;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading index");
            }
            index = load(archiveFile);
            if (index == null) {
                logger.error("Index not found");
                return;
            }
        } catch (IOException | XmlPullParserException ex) {
            logger.error("Error while loading the index", ex);
            return;
        }

        UnArchiver unArchiver;
        try {
            unArchiver = archiverManager.getUnArchiver("tar");
            unArchiver.setSourceFile(archiveFile.toFile());
        } catch (NoSuchArchiverException ex) {
            throw new IllegalStateException(ex);
        }

        final Map<String, String> unArchiverEntries = new HashMap<>();
        FileSelector[] fileSelectors = {
                fileInfo -> unArchiverEntries.containsKey(fileInfo.getName())
        };
        unArchiver.setFileSelectors(fileSelectors);
        FileMapper[] fileMappers = {unArchiverEntries::get};
        unArchiver.setFileMappers(fileMappers);
        unArchiver.setOverwrite(false);

        if (logger.isDebugEnabled()) {
            logger.debug("Processing repository files...");
        }
        Path localRepoDir = Path.of(session.getLocalRepository().getBasedir());
        unArchiver.setDestDirectory(localRepoDir.toFile());
        unArchiver.setOverwrite(true);
        for (FileEntry repoFile : index.repoFiles()) {
            unArchiverEntries.put(repoFile.index(), localRepoDir.resolve(repoFile.path()).toString());
        }
        unArchiver.extract();
        unArchiverEntries.clear();

        for (MavenProject project : session.getProjects()) {
            CacheArchiveIndex.ProjectEntry projectEntry = index.findProject(project.getGroupId(),
                    project.getArtifactId());
            if (projectEntry == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("[%s:%s] - not found in index",
                            project.getGroupId(),
                            project.getArtifactId()));
                }
                continue;
            }

            CacheConfig cacheConfig = CacheConfig.of(project, session);
            if (cacheConfig.skip()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                            "[%s:%s] - cache.skip is true, not loading state",
                            project.getGroupId(),
                            project.getArtifactId()));
                }
                continue;
            }

            List<String> buildFilesExcludes = cacheConfig.buildFilesExcludes();
            Path buildDir = project.getModel().getProjectDirectory().toPath()
                    .resolve(project.getModel().getBuild().getDirectory());
            for (FileEntry buildFile : projectEntry.buildFiles()) {
                if (new SourcePath(buildFile.path()).matches(null, buildFilesExcludes)) {
                    unArchiverEntries.put(buildFile.index(), buildDir.resolve(buildFile.path()).toString());
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Processing build files...");
        }
        unArchiver.setDestDirectory(new File(session.getExecutionRootDirectory()));
        unArchiver.setOverwrite(false);
        unArchiver.extract();
    }

    private static List<Artifact> projectArtifacts(MavenProject project) {
        LinkedList<Artifact> artifacts = new LinkedList<>();
        Artifact artifact = project.getArtifact();
        if (artifact != null) {
            artifacts.add(artifact);
        }
        artifacts.addAll(project.getAttachedArtifacts());
        return artifacts;
    }
}
