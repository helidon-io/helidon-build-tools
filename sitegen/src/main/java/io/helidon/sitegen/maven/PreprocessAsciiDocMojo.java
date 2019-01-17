/*
 * Copyright (c) 2018-2019 Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package io.helidon.sitegen.maven;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.sitegen.asciidoctor.AsciidocExtensionRegistry;
import static io.helidon.sitegen.maven.Constants.PROPERTY_PREFIX;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;

/**
 * Preprocesses AsciiDoc files to "pre-execute" includes so the resulting
 * AsciiDoc output will render nicely on GitHub (because GitHub AsciiDoc
 * rendering does not yet support include).
 * <p>
 * Settings for the goal:
 * <table>
 * <tr>
 * <th>Property</th>
 * <th>Usage</th>
 * </tr>
 *
 * <tr>
 * <td>inputDirectory</td>
 * <td>directory containing the files to be processed</td>
 * </tr>
 *
 * <tr>
 * <td>outputDirectory</td>
 * <td>where the reformatted .adoc file should be written</td>
 * </tr>
 *
 * <tr>
 * <td>includes</td>
 * <td>glob expressions for .adoc files to process</td>
 * </tr>
 *
 * <tr>
 * <td>excludes</td>
 * <td>glob expressions for .adoc files to skip</td>
 * </tr>
 *
 * <tr>
 * <td>outputType</td>
 * <td>type of output: numbered (default) or natural. Natural uses only normal
 * AsciiDoc {@code include::} directives which makes it easier for developers to
 * edit the .adoc file.
 * </tr>
 *
 * <tr>
 * <td>check</td>
 * <td>whether to check that the input and output are the same. Typically used
 * in pipeline builds to make sure that the developer committed a pre-included
 * version of the .adoc file.
 * </td>
 * </tr>
 * </table>
 *
 */
@Mojo(name = "preprocess-adoc",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresProject = true)
public class PreprocessAsciiDocMojo extends AbstractMojo {

