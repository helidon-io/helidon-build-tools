/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.v2.Script;
import io.helidon.build.archetype.engine.v2.ScriptCompiler;
import io.helidon.build.common.Lists;
import io.helidon.build.common.VirtualFileSystem;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import static io.helidon.build.common.FileUtils.randomPath;
import static io.helidon.build.maven.archetype.ScriptCompilerExt.Options.VALIDATE_REGEX;
import static io.helidon.build.maven.archetype.ScriptCompilerExt.Options.VALIDATE_SCHEMA;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugins.annotations.LifecyclePhase.COMPILE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

/**
 * {@code archetype:compile} mojo.
 */
@Mojo(name = "compile", defaultPhase = COMPILE, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class CompileMojo extends AbstractMojo {

    /**
     * The archetype source directory.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/archetype", readonly = true, required = true)
    private File sourceDirectory;

    /**
     * The archetype output directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/archetype", readonly = true, required = true)
    private File archetypeDirectory;

    /**
     * List of compiler options.
     */
    @Parameter
    private List<String> compilerOptions = List.of();

    /**
     * Entrypoint configuration.
     */
    @Parameter
    private PlexusConfiguration entrypoint;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            try (FileSystem fs = VirtualFileSystem.create(sourceDirectory.toPath())) {
                Path cwd = fs.getPath("/");
                ScriptCompiler compiler = new ScriptCompiler(entrypoint(cwd), cwd);
                List<ScriptCompiler.Option> options = Lists.addAll(
                        Lists.map(compilerOptions, ScriptCompiler.Options::valueOf),
                        VALIDATE_REGEX,
                        VALIDATE_SCHEMA);
                if (!compiler.compile(archetypeDirectory.toPath(), options)) {
                    compiler.errors().forEach(getLog()::error);
                    throw new MojoExecutionException("Validation failed");
                }
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException(ioe.getMessage(), ioe);
        }
    }

    private Script.Source entrypoint(Path dir) throws IOException {
        if (entrypoint != null) {
            Path path = randomPath(dir, "entrypoint-", ".xml");
            byte[] bytes = entrypointXml().getBytes(UTF_8);
            return new Script.Source() {
                @Override
                public InputStream inputStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public Path path() {
                    return path;
                }
            };
        }
        // assuming main exists in the source directory
        return () -> dir.resolve("main.xml");
    }

    private String entrypointXml() {
        return "<archetype-script xmlns=\"https://helidon.io/archetype/2.0\"\n"
               + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
               + "        xsi:schemaLocation=\"https://helidon.io/archetype/2.0 https://helidon.io/xsd/archetype-2.0.xsd\">"
               + Stream.of(entrypoint.getChildren())
                       .map(PlexusConfiguration::toString)
                       .collect(Collectors.joining())
               + "</archetype-script>";
    }
}
