/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.maven.link;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.helidon.build.util.Log;
import io.helidon.build.util.MavenLogWriter;
import io.helidon.linker.Configuration;
import io.helidon.linker.Linker;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import static io.helidon.linker.util.Constants.JRI_DIR_SUFFIX;

/**
 * Maven goal to create a custom Java Runtime Image.
 */
@Mojo(name = "jlink-image",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class JLinkImageMojo extends AbstractMojo {

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The project build output directory. (e.g. {@code target/})
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File buildDirectory;

    /**
     * Name of the output directory to be generated.
     */
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    private String finalName;

    /**
     * Add a Class Data Sharing archive to reduce startup time.
     */
    @Parameter(defaultValue = "true", property = "jlink.image.addClassDataSharingArchive")
    private boolean addClassDataSharingArchive;

    /**
     * Test the image after creation.
     */
    @Parameter(defaultValue = "true", property = "jlink.image.testImage")
    private boolean testImage;

    /**
     * Default JVM options to use when starting the application.
     */
    @Parameter(property = "jlink.image.defaultJvmOptions")
    private List<String> defaultJvmOptions;

    /**
     * Default JVM debug options to use when starting the application with {@code --debug}.
     */
    @Parameter(property = "jlink.image.defaultDebugOptions")
    private List<String> defaultDebugOptions;

    /**
     * Default arguments to use when starting the application.
     */
    @Parameter(property = "jlink.image.defaultArgs")
    private List<String> defaultArgs;

    /**
     * The maximum number of seconds to wait for the application to start.
     */
    @Parameter(defaultValue = "60", property = "jlink.image.maxAppStartSeconds")
    private int maxAppStartSeconds;

    /**
     * Strip debug information from all classes and exclude {@code jdk.jdwp.agent} module.
     */
    @Parameter(defaultValue = "false", property = "jlink.image.stripDebug")
    private boolean stripDebug;

    /**
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "jlink.image.skip")
    private boolean skipJavaImage;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipJavaImage) {
            getLog().info("Skipping execution.");
            return;
        }
        final Path buildDir = buildDirectory.toPath();
        final Path mainJar = mainJar(buildDir);
        final Path outputDir = buildDir.resolve(finalName + JRI_DIR_SUFFIX);
        final Log.Writer writer = new MavenLogWriter(getLog());
        try {
            Configuration config = Configuration.builder()
                                                .logWriter(writer)
                                                .verbose(getLog().isDebugEnabled())
                                                .mainJar(mainJar)
                                                .defaultJvmOptions(defaultJvmOptions)
                                                .defaultArgs(defaultArgs)
                                                .defaultDebugOptions(defaultDebugOptions)
                                                .cds(addClassDataSharingArchive)
                                                .stripDebug(stripDebug)
                                                .test(testImage)
                                                .jriDirectory(outputDir)
                                                .maxAppStartSeconds(maxAppStartSeconds)
                                                .replace(true)
                                                .build();
            Linker.linker(config).link();
        } catch (Exception e) {
            throw new MojoExecutionException("Image generation error", e);
        }
    }

    private Path mainJar(Path buildDir) throws MojoFailureException {
        Path result;
        File artifact = project.getArtifact().getFile();
        if (artifact == null) {
            result = buildDir.resolve(project.getBuild().getFinalName() + ".jar");
        } else {
            result = artifact.toPath();
        }
        if (!Files.exists(result)) {
            throw new MojoFailureException("Artifact does not exist: " + result.toAbsolutePath());
        }
        return result;
    }
}
