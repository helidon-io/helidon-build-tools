/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;

import static io.helidon.sitegen.maven.Constants.PROPERTY_PREFIX;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Preprocesses AsciiDoc files to execute includes so the resulting
 * AsciiDoc output will render nicely on github (because github AsciiDoc
 * rendering does not yet support include itself).
 *
 */
@Mojo(name = "preprocess-adoc",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresProject = true)
public class PreprocessAsciiDocMojo extends AbstractMojo {

    private static final String DEFAULT_SRC_DIR = "${project.basedir}/src/main/docs";
    private static final String DEFAULT_OUTPUT_DIR = "${project.basedir}/target/docs";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = PROPERTY_PREFIX + "outputDirectory",
               defaultValue = DEFAULT_OUTPUT_DIR,
               required = true)
    private File outputDirectory;

    @Parameter(property = PROPERTY_PREFIX + "inputDirectory",
               defaultValue = DEFAULT_SRC_DIR,
               required = true)
    private File inputDirectory;

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        validateParams(inputDirectory, includes);
        try {
            for (Path p : inputs(inputDirectory.toPath(), includes, excludes)) {
                processFile(inputDirectory.toPath(), outputDirectory.toPath(), p);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Error collecting inputs", ex);
        }
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
        return Files.find(inputDirectory, 5, (path,attrs) ->
                        matches(path, pathMatchers(inputDirectory, includes))
                        && ! matches(path, pathMatchers(inputDirectory, excludes)))
                .collect(Collectors.toSet());
    }

    /**
     * Creates a stream of PathMatchers, one for each glob.
     * @param inputDirectory Path within which the globs are applied
     * @param globs the glob patterns
     * @return PathMatchers for the globs
     */
    static private Stream<PathMatcher> pathMatchers(Path inputDirectory, String [] globs) {
          return Arrays.stream(globs)
                .map(glob -> {
                    return FileSystems.getDefault().getPathMatcher("glob:" + inputDirectory + "/" + glob);
                });
    }

    static private boolean matches(Path candidate, Stream<PathMatcher> matchers) {
        return matchers.anyMatch((m) -> (m.matches(candidate)));
    }

    static void validateParams(File inputDirectory, String[] includes) throws MojoExecutionException {
        if (includes.length == 0) {
            throw new MojoExecutionException("You must specify at least one 'includes'");
        }
        if (! inputDirectory.exists() ||
            ! inputDirectory.isDirectory()) {
            throw new MojoExecutionException(
                    String.format("inputDirectory %s does not exist or is not a directory", inputDirectory));
        }
    }

    private void processFile(Path inputDirectory, Path outputDirectory, Path adocFilePath) {
        Path outputAdocPath = outputDirectory.resolve(inputDirectory.relativize(adocFilePath));
        System.out.println(String.format("input path: %s, output path: %s", inputDirectory.resolve(adocFilePath),
                outputDirectory.resolve(adocFilePath)));



    }
}
