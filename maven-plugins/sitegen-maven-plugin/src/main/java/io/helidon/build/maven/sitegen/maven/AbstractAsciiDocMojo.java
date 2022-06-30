/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.sitegen.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.Maps;
import io.helidon.build.common.maven.plugin.PlexusLoggerHolder;
import io.helidon.build.maven.sitegen.asciidoctor.AsciidocExtensionRegistry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;

import static io.helidon.build.maven.sitegen.maven.Constants.PROPERTY_PREFIX;

/**
 * Processes AsciiDoc files to convert them between "natural" format (with
 * standard AsciiDoc {@code include::} includes) and "preprocessed" format, (with
 * comments identifying included text and the text pre-included.
 * <p>
 * The "preprocessed" format renders nicely on GitHub (because GitHub AsciiDoc
 * rendering does not yet support {@code include::}). The "natural" format is
 * more convenient for developers to edit because it uses the familiar AsciiDoc
 * {@code include::} syntax.
 * <p>
 * This class contains the behavior common between the two separate mojos
 * ({@link PreprocessAsciiDocMojo} and {@link NaturalizeAsciiDocMojo}).
 * <table>
 * <caption>Settings common to both goals</caption>
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
 * </table>
 */
public abstract class AbstractAsciiDocMojo extends AbstractMojo {

