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
package io.helidon.build.archetype.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import io.helidon.build.archetype.engine.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Property;
import io.helidon.build.archetype.engine.ArchetypeEngine;
import io.helidon.build.util.MustacheHelper.RawString;
import io.helidon.build.util.SourcePath;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import static io.helidon.build.util.MustacheHelper.MUSTACHE_EXT;
import static io.helidon.build.util.MustacheHelper.renderMustacheTemplate;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.DOTALL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * {@code archetype:jar} mojo.
 */
@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JarMojo extends AbstractMojo {

    private static final String ENGINE_GROUP_ID = MojoHelper.PLUGIN_GROUP_ID;
    private static final String ENGINE_ARTIFACT_ID = "helidon-archetype-engine";
    private static final String ENGINE_VERSION = MojoHelper.PLUGIN_VERSION;
    private static final String POST_SCRIPT_NAME = "archetype-post-generate.groovy";
    private static final String POST_SCRIPT_PKG = "io/helidon/build/archetype/maven/postgenerate";
    private static final Pattern COPYRIGHT_HEADER = Pattern.compile("^(\\s?\\R)?\\/\\*.*\\*\\/(\\s?\\R)?", DOTALL);

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
     * The archivers.
     */
    @Component
    private Map<String, Archiver> archivers;

    /**
     * The pom reader.
     */
    @Component
    private ModelReader modelReader;

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

    /**
     * Properties to use for pre-processing.
     */
    @Parameter
    private Map<String, String> properties = Collections.emptyMap();

    /**
     * Include project properties for pre-processing.
     */
    @Parameter(defaultValue = "true")
    private boolean includeProjectProperties;

    /**
     * Indicate if the generated JAR should be compatible with the {@code maven-archetype-plugin}.
     */
    @Parameter(defaultValue = "true")
    private boolean mavenArchetypeCompatible;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path archetypeDir = outputDirectory.toPath().resolve("archetype");
        Path baseDir = project.getBasedir().toPath();
        Path archetypeDescriptor = archetypeDir.resolve(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME);
        Path archetypeResourcesList = archetypeDir.resolve(ArchetypeEngine.RESOURCES_LIST);

        Map<String, List<String>> resources = scanResources();
        processDescriptor(resources, baseDir, archetypeDescriptor);
        if (mavenArchetypeCompatible) {
            processMavenCompat(archetypeDir, archetypeDescriptor);
        }
        processArchetypeResources(resources, archetypeDir, baseDir, archetypeResourcesList);

        File jarFile = generateArchetypeJar(archetypeDir);
        project.getArtifact().setFile(jarFile);
    }

    private void processDescriptor(Map<String, List<String>> resources, Path baseDir, Path archetypeDescriptor)
            throws MojoFailureException, MojoExecutionException {

        try {
            getLog().info("Processing archetype descriptor");

            // create target/archetype/META-INF
            Files.createDirectories(archetypeDescriptor.getParent());

            // find a descriptor template
            Path archetypeDescriptorTemplate = findResource(resources, baseDir,
                    ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME + MUSTACHE_EXT);
            if (archetypeDescriptorTemplate != null) {
                preProcessDescriptor(archetypeDescriptorTemplate, archetypeDescriptor);
            } else {
                // or else copy a descriptor
                Path archetypeDescriptorSource = findResource(resources, baseDir,
                        ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME);
                if (archetypeDescriptorSource == null) {
                    throw new MojoFailureException(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME + " not found");
                }
                getLog().info("Copying " + archetypeDescriptorSource);
                Files.copy(archetypeDescriptorSource, archetypeDescriptor, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private static InputStream resolveResource(String path) {
        InputStream is = JarMojo.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Unable to resolve resource: " + path);
        }
        return is;
    }

    private static List<String> listResources(String path) throws IOException {
        URL url = JarMojo.class.getClassLoader().getResource(path);
        if (url == null) {
            return Collections.emptyList();
        }
        if (url.getProtocol().equals("file")) {
            try {
                String[] result = new File(url.toURI()).list();
                if (result == null) {
                    return Collections.emptyList();
                }
                return Arrays.asList(result);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
        if (url.getProtocol().equals("jar")) {
            String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, UTF_8));
            Enumeration<JarEntry> entries = jar.entries();
            List<String> result = new ArrayList<>();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) {
                    result.add(name);
                }
            }
            return result;
        }
        throw new UnsupportedOperationException("Cannot list resources for URL " + url);
    }

    private void renderPostScript(Path archetypeDir, ArchetypeDescriptor desc) throws IOException {
        // mustache scope
        Map<String, Object> scope = new HashMap<>();

        // base64 encoded byte code keyed by fully qualified class names
        scope.put("classes", listResources(POST_SCRIPT_PKG)
                .stream()
                .filter(path -> path.endsWith(".class"))
                .collect(toMap(path -> path.replace('/', '.')
                                           .replace(".class", ""),
                        path -> {
                            try {
                                byte[] byteCode = resolveResource(path).readAllBytes();
                                String encodedByteCode = new String(Base64.getEncoder().encode(byteCode), UTF_8);
                                // break line after 140 characters
                                encodedByteCode = encodedByteCode.replaceAll("(.{100})", "$1\n");
                                return new RawString(encodedByteCode);
                            } catch (IOException ex) {
                                throw new UncheckedIOException(ex);
                            }
                        })).entrySet());

        // name of the properties to pass to the Helidon archetype engine
        scope.put("propNames", desc.properties().stream()
                                   .filter(Property::isExported)
                                   .map(Property::id)
                                   .collect(toList()));

        // Helidon archetype engine GAV
        scope.put("engineGAV", ENGINE_GROUP_ID + ":" + ENGINE_ARTIFACT_ID + ":" + ENGINE_VERSION);

        // The non mustache post generate script that contains the postGenerate function
        String postGenerateScript = new String(resolveResource(POST_SCRIPT_NAME).readAllBytes(), UTF_8);
        postGenerateScript = COPYRIGHT_HEADER.matcher(postGenerateScript).replaceAll("");

        scope.put("postGenerateScript", new RawString(postGenerateScript));

        getLog().info("Rendering " + POST_SCRIPT_NAME);

        renderMustacheTemplate(
                resolveResource(POST_SCRIPT_NAME + MUSTACHE_EXT), POST_SCRIPT_NAME,
                archetypeDir.resolve("META-INF/" + POST_SCRIPT_NAME),
                scope);
    }

    private void processMavenCompat(Path archetypeDir, Path archetypeDescriptor) throws MojoExecutionException {
        try {
            getLog().info("Processing maven-archetype-plugin compatibility");

            Path mavenArchetypeDescriptor = archetypeDir.resolve("META-INF/maven/archetype-metadata.xml");

            // create target/archetype/META-INF/maven
            Files.createDirectories(mavenArchetypeDescriptor.getParent());

            ArchetypeDescriptor desc = ArchetypeDescriptor.read(Files.newInputStream(archetypeDescriptor));

            // create target/archetype/META-INF/maven/archetype-metadata.xml
            try (BufferedWriter writer = Files.newBufferedWriter(mavenArchetypeDescriptor)) {
                StringWriter sw = new StringWriter();
                DescriptorConverter.convert(desc, sw);
                writer.append(sw.toString());
            }

            Path mavenArchetypePom = archetypeDir.resolve("archetype-resources/pom.xml");

            // create target/archetype/archetype-resources
            Files.createDirectories(mavenArchetypePom.getParent());

            // create an empty file target/archetype/archetype-resources/pom.xml
            if (!Files.exists(mavenArchetypePom)) {
                Files.createFile(mavenArchetypePom);
            }

            renderPostScript(archetypeDir, desc);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private void processArchetypeResources(Map<String, List<String>> resources,
                                           Path archetypeDir,
                                           Path baseDir,
                                           Path archetypeResourcesList)
            throws MojoExecutionException {

        getLog().info("Processing archetype resources");

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
    }

    private File generateArchetypeJar(Path archetypeDir) throws MojoExecutionException {
        File jarFile = new File(outputDirectory, finalName + ".jar");

        MavenArchiver archiver = new MavenArchiver();
        archiver.setCreatedBy("Helidon Archetype Plugin", "io.helidon.build-tools.archetype",
                "helidon-archetype-maven-plugin");
        archiver.setOutputFile(jarFile);
        archiver.setArchiver((JarArchiver) archivers.get("jar"));
        archiver.configureReproducible(outputTimestamp);

        try {
            archiver.getArchiver().addDirectory(archetypeDir.toFile());
            archiver.createArchive(session, project, archive);
        } catch (IOException | DependencyResolutionRequiredException | ArchiverException | ManifestException e) {
            throw new MojoExecutionException("Error assembling archetype jar " + jarFile, e);
        }
        return jarFile;
    }

    private void preProcessDescriptor(Path template, Path archetypeDescriptor) throws MojoExecutionException {
        getLog().info("Rendering " + template);
        Map<String, String> props = MojoHelper.templateProperties(properties, includeProjectProperties, project);
        try {
            renderMustacheTemplate(Files.newInputStream(template),
                    ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME + MUSTACHE_EXT, archetypeDescriptor, props);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private Path findResource(Map<String, List<String>> resources, Path baseDir, String name) {
        return resources.entrySet().stream()
                        .filter(e -> e.getValue().contains(name))
                        .map(e -> baseDir.resolve(e.getKey()).resolve(name))
                        .findAny()
                        .orElse(null);
    }

    private Map<String, List<String>> scanResources() {
        getLog().debug("Scanning project resources");
        Map<String, List<String>> allResources = new HashMap<>();
        for (Resource resource : project.getResources()) {
            List<String> resources = SourcePath.scan(new File(resource.getDirectory())).stream()
                                               .filter(p -> p.matches(resource.getIncludes(), resource.getExcludes()))
                                               .map(p -> p.asString(false))
                                               .collect(toList());
            if (getLog().isDebugEnabled()) {
                resources.forEach(r -> getLog().debug("Found resource: " + r));
            }
            allResources.put(resource.getDirectory(), resources);
        }
        return allResources;
    }
}
