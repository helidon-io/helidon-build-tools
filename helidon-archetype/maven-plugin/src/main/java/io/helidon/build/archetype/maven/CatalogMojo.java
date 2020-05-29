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
package io.helidon.build.archetype.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.archetype.engine.ArchetypeCatalog;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static io.helidon.build.archetype.maven.MojoHelper.MUSTACHE_EXT;
import static io.helidon.build.archetype.maven.MojoHelper.renderMustacheTemplate;
import static io.helidon.build.archetype.maven.MojoHelper.templateProperties;

/**
 * {@code archetype:catalog} mojo.
 */
@Mojo(name = "catalog", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public class CatalogMojo extends AbstractMojo {

    /**
     * The catalog filename.
     */
    private static final String CATALOG_FILENAME = "catalog.xml";

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The project build output directory. (e.g. {@code target/})
     */
    @Parameter(defaultValue = "${project.build.directory}",
            readonly = true, required = true)
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (archetypeCatalog.exists()) {
            getLog().info("Using " + archetypeCatalog);
        } else if (archetypeCatalogTemplate.exists()){
            getLog().info("Rendering " + archetypeCatalogTemplate);
            Path generatedCatalog = outputDirectory.toPath().resolve(CATALOG_FILENAME);
            Map<String, String> props = templateProperties(Map.of(), true, project);
            renderMustacheTemplate(archetypeCatalogTemplate, CATALOG_FILENAME + MUSTACHE_EXT,
                    generatedCatalog, props);
            archetypeCatalog = generatedCatalog.toFile();
        } else {
            throw new MojoFailureException("No catalog.xml or catalog.xml.mustache found");
        }
        verifyCatalog(archetypeCatalog);
        project.getArtifact().setFile(archetypeCatalog);
    }

    /**
     * Read the catalog to raise any error.
     *
     * @param catalogFile catalog file
     * @throws MojoExecutionException if an error occurred while reading the catalog
     */
    private static void verifyCatalog(File catalogFile) throws MojoExecutionException {
        try {
            ArchetypeCatalog.read(new FileInputStream(catalogFile));
        } catch (FileNotFoundException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
