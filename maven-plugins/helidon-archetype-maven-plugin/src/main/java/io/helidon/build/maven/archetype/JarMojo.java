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
package io.helidon.build.maven.archetype;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.v2.util.ArchetypeValidator;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.VirtualFileSystem;
import io.helidon.build.maven.archetype.MustacheHelper.RawString;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.ModelReader;
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
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import static io.helidon.build.common.FileUtils.pathOf;
import static io.helidon.build.common.FileUtils.toBase64;
import static io.helidon.build.maven.archetype.MustacheHelper.MUSTACHE_EXT;
import static io.helidon.build.maven.archetype.MustacheHelper.renderMustacheTemplate;
import static io.helidon.build.maven.archetype.Schema.RESOURCE_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
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
    private static final String MAVEN_URL_REPO_PROPERTY = "io.helidon.build.common.maven.url.localRepo";

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The archetype source directory.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/archetype", readonly = true, required = true)
    private File sourceDirectory;

    /**
     * The project build output directory. (e.g. {@code target/})
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
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
     * Indicate if the generated JAR should be compatible with the {@code maven-archetype-plugin}.
     */
    @Parameter(defaultValue = "true")
    private boolean mavenArchetypeCompatible;

    /**
     * Entrypoint configuration.
     */
    @Parameter
    private PlexusConfiguration entrypoint;

    private final Schema schema = new Schema(resolveResource(RESOURCE_NAME));

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Path archetypeDir = outputDirectory.toPath().resolve("archetype");
            Files.createDirectories(archetypeDir);
            processSources(archetypeDir);
            processEntryPoint(archetypeDir);
            validateEntryPoint(archetypeDir);
            if (mavenArchetypeCompatible) {
                processMavenCompat(archetypeDir);
            }
            File jarFile = generateArchetypeJar(archetypeDir);
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
                                           .map(this::coordinates)
                                           .collect(Collectors.toList());
        scope.put("dependencies", dependencies);

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

    private void processMavenCompat(Path archetypeDir) throws IOException {
        getLog().info("Processing maven-archetype-plugin compatibility");

        Path mavenArchetypeDescriptor = archetypeDir.resolve("META-INF/maven/archetype-metadata.xml");

        // create target/archetype/META-INF/maven
        Files.createDirectories(mavenArchetypeDescriptor.getParent());

        // create target/archetype/META-INF/maven/archetype-metadata.xml
        try (BufferedWriter writer = Files.newBufferedWriter(mavenArchetypeDescriptor)) {
            StringWriter sw = new StringWriter();
            Xpp3Dom root = new Xpp3Dom("archetype-descriptor");
            root.setAttribute("name", project.getArtifactId());
            Xpp3DomWriter.write(writer, root);
            writer.append(sw.toString());
        }

        Path mavenArchetypePom = archetypeDir.resolve("archetype-resources/pom.xml");

        // create target/archetype/archetype-resources
        Files.createDirectories(mavenArchetypePom.getParent());

        // create an empty file target/archetype/archetype-resources/pom.xml
        if (!Files.exists(mavenArchetypePom)) {
            Files.createFile(mavenArchetypePom);
        }

        renderPostScript(archetypeDir);
    }

    private void processEntryPoint(Path outputDir) throws MojoExecutionException, IOException {
        if (entrypoint != null) {
            Path main = outputDir.resolve("main.xml");
            if (Files.exists(main)) {
                throw new MojoExecutionException("Cannot generate custom entry-point, main.xml already exists");
            }
            Converter.convert(entrypoint, main);
            validateSchema(outputDir, "main.xml").ifPresent(getLog()::error);
        }
    }

    private void validateEntryPoint(Path outputDir) throws MojoExecutionException {
        System.setProperty(MAVEN_URL_REPO_PROPERTY, session.getLocalRepository().getBasedir());
        Path script = VirtualFileSystem.create(outputDir).getPath("/").resolve("main.xml");
        List<String> errors = ArchetypeValidator.validate(script);
        List<String> regexErrors = Validator.regexValidation(script);
        errors.addAll(regexErrors);
        if (!errors.isEmpty()) {
            errors.forEach(getLog()::error);
            throw new MojoExecutionException("Validation failed");
        }
    }

    private File generateArchetypeJar(Path archetypeDir) throws MojoExecutionException, IOException {
        File jarFile = new File(outputDirectory, finalName + ".jar");
        MavenArchiver archiver = new MavenArchiver();
        archiver.setCreatedBy("Helidon Archetype Plugin", MojoHelper.PLUGIN_GROUP_ID, MojoHelper.PLUGIN_ARTIFACT_ID);
        archiver.setOutputFile(jarFile);
        archiver.setArchiver((JarArchiver) archivers.get("jar"));
        archiver.configureReproducible(outputTimestamp);
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
            URL url = this.getClass().getClassLoader().getResource(JarMojo.POST_SCRIPT_PKG);
            return url == null ? Stream.empty() : Files.walk(pathOf(url.toURI()));
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Map.Entry<String, RawString> encodeClass(Path classFile) {
        String className = classFile.toString().substring(1).replace('/', '.').replace(".class", "");
        return Map.entry(className, new RawString(toBase64(classFile).replaceAll("(.{100})", "$1\n")));
    }

    private void processSources(Path outputDir) throws MojoExecutionException {
        getLog().debug("Scanning source files");
        Path sourceDir = sourceDirectory.toPath();
        List<String> errors = new LinkedList<>();
        SourcePath.scan(sourceDir).stream()
                  .map(p -> p.asString(false))
                  .forEach(file -> {
                      if (getLog().isDebugEnabled()) {
                          getLog().debug("Found source file: " + file);
                      }
                      if (FilenameUtils.getExtension(file).equalsIgnoreCase("xml")) {
                          validateSchema(sourceDir, file).ifPresent(errors::add);
                      }
                      try {
                          Path target = outputDir.resolve(file);
                          Files.createDirectories(target.getParent());
                          Files.copy(sourceDir.resolve(file), target, REPLACE_EXISTING);
                      } catch (IOException ioe) {
                          throw new UncheckedIOException(ioe);
                      }
                  });
        if (!errors.isEmpty()) {
            errors.forEach(getLog()::error);
            throw new MojoExecutionException("Schema validation failed");
        }
    }

    private Optional<String> validateSchema(Path sourceDir, String filename) {
        try {
            schema.validate(sourceDir.resolve(filename));
            return Optional.empty();
        } catch (Schema.ValidationException ex) {
            return Optional.of(
                    String.format("Schema validation error: file=%s, position=%s:%s, error=%s",
                            filename,
                            ex.lineNo(),
                            ex.colNo(),
                            ex.getMessage()));
        }
    }

    private InputStream resolveResource(String path) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Unable to resolve resource: " + path);
        }
        return is;
    }

    @SuppressWarnings("SameParameterValue")
    private String coordinates(Dependency d) {
        String coords = d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getType();
        String classifier = d.getClassifier();
        if (classifier != null) {
            coords += ":" + classifier;
        }
        coords += ":" + d.getVersion();
        return coords;
    }
}
