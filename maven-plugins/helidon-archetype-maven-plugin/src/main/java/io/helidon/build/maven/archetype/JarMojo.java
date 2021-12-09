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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Property;
import io.helidon.build.archetype.engine.v1.MustacheHelper.RawString;
import io.helidon.build.common.SourcePath;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static io.helidon.build.archetype.engine.v1.MustacheHelper.MUSTACHE_EXT;
import static io.helidon.build.archetype.engine.v1.MustacheHelper.renderMustacheTemplate;
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

    private static final String ENGINE_GROUP_ID = MojoHelper.PLUGIN_GROUP_ID + ".archetype";
    private static final String ENGINE_ARTIFACT_ID = "helidon-archetype-engine-v1";
    private static final String ENGINE_VERSION = MojoHelper.PLUGIN_VERSION;
    private static final String POST_SCRIPT_NAME = "archetype-post-generate.groovy";
    private static final String POST_SCRIPT_PKG = "io/helidon/build/maven/archetype/postgenerate";
    private static final Pattern COPYRIGHT_HEADER = Pattern.compile("^(\\s?\\R)?\\/\\*.*\\*\\/(\\s?\\R)?", DOTALL);
    private static final String SCHEMA_LANG = "http://www.w3.org/2001/XMLSchema";
    private static final String ARCHETYPE_ROOT_ELEMENT = "archetype-script";

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
        Map<String, List<String>> resources = scanResources();
        validateArchetypeScripts(resources);
        Path archetypeDir = outputDirectory.toPath().resolve("archetype");
        Path baseDir = project.getBasedir().toPath();

        //TODO uncomment later
//        if (mavenArchetypeCompatible) {
//            processMavenCompat(archetypeDir, null);
//        }

        File jarFile = generateArchetypeJar(archetypeDir);
        project.getArtifact().setFile(jarFile);
    }

    private void validateArchetypeScripts(Map<String, List<String>> resources) throws MojoExecutionException {
        File schemaFile = getArchetypeSchemaFile();
        if (schemaFile == null) {
            return;
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(SCHEMA_LANG);
            Schema schema = factory.newSchema(schemaFile);
            Validator validator = schema.newValidator();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            for (Entry<String, List<String>> resourcesEntry : resources.entrySet()) {
                for (String resource : resourcesEntry.getValue()) {
                    if (FilenameUtils.getExtension(resource).equalsIgnoreCase("xml")) {
                        File xmlFile = Path.of(resourcesEntry.getKey(), resource).toFile();
                        Document doc = db.parse(xmlFile);
                        if (doc.getDocumentElement().getNodeName().equals(ARCHETYPE_ROOT_ELEMENT)) {
                            validator.validate(new StreamSource(xmlFile));
                        }
                    }
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }


    private File getArchetypeSchemaFile() {
        PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().get("pluginDescriptor");
        return pluginDescriptor.getArtifacts().stream()
                        .filter(artifact -> artifact.getArtifactId().equals("helidon-archetype-engine-v2")
                                && artifact.getClassifier().equals("schema")
                                && artifact.getType().equals("xsd")
                        ).findFirst().map(Artifact::getFile).orElse(null);
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

    private File generateArchetypeJar(Path archetypeDir) throws MojoExecutionException {
        //todo can be removed if processMavenCompat() will create the directory when it will be uncommented
        try {
            Files.createDirectories(archetypeDir);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
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
