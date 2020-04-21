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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.helidon.build.archetype.engine.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.ArchetypeEngine;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * {@code archetype:jar} mojo.
 */
@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public class JarMojo extends AbstractMojo {

    /**
     * Plexus build context used to get the scanner for scanning resources.
     */
    @Component
    private BuildContext buildContext;

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
     * Name of the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.finalName}", alias = "jarName", required = true)
    private String finalName;

    /**
     * The {@link MavenSession}.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The Jar archiver.
     */
    @Component
    private Map<String, Archiver> archivers;

    /**
     * The archive configuration to use.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Timestamp for reproducible output archive entries.
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path archetypeDir = outputDirectory.toPath().resolve("archetype");
        Path baseDir = project.getBasedir().toPath();
        Path archetypeDescriptor = archetypeDir.resolve(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME);
        Path archetypeResourcesList = archetypeDir.resolve(ArchetypeEngine.RESOURCES_LIST);
        Path mavenArchetypeDescriptor = archetypeDir.resolve("META-INF/maven/archetype-metadata.xml");

        Map<String, List<String>> resources = getResources();
        Path archetypeDescriptorSource = resources.entrySet().stream()
                .filter(e -> e.getValue().contains(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME))
                .map(e -> baseDir.resolve(e.getKey()).resolve(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME))
                .findAny()
                .orElseThrow(() -> new MojoFailureException(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME + " not found"));

        // create target/archetype/META-INF
        // create target/archetype/META-INF/maven
        // copy src/main/resources/META-INF/helidon-archetype.xml to target/archetype/META-INF/helidon-archetype.xml
        try {
            Files.createDirectories(archetypeDescriptor.getParent());
            Files.createDirectories(mavenArchetypeDescriptor.getParent());
            Files.copy(archetypeDescriptorSource, archetypeDescriptor, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        // create target/archetype/META-INF/maven/archetype-metadata.xml
        try (BufferedWriter writer = Files.newBufferedWriter(mavenArchetypeDescriptor)) {
            ArchetypeDescriptor desc = ArchetypeDescriptor.read(Files.newInputStream(archetypeDescriptorSource));
            StringWriter sw = new StringWriter();
            DescriptorConverter.convert(desc, sw);
            writer.append(sw.toString());
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        // create target/archetype/META-INF/helidon-archetype-resources.txt
        // copy archetype resources to target/archetype/
        try (BufferedWriter writer = Files.newBufferedWriter(archetypeResourcesList)) {
            PrintWriter printer = new PrintWriter(writer);
            for (Entry<String, List<String>> resourcesEntry : resources.entrySet()) {
                getLog().debug("processing resources scanned from: " + resourcesEntry.getKey());
                for (String resource : resourcesEntry.getValue()) {
                    if (resource.startsWith("META-INF/")) {
                        continue;
                    }
                    getLog().debug("adding resource to archetype manifest: " + resource);
                    printer.println(resource);
                    Path resourceTarget = archetypeDir.resolve(resource);
                    getLog().debug("adding resource to archetype directory: " + resource);
                    Files.createDirectories(resourceTarget);
                    Files.copy(baseDir.resolve(resourcesEntry.getKey()).resolve(resource), resourceTarget,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        File jarFile = new File(outputDirectory, finalName + ".jar");
        getLog().info("Building archetype jar: " + jarFile);

        MavenArchiver archiver = new MavenArchiver();
        archiver.setCreatedBy("Helidon Archetype Plugin", "io.helidon.build-tools.archetype", "helidon-archetype-maven-plugin");
        archiver.setOutputFile(jarFile);
        archiver.setArchiver((JarArchiver) archivers.get("jar"));
        archiver.configureReproducible(outputTimestamp);

        try {
            archiver.getArchiver().addDirectory(archetypeDir.toFile());
            archiver.createArchive(session, project, archive);
        } catch (IOException | DependencyResolutionRequiredException | ArchiverException | ManifestException e) {
            throw new MojoExecutionException("Error assembling archetype jar " + jarFile, e);
        }
        project.getArtifact().setFile(jarFile);
    }

    /**
     * Scan for project resources and produce a comma separated list of include resources.
     *
     * @return list of resources
     */
    private Map<String, List<String>> getResources() {
        getLog().debug("Scanning project resources");
        Map<String, List<String>> allResources = new HashMap<>();
        for (Resource resource : project.getResources()) {
            List<String> resources = new ArrayList<>();
            allResources.put(resource.getDirectory(), resources);
            File resourcesDir = new File(resource.getDirectory());
            Scanner scanner = buildContext.newScanner(resourcesDir);
            String[] includes = null;
            if (resource.getIncludes() != null
                    && !resource.getIncludes().isEmpty()) {
                includes = (String[]) resource.getIncludes()
                        .toArray(new String[resource.getIncludes().size()]);
            }
            scanner.setIncludes(includes);
            String[] excludes = null;
            if (resource.getExcludes() != null
                    && !resource.getExcludes().isEmpty()) {
                excludes = (String[]) resource.getExcludes()
                        .toArray(new String[resource.getExcludes().size()]);
            }
            scanner.setExcludes(excludes);
            scanner.scan();
            for (String included : scanner.getIncludedFiles()) {
                getLog().debug("Found resource: " + included);
                resources.add(included);
            }
        }
        return allResources;
    }
}
