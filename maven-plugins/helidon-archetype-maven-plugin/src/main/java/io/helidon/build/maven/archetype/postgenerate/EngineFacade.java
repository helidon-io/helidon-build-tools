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
package io.helidon.build.maven.archetype.postgenerate;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.project.ProjectBuildingRequest;

/**
 * A utility to download the Helidon archetype engine with Aether and invoke it.
 */
@SuppressWarnings("unused")
public final class EngineFacade {

    private EngineFacade() {
    }

    private static final String MAVEN_CORE_POM_PROPERTIES = "META-INF/maven/org.apache.maven/maven-core/pom.properties";

    private static String getMavenVersion() {
        // see org.apache.maven.rtinfo.internal.DefaultRuntimeInformation
        Properties props = new Properties();
        try {
            props.load(EngineFacade.class.getClassLoader().getResourceAsStream(MAVEN_CORE_POM_PROPERTIES));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        String version = props.getProperty("version", "").trim();
        return version.startsWith("${") ? "" : version;
    }

    private static void checkMavenVersion() {
        String mavenVersion = getMavenVersion();
        ComparableVersion minMavenVersion = new ComparableVersion("3.2.5");
        if (!mavenVersion.isEmpty() && new ComparableVersion(mavenVersion).compareTo(minMavenVersion) < 0) {
            throw new IllegalStateException("Requires Maven >= 3.2.5");
        }
    }

    private static void checkJavaVersion() {
        try {
            //noinspection ConstantConditions,AccessStaticViaInstance
            if (Runtime.class.getMethod("version") != null
                    && Runtime.Version.class.getMethod("feature") != null
                    && Runtime.getRuntime().version().feature() < 11) {
                throw new IllegalStateException();
            }
        } catch (NoSuchMethodException | IllegalStateException ex) {
            throw new IllegalStateException("Requires Java >= 11");
        } catch (Throwable ex) {
            throw new IllegalStateException("Unable to verify Java version", ex);
        }
    }

    /**
     * Generate a project.
     *
     * @param request   archetype generation request
     * @param engineGAV Helidon archetype engine Maven coordinates
     */
    @SuppressWarnings("unused")
    public static void generate(ArchetypeGenerationRequest request, String engineGAV) {
        checkMavenVersion();
        checkJavaVersion();

        ProjectBuildingRequest mavenRequest = request.getProjectBuildingRequest();
        File localRepo = mavenRequest.getRepositorySession().getLocalRepository().getBasedir();
        List<ArtifactRepository> remoteRepos = mavenRequest.getRemoteRepositories();
        Aether aether = new Aether(localRepo, remoteRepos, mavenRequest.getActiveProfileIds());

        // resolve the helidon engine libs from remote repository

        // create a class-loader with the engine dependencies
        URL[] urls = aether.resolveDependencies(engineGAV)
                           .stream()
                           .map(f -> {
                               try {
                                   return f.toURI().toURL();
                               } catch (MalformedURLException e) {
                                   throw new UncheckedIOException(e);
                               }
                           }).toArray(URL[]::new);

        URLClassLoader ecl = new URLClassLoader(urls, EngineFacade.class.getClassLoader());

        Properties archetypeProps = request.getProperties();
        Map<String, String> props = new HashMap<>();
        archetypeProps.stringPropertyNames().forEach(k -> props.put(k, archetypeProps.getProperty(k)));

        // resolve the archetype JAR from the local repository
        File archetypeFile = aether.resolveArtifact(request.getArchetypeGroupId(), request.getArchetypeArtifactId(),
                "jar", request.getArchetypeVersion());

        try {
            FileSystem fileSystem = FileSystems.newFileSystem(archetypeFile.toPath(), EngineFacade.class.getClassLoader());
            Path projectDir = Path.of(request.getOutputDirectory()).resolve(request.getArtifactId());
            Files.delete(projectDir.resolve("pom.xml"));
            new ReflectedEngine(ecl, fileSystem).generate(request.isInteractiveMode(), props, Map.of(), n -> projectDir);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}
