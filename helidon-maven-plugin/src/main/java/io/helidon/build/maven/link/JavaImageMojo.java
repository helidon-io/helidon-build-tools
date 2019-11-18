/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

import io.helidon.linker.Configuration;
import io.helidon.linker.Linker;
import io.helidon.linker.util.Log;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Maven goal to create a custom Java Runtime Image {@code java-image}.
 */
@Mojo(name = "java-image",
    defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyResolution = ResolutionScope.RUNTIME)
public class JavaImageMojo extends AbstractMojo {

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
     * Add a Class Data Sharing archive.
     */
    @Parameter(defaultValue = "true", property = "java.image.addClassDataSharingArchive")
    private boolean addClassDataSharingArchive;

    /**
     * Default JVM options to use when starting the application.
     */
    @Parameter(property = "java.image.defaultJvmOptions")
    private List<String> defaultJvmOptions;

    /**
     * Default JVM debug options to use when starting the application with {@code --debug}.
     */
    @Parameter(property = "java.image.defaultDebugOptions")
    private List<String> defaultDebugOptions;

    /**
     * Default arguments to use when starting the application.
     */
    @Parameter(property = "java.image.defaultArgs")
    private List<String> defaultArgs;

    /**
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "java.image.skip")
    private boolean skipJavaImage;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipJavaImage) {
            getLog().info("Skipping execution.");
            return;
        }
        final Path buildDir = buildDirectory.toPath();
        final Path mainJar = mainJar(buildDir);
        final Path outputDir = buildDir.resolve(finalName);
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
                                                .jriDirectory(outputDir)
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
