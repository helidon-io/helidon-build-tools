/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.maven.plugin.MavenArtifact;
import io.helidon.build.common.xml.XMLGenerator;
import io.helidon.build.maven.archetype.MustacheHelper.RawString;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import static io.helidon.build.common.FileUtils.pathOf;
import static io.helidon.build.common.FileUtils.toBase64;
import static io.helidon.build.maven.archetype.MustacheHelper.MUSTACHE_EXT;
import static io.helidon.build.maven.archetype.MustacheHelper.renderMustacheTemplate;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.DOTALL;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

/**
 * {@code archetype:jar} mojo.
 */
@Mojo(name = "jar", defaultPhase = PACKAGE, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class JarMojo extends AbstractMojo {

    private static final String POST_SCRIPT_NAME = "archetype-post-generate.groovy";
    private static final String POST_SCRIPT_PKG = "io/helidon/build/maven/archetype/postgenerate";
    private static final Pattern COPYRIGHT_HEADER = Pattern.compile("^(\\s?\\R)?/\\*.*\\*/(\\s?\\R)?", DOTALL);

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The archetype directory to archive.
     */
    @Parameter(defaultValue = "${project.build.directory}/archetype", readonly = true, required = true)
    private File archetypeDirectory;

    /**
     * The build output directory. (e.g. {@code target/})
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File outputDirectory;

    /**
     * Name of the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.finalName}", alias = "jarName", required = true)
    private String finalName;

    /**
     * The {@link org.apache.maven.execution.MavenSession}.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The archivers.
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

    /**
     * Indicate if the generated JAR should be compatible with the {@code maven-archetype-plugin}.
     */
    @Parameter(property = "archetype.jar.mavenArchetypeCompatible", defaultValue = "true")
    private boolean mavenArchetypeCompatible;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Path archetypeDir = archetypeDirectory.toPath();
            if (mavenArchetypeCompatible) {
                processMavenCompat(archetypeDir);
            }
            File jarFile = createJar(archetypeDir);
            project.getArtifact().setFile(jarFile);
        } catch (IOException ioe) {
            throw new MojoExecutionException(ioe.getMessage(), ioe);
        }
    }

    private void renderPostScript(Path archetypeDir) throws IOException {
        // mustache scope
        Map<String, Object> scope = new HashMap<>();

        // base64 encoded byte code keyed by fully qualified class names
        Set<Map.Entry<String, RawString>> classes = postGenerateClasses()
                .filter(path -> path.toString().endsWith(".class"))
                .map(this::encodeClass)
                .collect(toSet());

        scope.put("classes", classes);

        // name of the properties to pass to the Helidon archetype engine
        scope.put("propNames", Map.of());

        // dependencies
        List<String> dependencies = project.getDependencies().stream()
                .filter(d -> !"test".equals(d.getScope()) && !d.isOptional())
                .map(MavenArtifact::new)
                .map(MavenArtifact::coordinates)
                .collect(Collectors.toList());
        scope.put("dependencies", dependencies);

        // The non-mustache post-generate script that contains the postGenerate function
        String postGenerateScript = new String(resourceAsBytes(POST_SCRIPT_NAME), UTF_8);
        postGenerateScript = COPYRIGHT_HEADER.matcher(postGenerateScript).replaceAll("");

        scope.put("postGenerateScript", new RawString(postGenerateScript));

        getLog().info("Rendering " + POST_SCRIPT_NAME);

        renderMustacheTemplate(
                resolveResource(POST_SCRIPT_NAME + MUSTACHE_EXT).openStream(), POST_SCRIPT_NAME,
                archetypeDir.resolve("META-INF/" + POST_SCRIPT_NAME),
                scope);
    }

    private void processMavenCompat(Path archetypeDir) throws IOException {
        getLog().info("Processing maven-archetype-plugin compatibility");

        Path descriptor = archetypeDir.resolve("META-INF/maven/archetype-metadata.xml");

        // create target/archetype/META-INF/maven
        Files.createDirectories(descriptor.getParent());

        // create target/archetype/META-INF/maven/archetype-metadata.xml
        try (XMLGenerator generator = new XMLGenerator(Files.newBufferedWriter(descriptor), true)) {
            generator.prolog();
            generator.startElement("archetype-descriptor");
            generator.attribute("name", project.getArtifactId());
            generator.endElement();
        }

        Path pom = archetypeDir.resolve("archetype-resources/pom.xml");

        // create target/archetype/archetype-resources
        Files.createDirectories(pom.getParent());

        // create an empty file target/archetype/archetype-resources/pom.xml
        if (!Files.exists(pom)) {
            Files.createFile(pom);
        }

        renderPostScript(archetypeDir);
    }

    private File createJar(Path archetypeDir) throws MojoExecutionException, IOException {
        File jarFile = new File(outputDirectory, finalName + ".jar");
        MavenArchiver archiver = new MavenArchiver();
        archiver.setCreatedBy("Helidon Archetype Plugin", MojoHelper.PLUGIN_GROUP_ID, MojoHelper.PLUGIN_ARTIFACT_ID);
        archiver.setOutputFile(jarFile);
        archiver.setArchiver((JarArchiver) archivers.get("jar"));
        archiver.configureReproducibleBuild(outputTimestamp);
        try {
            archiver.getArchiver().addDirectory(archetypeDir.toFile());
            archiver.createArchive(session, project, archive);
        } catch (DependencyResolutionRequiredException | ArchiverException | ManifestException e) {
            throw new MojoExecutionException("Error assembling archetype jar " + jarFile, e);
        }
        return jarFile;
    }

    private Stream<Path> postGenerateClasses() throws IOException {
        try {
            URL url = this.getClass().getClassLoader().getResource(POST_SCRIPT_PKG);
            return url == null ? Stream.empty() : Files.walk(pathOf(url.toURI()));
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Map.Entry<String, RawString> encodeClass(Path classFile) {
        String className = classFile.toString().substring(1).replace('/', '.').replace(".class", "");
        return Map.entry(className, new RawString(toBase64(classFile).replaceAll("(.{100})", "$1\n")));
    }

    @SuppressWarnings("SameParameterValue")
    private byte[] resourceAsBytes(String path) throws IOException {
        try (InputStream is = resolveResource(path).openStream()) {
            return is.readAllBytes();
        }
    }

    private URL resolveResource(String path) {
        URL resource = JarMojo.class.getClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Unable to resolve resource: " + path);
        }
        return resource;
    }
}
