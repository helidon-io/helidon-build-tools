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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.common.logging.Log;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.newZipFileSystem;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

/**
 * {@code archetype:stage} mojo.
 */
@Mojo(name = "stage", defaultPhase = PACKAGE, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class StageMojo extends AbstractMojo {

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Path of the {@code cli-data} directory to create.
     */
    @Parameter(defaultValue = "${project.build.directory}/cli-data",
               property = "archetype.stage.cliDataDirectory")
    private File cliDataDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Path cliData = ensureDirectory(cliDataDirectory.toPath());
            Path jarFile = project.getArtifact().getFile().toPath();
            String version = project.getVersion();
            Files.writeString(cliData.resolve("versions.xml"),
                    "<data>\n"
                    + "    <archetypes>\n"
                    + "        <version default=\"true\">" + version + "</version>\n"
                    + "    </archetypes>\n"
                    + "</data>\n");
            Path versionDir = ensureDirectory(cliData.resolve(version));
            try (FileSystem fs = newZipFileSystem(versionDir.resolve("cli-data.zip"))) {
                Files.copy(jarFile, fs.getPath("helidon-" + version + ".jar"), REPLACE_EXISTING);
            }

            // splash message
            Log.info("\n" + """
                |
                |                      /')
                |               $(cyan! /)$(blue /)$(blue! /)$(magenta /)  /' )'
                |              $(blue @)   \\/'  )'
                |  { $(yellow! note) }   $(yellow! <) (  (_...)'
                |               \\      )
                |                \\,,,,/
                |                 $(red _|_)
                |
                |  You can test the archetype locally with the Helidon CLI using the following arguments:
                |
                |     --reset --url %s
                |
                |  For example:
                |
                |     helidon init --reset --url %s
                |""", cliData.toUri(), cliData.toUri());
        } catch (IOException ioe) {
            throw new MojoExecutionException(ioe.getMessage(), ioe);
        }
    }
}