    private static final String DEFAULT_SRC_DIR = "${project.basedir}";
    private static final String JRUBY_DEBUG_PROPERTY_NAME = "jruby.cli.verbose";
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    @Component
    @SuppressWarnings("unused")
    private PlexusLoggerHolder plexusLogHolder;

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
            required = true,
            defaultValue = "README.adoc")
    private String[] includes;

    /**
     * List of files to exclude.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludes")
    private String[] excludes;

    /**
     * @return the Maven project for this mojo
     */
    public MavenProject project() {
        return project;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        validateParams(inputDirectory, includes);

        // enable jruby verbose mode on debugging
        final String previousJRubyCliVerboseValue = System.getProperty(JRUBY_DEBUG_PROPERTY_NAME);
        if (getLog().isDebugEnabled()) {
            System.setProperty(JRUBY_DEBUG_PROPERTY_NAME, "true");
        }

        AtomicBoolean isPrelim = new AtomicBoolean();
        Asciidoctor asciiDoctor = createAsciiDoctor("simple", isPrelim);
        try {
            for (Path p : inputs(inputDirectory.toPath(), includes, excludes)) {
                processFile(asciiDoctor, inputDirectory.toPath(), p, isPrelim);
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
     * Computes paths to be processed as inputs, based on an input directory
     * and glob-style include and exclude expressions identifying paths within
     * that input directory.
     *
     * @param inputDirectory the directory within which to search for files
     * @param includes       glob-style include expressions
     * @param excludes       glob-style exclude expressions
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
     * @return output type produced by the mojo
     */
    abstract String outputType();

    /**
     * Performs any work needed after the file has been processed.
     *
     * @param adocFilePath path to the AsciiDoc input file
     * @param outputPath   path to the AsciiDoc output file
     * @throws IOException            in case of I/O errors working with the files
     * @throws MojoFailureException   in case the post-processing finds a user-correctable error
     * @throws MojoExecutionException in case the post-processing encounters a system error
     */
    void postProcessFile(Path adocFilePath, Path outputPath) throws
            IOException, MojoFailureException, MojoExecutionException {
    }

    /**
     * Processes the AsciiDoctor file using a previously-created Asciidoctor
     * instance.
     *
     * @param asciiDoctor    Asciidoctor instance (reusable for multiple files)
     * @param inputDirectory Path for the directory where the input file resides
     * @param adocFilePath   Full Path for the input file
     * @throws IOException            in case of I/O errors working with the files
     * @throws MojoFailureException   in case the post-processing finds a user-correctable error
     * @throws MojoExecutionException in case the post-processing encounters a system error
     */
    void processFile(
            Asciidoctor asciiDoctor,
            Path inputDirectory,
            Path adocFilePath,
            AtomicBoolean isPrelim) throws IOException, MojoFailureException, MojoExecutionException {

        Path relativeInputPath = inputDirectory.relativize(adocFilePath);
        Path outputPath = outputDirectory.toPath().resolve(relativeInputPath);

        getLog().info(String.format("processing %s to format '%s' in %s",
                adocFilePath,
                outputType(),
                outputPath));

        /*
         * Process the document once, suppressing the preprocessing,
         * to gather attributes that might be needed to resolve include
         * references during the second, real AsciiDoctor processing.
         */
        Map<String, Object> attributes = new HashMap<>(projectProperties(project));
        isPrelim.set(true);
        Document doc = asciiDoctor.loadFile(adocFilePath.toFile(),
                asciiDoctorOptions(
                        attributes,
                        relativeInputPath,
                        outputDirectory,
                        inputDirectory.toAbsolutePath(),
                        false));

        attributes.putAll(doc.getAttributes());

        isPrelim.set(false);
        asciiDoctor.loadFile(adocFilePath.toFile(),
                asciiDoctorOptions(
                        attributes,
                        relativeInputPath,
                        outputDirectory,
                        inputDirectory.toAbsolutePath(),
                        true));
        /*
         * We do not need to convert the document because the
         * preprocessor has written the updated version of the .adoc
         * file already, and we do not need any rendered output as a
         * result of invoking this mojo.
         */

        postProcessFile(adocFilePath, outputPath);

    }

    /**
     * Creates a stream of PathMatchers, one for each glob.
     *
     * @param inputDirectory Path within which the globs are applied
     * @param globs          the glob patterns
     * @return PathMatchers for the globs
     */
    private static Stream<PathMatcher> pathMatchers(Path inputDirectory, String[] globs) {
        return Arrays.stream(globs)
                     .map(glob -> {
                         if (WINDOWS) {
                             String pattern = "glob:" + inputDirectory + File.separator + glob.replace("/", "\\");
                             return FileSystems.getDefault().getPathMatcher(pattern.replace("\\", "\\\\"));
                         }
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

    private Asciidoctor createAsciiDoctor(String backendName, AtomicBoolean isPrelim) {
        Asciidoctor asciiDoctor = Asciidoctor.Factory.create();
        asciiDoctor.registerLogHandler(new SelectiveLogHandler(isPrelim));
        AsciidocExtensionRegistry.create(backendName).register(asciiDoctor);
        return asciiDoctor;
    }

    private static class SelectiveLogHandler implements LogHandler {

        private final AtomicBoolean isPrelim;

        private SelectiveLogHandler(AtomicBoolean isPrelim) {
            this.isPrelim = isPrelim;
        }

        @Override
        public void log(LogRecord logRecord) {
            if (!isPrelim.get()) {
                System.err.println(logRecord.getMessage());
            }
        }
    }

    private Options asciiDoctorOptions(Map<String, Object> attributes,
                                       Path inputRelativePath,
                                       File outputDirectory,
                                       Path baseDirPath,
                                       boolean runPreprocessing) {
        OptionsBuilder optionsBuilder =
                Options.builder()
                       .attributes(Attributes.builder().attributes(attributes).build())
                       .safe(SafeMode.UNSAFE)
                       .headerFooter(false)
                       .baseDir(baseDirPath.toFile())
                       .eruby("");
        if (outputDirectory != null) {
            optionsBuilder.option("preincludeOutputPath", outputDirectory.toPath().resolve(inputRelativePath));
        }
        if (runPreprocessing) {
            optionsBuilder.option("preprocessOutputType", outputType());
        }
        return optionsBuilder.build();
    }

    /**
     * Get the project properties.
     *
     * @param project project
     * @return map
     */
    static Map<String, String> projectProperties(MavenProject project) {
        Map<String, String> map = new HashMap<>(Maps.fromProperties(project.getProperties()));
        map.put("project.groupId", project.getGroupId());
        map.put("project.artifactId", project.getArtifactId());
        map.put("project.version", project.getVersion());
        map.put("project.basedir", project.getBasedir().getAbsolutePath());
        return map;
    }
}
