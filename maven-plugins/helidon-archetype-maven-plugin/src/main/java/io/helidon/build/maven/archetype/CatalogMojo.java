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
package io.helidon.build.maven.archetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import static io.helidon.build.archetype.engine.v1.MustacheHelper.MUSTACHE_EXT;
import static io.helidon.build.archetype.engine.v1.MustacheHelper.renderMustacheTemplate;

/**
 * {@code archetype:catalog} mojo.
 */
@Mojo(name = "catalog", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public class CatalogMojo extends AbstractMojo {

    /**
     * The catalog filename.
     */
    private static final String CATALOG_FILENAME = "archetype-catalog.xml";

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

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
     * The archetype catalog template.
     */
    @Parameter(defaultValue = "${basedir}/catalog.xml.mustache", required = true)
    private File archetypeCatalogTemplate;

    /**
     * The archetype catalog.
     */
    @Parameter(defaultValue = "${basedir}/catalog.xml", required = true)
    private File archetypeCatalog;

    /**
     * The archetype catalog archive.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.zip", required = true)
    private File catalogArchive;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (archetypeCatalog.exists()) {
            getLog().info("Using " + archetypeCatalog);
        } else if (archetypeCatalogTemplate.exists()){
            getLog().info("Rendering " + archetypeCatalogTemplate);
            Path generatedCatalog = outputDirectory.toPath().resolve(CATALOG_FILENAME);
            Map<String, String> props = MojoHelper.templateProperties(Map.of(), true, project);
            try {
                renderMustacheTemplate(archetypeCatalogTemplate, CATALOG_FILENAME + MUSTACHE_EXT,
                        generatedCatalog, props);
            } catch (IOException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
            archetypeCatalog = generatedCatalog.toFile();
        } else {
            throw new MojoFailureException("No catalog.xml or catalog.xml.mustache found");
        }

        getLog().info("Resolving archetypes...");
        ArchetypeCatalog catalog = readCatalog(archetypeCatalog);
        List<File> archetypeFiles = new LinkedList<>();
        for (ArchetypeCatalog.ArchetypeEntry archetypeEntry : catalog.entries()) {
            archetypeFiles.add(resolveArchetype(archetypeEntry));
        }

        getLog().info("Creating archetype catalog archive: " + catalogArchive);
        try {
            Archiver archiver = archiverManager.getArchiver(catalogArchive);
            archiver.addFile(archetypeCatalog, CATALOG_FILENAME);
            for (File archetypeFile : archetypeFiles) {
                archiver.addFile(archetypeFile, archetypeFile.getName());
            }
            archiver.setDestFile(catalogArchive);
            archiver.createArchive();
        } catch (NoSuchArchiverException | IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
        project.getArtifact().setFile(catalogArchive);
    }

    /**
     * Resolve an archetype artifact.
     *
     * @param archetypeEntry archetype entry
     * @return File
     */
    private File resolveArchetype(ArchetypeCatalog.ArchetypeEntry archetypeEntry) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(archetypeEntry.groupId(), archetypeEntry.artifactId(), null,
                "jar", archetypeEntry.version()));
        request.setRepositories(remoteRepos);
        ArtifactResult result = null;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException ex) {
            throw new RuntimeException(ex);
        }
        return result.getArtifact().getFile();
    }

    /**
     * Read the catalog to raise any error.
     *
     * @param catalogFile catalog file
     * @throws MojoExecutionException if an error occurred while reading the catalog
     */
    private static ArchetypeCatalog readCatalog(File catalogFile) throws MojoExecutionException {
        try {
            return ArchetypeCatalog.read(new FileInputStream(catalogFile));
        } catch (FileNotFoundException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