    private static final String DEFAULT_SRC_DIR = "${project.basedir}";
    private static final String JRUBY_DEBUG_PROPERTY_NAME = "jruby.cli.verbose";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = PROPERTY_PREFIX + "inputDirectory",
               defaultValue = DEFAULT_SRC_DIR,
               required = true)
    private File inputDirectory;

    /**
     * Where the pre-included file should be stored; not stored if
     * not specified.
     */
    @Parameter(property = PROPERTY_PREFIX + "outputDirectory",
            defaultValue = "${project.basedir}")
    private File outputDirectory;

    /**
     * List of files to include.
     */
    @Parameter(property = PROPERTY_PREFIX + "includes",
            required = true)
    private String[] includes;

    /**
     * List of files to exclude.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludes")
    private String[] excludes;

    /**
     * Type of output to produce: either numbered (with numbered comments
     * annotating the included text) or natural (with the normal AsciiDoc-style
     * include directives).
     */
    @Parameter(property = PROPERTY_PREFIX + "outputType",
            defaultValue = "numbered")
    private String outputType;

    /**
     * Whether to check that the input and output files are the same. Primarily
     * intended for use during a pipeline build to make sure that the .adoc file
     * in the repository is the same as the pre-included form (to ensure that the
     * developer has included the updated pre-included file in the commit).
     */
    @Parameter(property = PROPERTY_PREFIX + "check",
            defaultValue = "false")
    private boolean check;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        validateParams(inputDirectory, includes);

        // enable jruby verbose mode on debugging
        final String previousJRubyCliVerboseValue = System.getProperty(JRUBY_DEBUG_PROPERTY_NAME);
        if (getLog().isDebugEnabled()) {
            System.setProperty(JRUBY_DEBUG_PROPERTY_NAME, "true");
        }

        Asciidoctor asciiDoctor = createAsciiDoctor("simple");
        try {
            for (Path p : inputs(inputDirectory.toPath(), includes, excludes)) {
                processFile(asciiDoctor, inputDirectory.toPath(), p);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Error collecting inputs", ex);
        } finally {
            if (getLog().isDebugEnabled()) {
                if (previousJRubyCliVerboseValue == null) {
                    System.clearProperty(JRUBY_DEBUG_PROPERTY_NAME);
                } else {
                    System.setProperty(JRUBY_DEBUG_PROPERTY_NAME, previousJRubyCliVerboseValue);
                }
            }
        }
    }

    /**
     * @return the Maven project for this mojo
     */
    public MavenProject project() {
        return project;
    }

    /**
     * Computes paths to be processed as inputs, based on an input directory
     * and glob-style include and exclude expressions identifying paths within
     * that input directory.
     * @param inputDirectory the directory within which to search for files
     * @param includes glob-style include expressions
     * @param excludes glob-style exclude expressions
     * @return Paths within the input directory tree that match the includes and
     * are not ruled out by the excludes
     * @throws IOException in case of errors matching candidate paths
     */
    static Collection<Path> inputs(Path inputDirectory, String[] includes, String[] excludes) throws IOException {
        return Files.find(inputDirectory, Integer.MAX_VALUE, (path, attrs) ->
                        matches(path, pathMatchers(inputDirectory, includes))
                        && !matches(path, pathMatchers(inputDirectory, excludes)))
                .collect(Collectors.toSet());
    }

    /**
     * Creates a stream of PathMatchers, one for each glob.
     * @param inputDirectory Path within which the globs are applied
     * @param globs the glob patterns
     * @return PathMatchers for the globs
     */
    private static Stream<PathMatcher> pathMatchers(Path inputDirectory, String[] globs) {
          return Arrays.stream(globs)
                .map(glob -> {
                    return FileSystems.getDefault().getPathMatcher("glob:" + inputDirectory + "/" + glob);
                });
    }

    private static boolean matches(Path candidate, Stream<PathMatcher> matchers) {
        return matchers.anyMatch((m) -> (m.matches(candidate)));
    }

    static void validateParams(File inputDirectory, String[] includes) throws MojoExecutionException {
        if (includes.length == 0) {
            throw new MojoExecutionException("You must specify at least one 'includes'");
        }
        if (!inputDirectory.exists()
                || !inputDirectory.isDirectory()) {
            throw new MojoExecutionException(
                    String.format(
                            "inputDirectory %s does not exist or is not a directory",
                            inputDirectory));
        }
    }

    private void processFile(
            Asciidoctor asciiDoctor,
            Path inputDirectory,
            Path adocFilePath) throws IOException, MojoFailureException, MojoExecutionException {

        Path relativeInputPath = inputDirectory.relativize(adocFilePath);
        Path outputPath = outputDirectory.toPath().resolve(relativeInputPath);

        getLog().info(String.format("processing %s to format '%s' in %s",
                adocFilePath.toString(),
                outputType,
                outputPath.toString()));


        asciiDoctor.loadFile(adocFilePath.toFile(),
                asciiDoctorOptions(
                        projectPropertiesMap(project),
                        relativeInputPath,
                        outputDirectory,
                        inputDirectory.toAbsolutePath()));
        /*
         * We do not need to convert the document because the
         * preprocessor has written the updated version of the .adoc
         * file already and we do not need any rendered output as a
         * result of invoking this mojo.
         */

        if (check) {
            compareFiles(adocFilePath, outputPath);
        }
    }

    private void compareFiles(Path pathA, Path pathB) throws IOException, MojoFailureException, MojoExecutionException {
        if (pathA.equals(pathB)) {
            getLog().warn(
                    new IllegalArgumentException(
                        "'check' set to true but it will always pass: "
                                + "input and output files are the same"));
        }
        try {
            byte[] inputDigest = digest(pathA);
            byte[] outputDigest = digest(pathB);
            if (!Arrays.equals(inputDigest, outputDigest)) {
                throw new MojoFailureException(String.format(
                        "file %s does not match its expected pre-included form; "
                                + "the commit might need an up-to-date file from running 'preinclude-adoc' ",
                        pathA.toString()));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException("error checking for matching input and output files", e);
        }
    }

    private byte[] digest(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] buffer = new byte[256];
        try (InputStream is = new BufferedInputStream(Files.newInputStream(path));
             DigestInputStream dis = new DigestInputStream(is, md)) {
            while (dis.read(buffer) != -1) {}
        }
        return md.digest();
    }

    private Asciidoctor createAsciiDoctor(String backendName) {
        Asciidoctor asciiDoctor = Asciidoctor.Factory.create();
        asciiDoctor.registerLogHandler(new LogHandler() {
                @Override
                public void log(LogRecord logRecord) {
                    System.err.println(logRecord.getMessage());
                }
            });
        new AsciidocExtensionRegistry(backendName).register(asciiDoctor);
        return asciiDoctor;
    }

    private Map<String, Object> asciiDoctorOptions(
            Map<String, Object> attributes,
            Path inputRelativePath,
            File outputDirectory,
            Path baseDirPath) {
        final OptionsBuilder optionsBuilder = OptionsBuilder.options()
                .attributes(
                        AttributesBuilder
                                .attributes()
                                .attributes(attributes))
                .safe(SafeMode.UNSAFE)
                .headerFooter(false)
                .baseDir(baseDirPath.toFile())
                .eruby("")
                .option("preincludeOutputType", outputType);
        if (outputDirectory != null) {
            optionsBuilder.option("preincludeOutputPath",
                        outputDirectory.toPath().resolve(inputRelativePath));
        }

        return optionsBuilder.asMap();
    }

    private Map<String, Object> projectPropertiesMap(MavenProject project) {
        Properties properties = new Properties();
        properties.putAll(project.getProperties());
        properties.setProperty("project.groupId", project.getGroupId());
        properties.setProperty("project.artifactId", project.getArtifactId());
        properties.setProperty("project.version", project.getVersion());
        properties.setProperty("project.basedir", project.getBasedir().getAbsolutePath());

        return properties.entrySet().stream()
                .collect(Collectors.toMap(entry -> String.class.cast(entry.getKey()),
                                          entry -> entry.getValue()));
    }
}
